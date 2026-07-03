package com.ruoyi.system.service.impl;

import com.ruoyi.system.domain.SysRagDoc;
import com.ruoyi.system.domain.permission.PermissionContext;
import com.ruoyi.system.domain.rag.RagSearchResult;
import com.ruoyi.system.mapper.AccessDecisionMapper;
import com.ruoyi.system.mapper.SysRagDocMapper;
import com.ruoyi.system.service.IRagSecondFilterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * RAG 检索结果二次过滤 Service 实现
 *
 * 严格对齐原型文档：
 * 1. 对每个文档执行 check(user, doc) -> ALLOW / DENY；
 * 2. 文档状态 doc.status == ACTIVE；
 * 3. 用户密级 >= 文档密级；
 * 4. doc.allowed_group ∈ user.user_groups；
 * 5. 用户组密级 >= 文档密级；
 * 6. doc_level >= SECRET -> 仅允许工作时间访问；
 * 7. 剔除不合规结果，并写入 blockedReason。
 */
@Service
public class RagSecondFilterServiceImpl implements IRagSecondFilterService
{
    private static final LocalTime WORK_START = LocalTime.of(8, 0);
    private static final LocalTime WORK_END = LocalTime.of(18, 0);

    @Autowired
    private SysRagDocMapper sysRagDocMapper;

    @Autowired
    private AccessDecisionMapper accessDecisionMapper;

    @Override
    public List<RagSearchResult> filter(PermissionContext context, List<RagSearchResult> results)
    {
        List<RagSearchResult> filtered = new ArrayList<RagSearchResult>();

        if (context == null || results == null || results.isEmpty())
        {
            return filtered;
        }

        Long userId = context.getUserId();
        Map<String, Object> userAttr = accessDecisionMapper.selectUserSecurityAttr(userId);

        if (userAttr == null)
        {
            rejectAll(results, "USER_SECURITY_ATTR_NOT_FOUND", "二次过滤拒绝：用户安全属性不存在");
            return filtered;
        }

        String userSecretLevel = toStr(userAttr.get("secret_level"));
        String accessStatus = toStr(userAttr.get("access_status"));

        if (!"ACTIVE".equalsIgnoreCase(accessStatus))
        {
            rejectAll(results, "USER_ACCESS_STATUS_NOT_ACTIVE", "二次过滤拒绝：用户访问状态不是 ACTIVE");
            return filtered;
        }

        List<String> allowedScopes = context.getScopeCodes();
        if (allowedScopes == null)
        {
            allowedScopes = new ArrayList<String>();
        }

        List<String> allowedGroups = context.getGroupCodes();
        if (allowedGroups == null)
        {
            allowedGroups = new ArrayList<String>();
        }

        for (RagSearchResult result : results)
        {
            if (result == null)
            {
                continue;
            }

            SysRagDoc doc = null;
            if (result.getDocId() != null && result.getDocId().length() > 0)
            {
                doc = sysRagDocMapper.selectSysRagDocByDocId(result.getDocId());
            }

            if (doc == null)
            {
                reject(result, "DOCUMENT_METADATA_NOT_FOUND", "二次过滤拒绝：平台侧未找到该文档元数据");
                continue;
            }

            enrichResult(result, doc);

            String metadataStatus = result.getMetadataStatus();
            String scopeCode = result.getScopeCode();
            String level = result.getLevel();
            String ownerGroupCode = result.getOwnerGroupCode();

            if (!isActive(metadataStatus))
            {
                reject(result, "DOCUMENT_STATUS_NOT_ACTIVE", "二次过滤拒绝：文档状态不是 ACTIVE");
                continue;
            }

            // PUBLIC 文档：登录用户通过基础用户状态检查后允许访问
            if (isPublicResource(result))
            {
                pass(result, "check(user, doc) -> ALLOW：PUBLIC 公开文档默认允许登录用户访问");
                filtered.add(result);
                continue;
            }

            // 用户密级 >= 文档密级
            if (secretRank(userSecretLevel) < secretRank(level))
            {
                reject(result, "USER_SECRET_LEVEL_LOWER_THAN_DOCUMENT", "check(user, doc) -> DENY：用户密级低于文档密级");
                continue;
            }

            // 高密级文档仅允许工作时间访问
            if (secretRank(level) >= secretRank("SECRET") && !isInWorkTime())
            {
                reject(result, "HIGH_SECRET_DOCUMENT_ONLY_WORK_TIME", "check(user, doc) -> DENY：高密级文档仅允许工作时间访问");
                continue;
            }

            // doc.allowed_group ∈ user.user_groups
            if (ownerGroupCode == null || ownerGroupCode.length() == 0 || !allowedGroups.contains(ownerGroupCode))
            {
                reject(result, "USER_GROUP_NOT_MATCH_DOCUMENT_GROUP", "check(user, doc) -> DENY：用户不属于文档所属用户组");
                continue;
            }

            // 用户组密级 >= 文档密级
            String userGroupSecretLevel = accessDecisionMapper.selectUserGroupSecretLevel(userId, ownerGroupCode);
            if (secretRank(userGroupSecretLevel) < secretRank(level))
            {
                reject(result, "USER_GROUP_SECRET_LEVEL_LOWER_THAN_DOCUMENT", "check(user, doc) -> DENY：用户组密级低于文档密级");
                continue;
            }

            // 知悉范围标签校验
            if (scopeCode == null || scopeCode.length() == 0 || !allowedScopes.contains(scopeCode))
            {
                reject(result, "USER_SCOPE_NOT_MATCH_DOCUMENT_SCOPE", "check(user, doc) -> DENY：用户不具备该知悉范围");
                continue;
            }

            pass(result, "check(user, doc) -> ALLOW：文档状态、用户密级、用户组、用户组密级与知悉范围均满足要求");
            filtered.add(result);
        }

        return filtered;
    }

    private void enrichResult(RagSearchResult result, SysRagDoc doc)
    {
        result.setDocId(doc.getDocId());
        result.setTitle(doc.getDocName());
        result.setScopeCode(doc.getScopeCode());
        result.setLevel(doc.getSecurityLevel());
        result.setOwnerGroupCode(doc.getOwnerGroupCode());
        result.setOwnerGroupName(doc.getOwnerGroupName());
        result.setMetadataStatus(doc.getMetadataStatus());
    }

    private void rejectAll(List<RagSearchResult> results, String blockedReason, String reason)
    {
        for (RagSearchResult result : results)
        {
            if (result != null)
            {
                reject(result, blockedReason, reason);
            }
        }
    }

    private void pass(RagSearchResult result, String reason)
    {
        result.setPassed(true);
        result.setBlockedReason("");
        result.setFilterReason(reason);
    }

    private void reject(RagSearchResult result, String blockedReason, String reason)
    {
        result.setPassed(false);
        result.setBlockedReason(blockedReason);
        result.setFilterReason(reason);
    }

    private boolean isActive(String metadataStatus)
    {
        return "ACTIVE".equalsIgnoreCase(metadataStatus);
    }

    private boolean isPublicResource(RagSearchResult result)
    {
        if (result == null)
        {
            return false;
        }

        return "PUBLIC".equalsIgnoreCase(result.getScopeCode())
                || "PUBLIC".equalsIgnoreCase(result.getLevel())
                || "GROUP_PUBLIC".equalsIgnoreCase(result.getOwnerGroupCode());
    }

    private boolean isInWorkTime()
    {
        LocalTime now = LocalTime.now();
        return !now.isBefore(WORK_START) && !now.isAfter(WORK_END);
    }

    private int secretRank(String level)
    {
        if ("PUBLIC".equalsIgnoreCase(level)) return 1;
        if ("INTERNAL".equalsIgnoreCase(level)) return 2;
        if ("SECRET".equalsIgnoreCase(level)) return 3;
        if ("CONFIDENTIAL".equalsIgnoreCase(level)) return 4;
        return 0;
    }

    private String toStr(Object obj)
    {
        return obj == null ? null : String.valueOf(obj);
    }
}
