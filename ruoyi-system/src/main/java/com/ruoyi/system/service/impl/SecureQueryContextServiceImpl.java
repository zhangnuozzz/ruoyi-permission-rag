package com.ruoyi.system.service.impl;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.ruoyi.system.domain.security.SecureQueryContext;
import com.ruoyi.system.mapper.SecureQueryContextMapper;
import com.ruoyi.system.mapper.BbacSecurityMapper;
import com.ruoyi.system.service.ISecureQueryContextService;

/**
 * VACP 查询安全上下文服务实现
 *
 * 主要完成：
 * 1. 用户安全属性注入；
 * 2. 用户组与知悉范围注入；
 * 3. 查询文本基础净化；
 * 4. metadata filter 生成；
 * 5. topK 动态收缩；
 * 6. 风险等级转为 LIMITED 查询。
 */
@Service
public class SecureQueryContextServiceImpl implements ISecureQueryContextService
{
    private static final int REQUEST_LIMIT_PER_MINUTE = 10;
    private static final int REPEAT_QUERY_LIMIT_FIVE_MINUTES = 3;
    private static final int FAIL_COUNT_LOCK_THRESHOLD = 5;
    private static final int TEMP_LOCK_MINUTES = 30;

    @Autowired
    private SecureQueryContextMapper secureQueryContextMapper;

    @Autowired
    private BbacSecurityMapper bbacSecurityMapper;

    @Override
    public SecureQueryContext buildContext(Long userId, String userName, Boolean admin, String query, Integer topK)
    {
        return buildContext(userId, userName, admin, query, topK, null);
    }

    @Override
    public SecureQueryContext buildContext(Long userId, String userName, Boolean admin, String query, Integer topK, String clientIp)
    {
        SecureQueryContext context = new SecureQueryContext();
        context.setUserId(userId);
        context.setUserName(userName);
        context.setAdmin(Boolean.TRUE.equals(admin));
        context.setRawQuery(query);
        context.setSanitizedQuery(sanitizeQuery(query));
        context.setClientIp(clientIp);
        context.setRawTopK(topK);
        context.setSafeTopK(normalizeTopK(topK));

        // BBAC：如果临时封禁已过期，自动恢复 ACTIVE。
        bbacSecurityMapper.unlockExpiredUser(userId);

        Map<String, Object> userAttr = secureQueryContextMapper.selectUserSecurityAttr(userId);
        if (userAttr == null)
        {
            context.setAllowQuery(false);
            context.getReasons().add("USER_SECURITY_ATTR_NOT_FOUND");
            return context;
        }

        String secretLevel = toStr(userAttr.get("secret_level"));
        String accessStatus = toStr(userAttr.get("access_status"));
        String riskLevel = toStr(userAttr.get("risk_level"));
        Integer failCount = toInt(userAttr.get("fail_count"));
        String accessStartTime = toStr(userAttr.get("access_start_time"));
        String accessEndTime = toStr(userAttr.get("access_end_time"));

        context.setUserSecretLevel(secretLevel);
        context.setUserAccessStatus(accessStatus);
        context.setUserRiskLevel(riskLevel);

        // BBAC 1：校验来源 IP，黑名单命中直接拒绝。
        if (clientIp != null && clientIp.length() > 0 && bbacSecurityMapper.countActiveBlacklistIp(clientIp) > 0)
        {
            context.setAllowQuery(false);
            context.getReasons().add("BBAC_IP_BLACKLIST_DENY");
            context.setRiskScore(100);
            return context;
        }

        // BBAC 3：连续访问失败次数 >= N -> 临时封禁。
        if (failCount != null && failCount >= FAIL_COUNT_LOCK_THRESHOLD)
        {
            bbacSecurityMapper.lockUserTemporarily(userId, TEMP_LOCK_MINUTES, "BBAC_FAIL_COUNT_GE_" + FAIL_COUNT_LOCK_THRESHOLD);
            context.setAllowQuery(false);
            context.getReasons().add("BBAC_FAIL_COUNT_TEMP_LOCK");
            context.setRiskScore(100);
            return context;
        }

        // BBAC 2：单位时间访问次数 <= 阈值。
        int requestCount = bbacSecurityMapper.countUserRequestsLastMinute(userId);
        context.setRequestCountInWindow(requestCount);
        if (requestCount >= REQUEST_LIMIT_PER_MINUTE)
        {
            context.setLimitedQuery(true);
            context.setSafeTopK(Math.min(context.getSafeTopK(), 3));
            context.getReasons().add("BBAC_REQUEST_RATE_LIMITED");
        }

        // BBAC 4：重复 query pattern -> 限制访问。
        int repeatedQueryCount = 0;
        if (context.getSanitizedQuery() != null && context.getSanitizedQuery().length() > 0)
        {
            repeatedQueryCount = bbacSecurityMapper.countRepeatedQueryLastFiveMinutes(userId, context.getSanitizedQuery());
        }
        context.setRepeatedQueryCount(repeatedQueryCount);
        if (repeatedQueryCount >= REPEAT_QUERY_LIMIT_FIVE_MINUTES)
        {
            context.setLimitedQuery(true);
            context.setSafeTopK(Math.min(context.getSafeTopK(), 3));
            context.getReasons().add("BBAC_REPEAT_QUERY_PATTERN_LIMITED");
        }

        int riskScore = calculateRiskScore(
                context,
                riskLevel,
                failCount,
                accessStatus,
                accessStartTime,
                accessEndTime,
                query,
                topK
        );
        context.setRiskScore(riskScore);

        if (riskScore >= 90)
        {
            context.setAllowQuery(false);
            context.getReasons().add("RISK_SCORE_TOO_HIGH_QUERY_BLOCKED");
            return context;
        }

        if (!"ACTIVE".equalsIgnoreCase(accessStatus))
        {
            context.setAllowQuery(false);
            context.getReasons().add("USER_ACCESS_STATUS_NOT_ACTIVE");
            return context;
        }

        // 原型文档基础过滤：current_time ∈ user.access_time_window。
        // 查询进入向量检索前，访问时间不合法必须快速拒绝，而不是只增加风险分。
        if (isOutsideAccessWindow(accessStartTime, accessEndTime))
        {
            context.setAllowQuery(false);
            context.getReasons().add("OUT_OF_USER_ACCESS_TIME_WINDOW");
            return context;
        }

        List<String> groupCodes = secureQueryContextMapper.selectGroupCodesByUserId(userId);
        List<String> scopeCodes = secureQueryContextMapper.selectScopeCodesByUserId(userId);

        context.setGroupCodes(groupCodes);
        context.setScopeCodes(scopeCodes);

        if (!Boolean.TRUE.equals(admin))
        {
            if (CollectionUtils.isEmpty(groupCodes))
            {
                context.setAllowQuery(false);
                context.getReasons().add("NO_VALID_GROUP_ASSIGNED");
                return context;
            }

            if (CollectionUtils.isEmpty(scopeCodes))
            {
                context.setAllowQuery(false);
                context.getReasons().add("NO_VALID_SCOPE_ASSIGNED");
                return context;
            }
        }

        if (context.getRiskScore() >= 70)
        {
            context.setLimitedQuery(true);
            context.setSafeTopK(Math.min(context.getSafeTopK(), 3));
            context.getReasons().add("HIGH_RISK_SCORE_TOPK_LIMITED");
        }

        context.setMetadataFilter(buildMetadataFilter(context));
        context.getReasons().add("SECURE_QUERY_CONTEXT_READY");
        return context;
    }

    /**
     * 查询文本基础净化。
     * 第一版只做轻量处理，避免复杂改写影响功能测试。
     */
    private String sanitizeQuery(String query)
    {
        if (query == null)
        {
            return "";
        }

        String q = query.trim();

        // 去掉常见控制字符，避免日志和下游请求污染
        q = q.replace("\n", " ");
        q = q.replace("\r", " ");
        q = q.replace("\t", " ");

        // 限制超长查询
        if (q.length() > 500)
        {
            q = q.substring(0, 500);
        }

        return q;
    }

    /**
     * topK 安全归一化。
     */
    private Integer normalizeTopK(Integer topK)
    {
        if (topK == null || topK <= 0)
        {
            return 5;
        }

        if (topK > 20)
        {
            return 20;
        }

        return topK;
    }

    /**
     * 构建 metadata filter。
     *
     * 说明：
     * 这是平台侧标准化表达式，后续可转换为 Milvus / RAG Server 实际过滤语法。
     */
    private String buildMetadataFilter(SecureQueryContext context)
    {
        StringBuilder filter = new StringBuilder();

        filter.append("metadata_status == 'ACTIVE'");

        if (context.getUserSecretLevel() != null && context.getUserSecretLevel().length() > 0)
        {
            filter.append(" && security_level <= '");
            filter.append(context.getUserSecretLevel());
            filter.append("'");
        }

        if (!Boolean.TRUE.equals(context.getAdmin()) && !CollectionUtils.isEmpty(context.getGroupCodes()))
        {
            filter.append(" && owner_group_code in ");
            filter.append(toQuotedList(context.getGroupCodes()));
        }

        if (!CollectionUtils.isEmpty(context.getScopeCodes()))
        {
            filter.append(" && scope_code in ");
            filter.append(toQuotedList(context.getScopeCodes()));
        }

        return filter.toString();
    }

    private String toQuotedList(List<String> list)
    {
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        for (int i = 0; i < list.size(); i++)
        {
            if (i > 0)
            {
                builder.append(", ");
            }
            builder.append("'");
            builder.append(list.get(i));
            builder.append("'");
        }
        builder.append("]");
        return builder.toString();
    }

    /**
     * 动态风险评分。
     *
     * 评分来源：
     * 1. 用户基础风险等级；
     * 2. 账号访问状态；
     * 3. 登录失败次数；
     * 4. 是否处于允许访问时间窗口；
     * 5. 查询内容是否包含敏感词；
     * 6. topK 是否过大。
     */
    private int calculateRiskScore(SecureQueryContext context, String riskLevel, Integer failCount,
                                   String accessStatus, String accessStartTime, String accessEndTime,
                                   String query, Integer topK)
    {
        int score = 10;

        if ("MEDIUM".equalsIgnoreCase(riskLevel))
        {
            score = 40;
            context.getReasons().add("BASE_RISK_LEVEL_MEDIUM");
        }
        else if ("HIGH".equalsIgnoreCase(riskLevel))
        {
            score = 70;
            context.getReasons().add("BASE_RISK_LEVEL_HIGH");
        }
        else
        {
            context.getReasons().add("BASE_RISK_LEVEL_LOW");
        }

        if (!"ACTIVE".equalsIgnoreCase(accessStatus))
        {
            score += 30;
            context.getReasons().add("RISK_ACCESS_STATUS_NOT_ACTIVE");
        }

        if (failCount != null && failCount >= 5)
        {
            score += 20;
            context.getReasons().add("RISK_FAIL_COUNT_GE_5");
        }
        else if (failCount != null && failCount >= 3)
        {
            score += 10;
            context.getReasons().add("RISK_FAIL_COUNT_GE_3");
        }

        if (isOutsideAccessWindow(accessStartTime, accessEndTime))
        {
            score += 10;
            context.getReasons().add("RISK_OUTSIDE_ACCESS_TIME_WINDOW");
        }

        if (containsSensitiveKeyword(query))
        {
            score += 10;
            context.getReasons().add("RISK_QUERY_CONTAINS_SENSITIVE_KEYWORD");
        }

        if (topK != null && topK > 20)
        {
            score += 10;
            context.getReasons().add("RISK_TOPK_TOO_LARGE");
        }

        if (context.getRequestCountInWindow() != null && context.getRequestCountInWindow() >= REQUEST_LIMIT_PER_MINUTE)
        {
            score += 20;
            context.getReasons().add("RISK_REQUEST_RATE_TOO_HIGH");
        }

        if (context.getRepeatedQueryCount() != null && context.getRepeatedQueryCount() >= REPEAT_QUERY_LIMIT_FIVE_MINUTES)
        {
            score += 20;
            context.getReasons().add("RISK_REPEAT_QUERY_PATTERN");
        }

        return Math.min(score, 100);
    }

    private boolean isOutsideAccessWindow(String accessStartTime, String accessEndTime)
    {
        if (accessStartTime == null || accessEndTime == null
                || accessStartTime.length() == 0 || accessEndTime.length() == 0)
        {
            return false;
        }

        try
        {
            LocalTime now = LocalTime.now();
            LocalTime start = LocalTime.parse(accessStartTime.substring(0, 8));
            LocalTime end = LocalTime.parse(accessEndTime.substring(0, 8));

            if (start.equals(end))
            {
                return false;
            }

            if (start.isBefore(end))
            {
                return now.isBefore(start) || now.isAfter(end);
            }

            // 兼容跨天窗口，例如 22:00:00 - 06:00:00
            return now.isAfter(end) && now.isBefore(start);
        }
        catch (Exception e)
        {
            return false;
        }
    }

    private boolean containsSensitiveKeyword(String query)
    {
        if (query == null)
        {
            return false;
        }

        String q = query.toLowerCase();
        return q.contains("password")
                || q.contains("passwd")
                || q.contains("token")
                || q.contains("secret")
                || q.contains("key")
                || q.contains("密钥")
                || q.contains("密码")
                || q.contains("绝密")
                || q.contains("泄露");
    }

    private String toStr(Object obj)
    {
        return obj == null ? null : String.valueOf(obj);
    }

    private Integer toInt(Object obj)
    {
        if (obj == null)
        {
            return 0;
        }
        if (obj instanceof Number)
        {
            return ((Number) obj).intValue();
        }
        try
        {
            return Integer.parseInt(String.valueOf(obj));
        }
        catch (Exception e)
        {
            return 0;
        }
    }
}
