package com.ruoyi.system.service.impl;

import java.sql.Time;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.ruoyi.system.domain.permission.AccessDecisionResult;
import com.ruoyi.system.mapper.AccessDecisionMapper;
import com.ruoyi.system.service.IAccessDecisionService;

/**
 * VACP 访问控制决策服务实现
 *
 * 严格对齐原型文档：
 * 1. Token 解析后取得 user_id；
 * 2. user.status == ACTIVE；
 * 3. current_time ∈ user.access_time_window；
 * 4. doc.status == ACTIVE；
 * 5. 用户密级 >= 文档密级；
 * 6. doc.allowed_group ∈ user.user_groups；
 * 7. 用户在共同用户组上的组密级 >= 文档密级；
 * 8. doc_level >= SECRET 时仅允许工作时间访问；
 * 9. 输出 ALLOW / DENY / LIMITED + risk_score + user_groups。
 */
@Service
public class AccessDecisionServiceImpl implements IAccessDecisionService
{
    private static final LocalTime WORK_START = LocalTime.of(8, 0);
    private static final LocalTime WORK_END = LocalTime.of(18, 0);

    @Autowired
    private AccessDecisionMapper accessDecisionMapper;

    @Override
    public AccessDecisionResult decide(Long userId, String userName, Boolean admin, String docId)
    {
        AccessDecisionResult result = new AccessDecisionResult();
        result.setUserId(userId);
        result.setUserName(userName);
        result.setDocId(docId);
        result.setPolicyHit("ABAC+TBAC+BBAC");

        Map<String, Object> userAttr = accessDecisionMapper.selectUserSecurityAttr(userId);
        if (userAttr == null)
        {
            deny(result, "USER_SECURITY_ATTR_NOT_FOUND");
            return result;
        }

        Map<String, Object> doc = accessDecisionMapper.selectDocMetadata(docId);
        if (doc == null)
        {
            deny(result, "DOCUMENT_METADATA_NOT_FOUND");
            return result;
        }

        result.setDocId(toStr(doc.get("doc_id")));
        result.setDocName(toStr(doc.get("doc_name")));

        String userSecretLevel = toStr(userAttr.get("secret_level"));
        String accessStatus = toStr(userAttr.get("access_status"));
        String riskLevel = toStr(userAttr.get("risk_level"));
        Integer failCount = toInt(userAttr.get("fail_count"));

        String docSecurityLevel = toStr(doc.get("security_level"));
        String ownerGroupCode = firstNotEmpty(toStr(doc.get("doc_group_id")), toStr(doc.get("owner_group_code")));
        String metadataStatus = toStr(doc.get("metadata_status"));
        String oldStatus = toStr(doc.get("status"));

        int riskScore = calculateRiskScore(riskLevel, failCount);
        result.setRiskScore(riskScore);

        // 1. 用户状态检查：user.status == ACTIVE
        if (!"ACTIVE".equalsIgnoreCase(accessStatus))
        {
            deny(result, "USER_ACCESS_STATUS_NOT_ACTIVE");
            return result;
        }

        // 2. 用户访问时间窗口：current_time ∈ user.access_time_window
        if (!isInAccessTimeWindow(userAttr.get("access_start_time"), userAttr.get("access_end_time")))
        {
            deny(result, "OUT_OF_USER_ACCESS_TIME_WINDOW");
            return result;
        }

        // 3. 文档状态检查：doc.status == ACTIVE
        if (!isDocActive(metadataStatus, oldStatus))
        {
            deny(result, "DOCUMENT_STATUS_NOT_ACTIVE");
            return result;
        }

        // 4. PUBLIC 文档：登录用户通过基础状态与时间校验后可访问
        if (isPublic(docSecurityLevel, ownerGroupCode))
        {
            if (riskScore >= 80)
            {
                limited(result, "PUBLIC_DOCUMENT_HIGH_RISK_LIMITED");
            }
            else
            {
                allow(result, "PUBLIC_DOCUMENT_ACCESS_ALLOWED");
            }
            result.setMetadataFilter(buildMetadataFilter(userSecretLevel, "GROUP_PUBLIC"));
            return result;
        }

        // 5. 用户密级比较：用户密级 >= 文档密级
        if (secretRank(userSecretLevel) < secretRank(docSecurityLevel))
        {
            deny(result, "USER_SECRET_LEVEL_LOWER_THAN_DOCUMENT");
            return result;
        }

        // 6. 高密级数据时间限制：doc_level >= SECRET -> 仅允许工作时间访问
        if (secretRank(docSecurityLevel) >= secretRank("SECRET") && !isInWorkTime())
        {
            deny(result, "HIGH_SECRET_DOCUMENT_ONLY_WORK_TIME");
            return result;
        }

        // 7. 用户必须属于文档所属组：doc.allowed_group ∈ user.user_groups
        List<String> userGroupCodes = accessDecisionMapper.selectUserGroupCodes(userId);
        if (CollectionUtils.isEmpty(userGroupCodes) || ownerGroupCode == null || !userGroupCodes.contains(ownerGroupCode))
        {
            deny(result, "USER_GROUP_NOT_MATCH_DOCUMENT_GROUP");
            return result;
        }

        // 8. 用户组密级比较：共同用户组的密级 >= 文档密级
        String userGroupSecretLevel = accessDecisionMapper.selectUserGroupSecretLevel(userId, ownerGroupCode);
        if (secretRank(userGroupSecretLevel) < secretRank(docSecurityLevel))
        {
            deny(result, "USER_GROUP_SECRET_LEVEL_LOWER_THAN_DOCUMENT");
            return result;
        }

        // 9. 高风险用户 LIMITED
        if ("HIGH".equalsIgnoreCase(riskLevel) || riskScore >= 80)
        {
            limited(result, "HIGH_RISK_USER_LIMITED_ACCESS");
            result.setMetadataFilter(buildMetadataFilter(userSecretLevel, ownerGroupCode));
            return result;
        }

        allow(result, "CHECK_USER_DOC_ALLOW");
        result.setMetadataFilter(buildMetadataFilter(userSecretLevel, ownerGroupCode));
        return result;
    }

    private void deny(AccessDecisionResult result, String reason)
    {
        result.setDecision("DENY");
        result.setAllowAccess(false);
        result.setLimitedAccess(false);
        result.getReasons().add(reason);
    }

    private void allow(AccessDecisionResult result, String reason)
    {
        result.setDecision("ALLOW");
        result.setAllowAccess(true);
        result.setLimitedAccess(false);
        result.getReasons().add(reason);
    }

    private void limited(AccessDecisionResult result, String reason)
    {
        result.setDecision("LIMITED");
        result.setAllowAccess(true);
        result.setLimitedAccess(true);
        result.getReasons().add(reason);
    }

    private int secretRank(String level)
    {
        if ("PUBLIC".equalsIgnoreCase(level)) return 1;
        if ("INTERNAL".equalsIgnoreCase(level)) return 2;
        if ("SECRET".equalsIgnoreCase(level)) return 3;
        if ("CONFIDENTIAL".equalsIgnoreCase(level)) return 4;
        return 0;
    }

    private boolean isPublic(String docSecurityLevel, String ownerGroupCode)
    {
        return "PUBLIC".equalsIgnoreCase(docSecurityLevel)
                || "GROUP_PUBLIC".equalsIgnoreCase(ownerGroupCode);
    }

    private boolean isDocActive(String metadataStatus, String oldStatus)
    {
        if (metadataStatus != null && !"".equals(metadataStatus))
        {
            return "ACTIVE".equalsIgnoreCase(metadataStatus);
        }
        return "0".equals(oldStatus);
    }

    private boolean isInAccessTimeWindow(Object startObj, Object endObj)
    {
        if (startObj == null || endObj == null)
        {
            return true;
        }

        LocalTime now = LocalTime.now();
        LocalTime start = toLocalTime(startObj);
        LocalTime end = toLocalTime(endObj);

        if (start == null || end == null || start.equals(end))
        {
            return true;
        }

        if (start.isBefore(end))
        {
            return !now.isBefore(start) && !now.isAfter(end);
        }

        return !now.isBefore(start) || !now.isAfter(end);
    }

    private boolean isInWorkTime()
    {
        LocalTime now = LocalTime.now();
        return !now.isBefore(WORK_START) && !now.isAfter(WORK_END);
    }

    private LocalTime toLocalTime(Object obj)
    {
        if (obj instanceof Time)
        {
            return ((Time) obj).toLocalTime();
        }
        try
        {
            return LocalTime.parse(String.valueOf(obj).substring(0, 8));
        }
        catch (Exception e)
        {
            return null;
        }
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

    private String buildMetadataFilter(String userSecretLevel, String ownerGroupCode)
    {
        return "metadata_status == 'ACTIVE' && security_level <= '" + userSecretLevel
                + "' && owner_group_code == '" + ownerGroupCode + "'";
    }

    private String firstNotEmpty(String a, String b)
    {
        return a != null && a.length() > 0 ? a : b;
    }

    private String toStr(Object obj)
    {
        return obj == null ? null : String.valueOf(obj);
    }

    private Integer toInt(Object obj)
    {
        if (obj == null) return 0;
        if (obj instanceof Number) return ((Number) obj).intValue();
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
