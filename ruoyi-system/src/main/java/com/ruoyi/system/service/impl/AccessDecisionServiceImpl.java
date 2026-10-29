package com.ruoyi.system.service.impl;

import java.sql.Time;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.ruoyi.system.domain.permission.AccessDecisionResult;
import com.ruoyi.system.domain.permission.PermissionContext;
import com.ruoyi.system.domain.permission.WorkingHoursRule;
import com.ruoyi.system.service.IPermissionContextService;
import com.ruoyi.system.mapper.AccessDecisionMapper;
import com.ruoyi.system.service.IAccessDecisionService;

/**
 * 向量库访问控制系统核心文档访问决策。
 *
 * 核心规则：
 *
 * PUBLIC：
 * 公开非密数据，不检查密级、不检查知悉用户组。
 *
 * INTERNAL：
 * 工作秘密，只检查人员密级。
 *
 * SECRET / CONFIDENTIAL：
 * 国家秘密，同时检查：
 * 1. 人员密级 >= 文档密级；
 * 2. 文档唯一用户组属于人员用户组集合。
 *
 * scope_code 和 group_secret_level 不再参与核心鉴权。
 */
@Service
public class AccessDecisionServiceImpl implements IAccessDecisionService
{
    @Autowired
    private AccessDecisionMapper accessDecisionMapper;

    @Autowired
    private IPermissionContextService permissionContextService;

    @Override
    public AccessDecisionResult decide(
            Long userId,
            String userName,
            Boolean admin,
            String docId)
    {
        AccessDecisionResult result = new AccessDecisionResult();

        result.setUserId(userId);
        result.setUserName(userName);
        result.setDocId(docId);
        result.setPolicyHit("SECURITY_LEVEL+KNOWLEDGE_GROUP+ADDITIONAL_SECURITY");

        Map<String, Object> doc =
                accessDecisionMapper.selectDocMetadata(docId);

        if (doc == null)
        {
            deny(result, "DOCUMENT_METADATA_NOT_FOUND");
            return result;
        }

        result.setDocId(toStr(doc.get("doc_id")));
        result.setDocName(toStr(doc.get("doc_name")));

        String docLevel =
                normalizeLevel(toStr(doc.get("security_level")));

        if ("UNKNOWN".equals(docLevel))
        {
            deny(result, "INVALID_DOCUMENT_SECURITY_LEVEL");
            return result;
        }

        String ownerGroupCode =
                firstNotEmpty(
                        toStr(doc.get("doc_group_id")),
                        toStr(doc.get("owner_group_code"))
                );

        String metadataStatus =
                toStr(doc.get("metadata_status"));

        String oldStatus =
                toStr(doc.get("status"));

        // 文档必须有效
        if (!isDocActive(metadataStatus, oldStatus))
        {
            deny(result, "DOCUMENT_STATUS_NOT_ACTIVE");
            return result;
        }

        PermissionContext policyContext = permissionContextService.buildContext(userId, userName, admin);
        String policyDenial = WorkingHoursRule.denialReason(policyContext, docLevel);
        if (Boolean.FALSE.equals(policyContext.getAllowAccess()) || policyDenial != null)
        {
            deny(result, policyDenial == null ? "ACCESS_POLICY_DENIED" : policyDenial);
            return result;
        }

        /*
         * PUBLIC：
         * 公开非密数据，不依赖人员密级和用户组。
         */
        if ("PUBLIC".equals(docLevel))
        {
            allow(result, "PUBLIC_NON_SECRET_ACCESS_ALLOWED");
            result.setMetadataFilter(
                    "security_level == \"PUBLIC\""
            );
            return result;
        }

        /*
         * INTERNAL / SECRET / CONFIDENTIAL
         * 均需要存在人员安全属性。
         */
        Map<String, Object> userAttr =
                accessDecisionMapper.selectUserSecurityAttr(userId);

        if (userAttr == null)
        {
            deny(result, "USER_SECURITY_ATTR_NOT_FOUND");
            return result;
        }

        String userLevel =
                normalizeLevel(toStr(userAttr.get("secret_level")));

        String accessStatus =
                toStr(userAttr.get("access_status"));

        String riskLevel =
                toStr(userAttr.get("risk_level"));

        Integer failCount =
                toInt(userAttr.get("fail_count"));

        int riskScore =
                calculateRiskScore(riskLevel, failCount);

        result.setRiskScore(riskScore);

        // 附加安全限制：人员访问状态
        if (!"ACTIVE".equalsIgnoreCase(accessStatus))
        {
            deny(result, "USER_ACCESS_STATUS_NOT_ACTIVE");
            return result;
        }

        // 附加安全限制：人员访问时间窗口
        if (!isInAccessTimeWindow(
                userAttr.get("access_start_time"),
                userAttr.get("access_end_time")))
        {
            deny(result, "OUT_OF_USER_ACCESS_TIME_WINDOW");
            return result;
        }

        /*
         * 所有非公开数据首先检查人员密级。
         */
        if (secretRank(userLevel) < secretRank(docLevel))
        {
            deny(
                    result,
                    "USER_SECRET_LEVEL_LOWER_THAN_DOCUMENT"
            );
            return result;
        }

        /*
         * INTERNAL：
         * 工作秘密。
         *
         * 密级满足即可，不检查：
         * - 用户组
         * - scope_code
         * - group_secret_level
         */
        if ("INTERNAL".equals(docLevel))
        {
            if (isHighRisk(riskLevel, riskScore))
            {
                limited(
                        result,
                        "INTERNAL_DOCUMENT_HIGH_RISK_LIMITED"
                );
            }
            else
            {
                allow(
                        result,
                        "INTERNAL_WORK_SECRET_LEVEL_ALLOWED"
                );
            }

            result.setMetadataFilter(
                    "security_level == \"PUBLIC\""
                    + " || security_level == \"INTERNAL\""
            );

            return result;
        }

        /*
         * SECRET / CONFIDENTIAL：
         * 国家秘密必须具有唯一知悉用户组。
         */
        if (ownerGroupCode == null
                || ownerGroupCode.length() == 0)
        {
            deny(
                    result,
                    "STATE_SECRET_DOCUMENT_GROUP_NOT_CONFIGURED"
            );
            return result;
        }

        List<String> userGroupCodes =
                accessDecisionMapper.selectUserGroupCodes(userId);

        /*
         * 知悉范围判定：
         *
         * 文档唯一用户组 ∈ 用户所属用户组集合
         */
        if (CollectionUtils.isEmpty(userGroupCodes)
                || !userGroupCodes.contains(ownerGroupCode))
        {
            deny(
                    result,
                    "USER_NOT_IN_DOCUMENT_KNOWLEDGE_GROUP"
            );
            return result;
        }

        /*
         * 不再检查 group_secret_level。
         *
         * 用户组代表知悉范围，
         * 人员密级代表安全许可等级。
         */

        if (isHighRisk(riskLevel, riskScore))
        {
            limited(
                    result,
                    "STATE_SECRET_HIGH_RISK_LIMITED"
            );
        }
        else
        {
            allow(
                    result,
                    "STATE_SECRET_LEVEL_AND_GROUP_ALLOWED"
            );
        }

        result.setMetadataFilter(
                buildDocumentFilter(userLevel, userGroupCodes)
        );

        return result;
    }

    private String buildDocumentFilter(
            String userLevel,
            List<String> groups)
    {
        StringBuilder filter = new StringBuilder();

        filter.append(
                "(security_level == \"PUBLIC\""
        );

        if (secretRank(userLevel)
                >= secretRank("INTERNAL"))
        {
            filter.append(
                    " || security_level == \"INTERNAL\""
            );
        }

        if (secretRank(userLevel)
                >= secretRank("SECRET")
                && groups != null
                && !groups.isEmpty())
        {
            filter.append(
                    " || (security_level == \"SECRET\""
            );

            filter.append(
                    " && group_id in "
            );

            filter.append(toQuotedList(groups));

            filter.append(")");
        }

        if (secretRank(userLevel)
                >= secretRank("CONFIDENTIAL")
                && groups != null
                && !groups.isEmpty())
        {
            filter.append(
                    " || (security_level == \"CONFIDENTIAL\""
            );

            filter.append(
                    " && group_id in "
            );

            filter.append(toQuotedList(groups));

            filter.append(")");
        }

        filter.append(")");

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

            builder.append("\"");
            builder.append(list.get(i));
            builder.append("\"");
        }

        builder.append("]");

        return builder.toString();
    }

    private boolean isHighRisk(
            String riskLevel,
            int riskScore)
    {
        return "HIGH".equalsIgnoreCase(riskLevel)
                || riskScore >= 80;
    }

    private void deny(
            AccessDecisionResult result,
            String reason)
    {
        result.setDecision("DENY");
        result.setAllowAccess(false);
        result.setLimitedAccess(false);
        result.getReasons().add(reason);
    }

    private void allow(
            AccessDecisionResult result,
            String reason)
    {
        result.setDecision("ALLOW");
        result.setAllowAccess(true);
        result.setLimitedAccess(false);
        result.getReasons().add(reason);
    }

    private void limited(
            AccessDecisionResult result,
            String reason)
    {
        result.setDecision("LIMITED");
        result.setAllowAccess(true);
        result.setLimitedAccess(true);
        result.getReasons().add(reason);
    }

    private int secretRank(String level)
    {
        if ("PUBLIC".equals(level))
        {
            return 1;
        }

        if ("INTERNAL".equals(level))
        {
            return 2;
        }

        if ("SECRET".equals(level))
        {
            return 3;
        }

        if ("CONFIDENTIAL".equals(level))
        {
            return 4;
        }

        return 0;
    }

    private String normalizeLevel(String level)
    {
        String value = level == null ? "" : level.trim();
        if ("PUBLIC".equalsIgnoreCase(value)
                || "INTERNAL".equalsIgnoreCase(value)
                || "SECRET".equalsIgnoreCase(value)
                || "CONFIDENTIAL".equalsIgnoreCase(value))
        {
            return value.toUpperCase(java.util.Locale.ROOT);
        }
        return "UNKNOWN";
    }

    private boolean isDocActive(
            String metadataStatus,
            String oldStatus)
    {
        if (metadataStatus != null
                && metadataStatus.length() > 0)
        {
            return "ACTIVE".equalsIgnoreCase(
                    metadataStatus
            );
        }

        return "0".equals(oldStatus);
    }

    private boolean isInAccessTimeWindow(
            Object startObj,
            Object endObj)
    {
        if (startObj == null || endObj == null)
        {
            return true;
        }

        LocalTime start = toLocalTime(startObj);
        LocalTime end = toLocalTime(endObj);

        if (start == null
                || end == null
                || start.equals(end))
        {
            return true;
        }

        LocalTime now = LocalTime.now();

        if (start.isBefore(end))
        {
            return !now.isBefore(start)
                    && !now.isAfter(end);
        }

        return !now.isBefore(start)
                || !now.isAfter(end);
    }

    private LocalTime toLocalTime(Object value)
    {
        if (value instanceof Time)
        {
            return ((Time) value).toLocalTime();
        }

        try
        {
            String text = String.valueOf(value);

            if (text.length() >= 8)
            {
                text = text.substring(0, 8);
            }

            return LocalTime.parse(text);
        }
        catch (Exception e)
        {
            return null;
        }
    }

    private int calculateRiskScore(
            String riskLevel,
            Integer failCount)
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
            score += Math.min(
                    failCount * 5,
                    20
            );
        }

        return Math.min(score, 100);
    }

    private String firstNotEmpty(
            String first,
            String second)
    {
        return first != null
                && first.length() > 0
                ? first
                : second;
    }

    private String toStr(Object value)
    {
        return value == null
                ? null
                : String.valueOf(value);
    }

    private Integer toInt(Object value)
    {
        if (value == null)
        {
            return 0;
        }

        if (value instanceof Number)
        {
            return ((Number) value).intValue();
        }

        try
        {
            return Integer.parseInt(
                    String.valueOf(value)
            );
        }
        catch (Exception e)
        {
            return 0;
        }
    }
}
