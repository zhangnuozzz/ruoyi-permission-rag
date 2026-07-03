package com.ruoyi.system.service.impl;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.ruoyi.system.domain.security.SecureQueryContext;
import com.ruoyi.system.mapper.SecureQueryContextMapper;
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
    @Autowired
    private SecureQueryContextMapper secureQueryContextMapper;

    @Override
    public SecureQueryContext buildContext(Long userId, String userName, Boolean admin, String query, Integer topK)
    {
        SecureQueryContext context = new SecureQueryContext();
        context.setUserId(userId);
        context.setUserName(userName);
        context.setAdmin(Boolean.TRUE.equals(admin));
        context.setRawQuery(query);
        context.setSanitizedQuery(sanitizeQuery(query));
        context.setRawTopK(topK);
        context.setSafeTopK(normalizeTopK(topK));

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

        context.setUserSecretLevel(secretLevel);
        context.setUserAccessStatus(accessStatus);
        context.setUserRiskLevel(riskLevel);
        context.setRiskScore(calculateRiskScore(riskLevel, failCount));

        if (!"ACTIVE".equalsIgnoreCase(accessStatus))
        {
            context.setAllowQuery(false);
            context.getReasons().add("USER_ACCESS_STATUS_NOT_ACTIVE");
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

        if ("HIGH".equalsIgnoreCase(riskLevel) || context.getRiskScore() >= 80)
        {
            context.setLimitedQuery(true);
            context.setSafeTopK(Math.min(context.getSafeTopK(), 3));
            context.getReasons().add("HIGH_RISK_USER_TOPK_LIMITED");
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

    private int calculateRiskScore(String riskLevel, Integer failCount)
    {
        int score = 10;

        if ("MEDIUM".equalsIgnoreCase(riskLevel))
        {
            score = 40;
        }
        else if ("HIGH".equalsIgnoreCase(riskLevel))
        {
            score = 80;
        }

        if (failCount != null && failCount > 0)
        {
            score += Math.min(failCount * 5, 20);
        }

        return Math.min(score, 100);
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
