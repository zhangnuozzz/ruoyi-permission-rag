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
 * 第一版规则：
 * 1. 用户安全属性不存在：DENY
 * 2. 用户状态不是 ACTIVE：DENY
 * 3. 当前时间不在用户访问时间窗口内：DENY
 * 4. 文档不存在：DENY
 * 5. 文档状态不是 ACTIVE：DENY
 * 6. 用户密级低于文档密级：DENY
 * 7. 非管理员用户不属于文档所属用户组：DENY
 * 8. 风险等级 HIGH：LIMITED
 * 9. 其余情况：ALLOW
 */
@Service
public class AccessDecisionServiceImpl implements IAccessDecisionService
{
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
        String ownerGroupCode = toStr(doc.get("owner_group_code"));
        String metadataStatus = toStr(doc.get("metadata_status"));
        String oldStatus = toStr(doc.get("status"));

        int riskScore = calculateRiskScore(riskLevel, failCount);
        result.setRiskScore(riskScore);

        // 1. 用户状态检查
        if (!"ACTIVE".equalsIgnoreCase(accessStatus))
        {
            deny(result, "USER_ACCESS_STATUS_NOT_ACTIVE");
            return result;
        }

        // 2. 访问时间窗口检查
        if (!isInAccessTimeWindow(userAttr.get("access_start_time"), userAttr.get("access_end_time")))
        {
            deny(result, "OUT_OF_USER_ACCESS_TIME_WINDOW");
            return result;
        }

        // 3. 文档状态检查
        if (!isDocActive(metadataStatus, oldStatus))
        {
            deny(result, "DOCUMENT_STATUS_NOT_ACTIVE");
            return result;
        }

        // 4. 密级检查：用户密级必须 >= 文档密级
        if (secretRank(userSecretLevel) < secretRank(docSecurityLevel))
        {
            deny(result, "USER_SECRET_LEVEL_LOWER_THAN_DOCUMENT");
            return result;
        }

        // 5. 用户组检查：非管理员必须属于文档所属用户组
        if (!Boolean.TRUE.equals(admin))
        {
            List<String> userGroupCodes = accessDecisionMapper.selectUserGroupCodes(userId);
            if (CollectionUtils.isEmpty(userGroupCodes) || !userGroupCodes.contains(ownerGroupCode))
            {
                deny(result, "USER_GROUP_NOT_MATCH_DOCUMENT_GROUP");
                return result;
            }
        }

        // 6. 高风险用户不完全拒绝，而是 LIMITED
        if ("HIGH".equalsIgnoreCase(riskLevel) || riskScore >= 80)
        {
            limited(result, "HIGH_RISK_USER_LIMITED_ACCESS");
            result.setMetadataFilter(buildMetadataFilter(docSecurityLevel, ownerGroupCode));
            return result;
        }

        allow(result, "ACCESS_ALLOWED");
        result.setMetadataFilter(buildMetadataFilter(docSecurityLevel, ownerGroupCode));
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
        if ("PUBLIC".equalsIgnoreCase(level))
        {
            return 1;
        }
        if ("INTERNAL".equalsIgnoreCase(level))
        {
            return 2;
        }
        if ("SECRET".equalsIgnoreCase(level))
        {
            return 3;
        }
        if ("CONFIDENTIAL".equalsIgnoreCase(level))
        {
            return 4;
        }
        return 0;
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

        if (start == null || end == null)
        {
            return true;
        }

        if (start.equals(end))
        {
            return true;
        }

        // 普通时间段，例如 08:00 - 18:00
        if (start.isBefore(end))
        {
            return !now.isBefore(start) && !now.isAfter(end);
        }

        // 跨天时间段，例如 22:00 - 06:00
        return !now.isBefore(start) || !now.isAfter(end);
    }

    private LocalTime toLocalTime(Object obj)
    {
        if (obj instanceof Time)
        {
            return ((Time) obj).toLocalTime();
        }
        try
        {
            return LocalTime.parse(String.valueOf(obj));
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

    private String buildMetadataFilter(String securityLevel, String ownerGroupCode)
    {
        return "metadata_status == 'ACTIVE' && security_level <= '" + securityLevel
                + "' && owner_group_code == '" + ownerGroupCode + "'";
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
