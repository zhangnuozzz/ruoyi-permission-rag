package com.ruoyi.system.service.impl;

import com.ruoyi.system.domain.SysRagDoc;
import com.ruoyi.system.domain.permission.PermissionContext;
import com.ruoyi.system.domain.rag.RagSearchResult;
import com.ruoyi.system.mapper.SysRagDocMapper;
import com.ruoyi.system.service.IRagSecondFilterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * RAG 检索结果二次过滤 Service 实现
 *
 * VACP 安全向量检索结果级校验：
 * 1. 文档元数据必须存在；
 * 2. 文档状态必须 ACTIVE；
 * 3. 用户密级必须覆盖文档密级；
 * 4. 非管理员用户必须属于文档所属用户组；
 * 5. 非公开文档要求用户具备对应知悉范围；
 * 6. 每条被拒绝的记录都写入 blockedReason / filterReason。
 */
@Service
public class RagSecondFilterServiceImpl implements IRagSecondFilterService
{
    @Autowired
    private SysRagDocMapper sysRagDocMapper;

    @Override
    public List<RagSearchResult> filter(PermissionContext context, List<RagSearchResult> results)
    {
        List<RagSearchResult> filtered = new ArrayList<RagSearchResult>();

        if (context == null || results == null || results.isEmpty())
        {
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

        String userSecretLevel = resolveUserSecretLevel(context);

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

            // 用平台侧 sys_rag_doc 元数据覆盖检索结果元数据，避免远程返回不完整
            if (doc != null)
            {
                enrichResult(result, doc);
            }

            String metadataStatus = result.getMetadataStatus();
            String scopeCode = result.getScopeCode();
            String level = result.getLevel();
            String ownerGroupCode = result.getOwnerGroupCode();

            if (doc == null)
            {
                reject(result, "DOCUMENT_METADATA_NOT_FOUND", "二次过滤拒绝：平台侧未找到该文档元数据");
                continue;
            }

            if (!isActive(metadataStatus))
            {
                reject(result, "DOCUMENT_STATUS_NOT_ACTIVE", "二次过滤拒绝：文档状态不是 ACTIVE");
                continue;
            }

            if (secretRank(userSecretLevel) < secretRank(level))
            {
                reject(result, "USER_SECRET_LEVEL_LOWER_THAN_DOCUMENT", "二次过滤拒绝：用户密级低于文档密级");
                continue;
            }

            if (isPublicResource(result))
            {
                pass(result, "二次过滤通过：PUBLIC 公开文档默认允许登录用户访问");
                filtered.add(result);
                continue;
            }

            if (!Boolean.TRUE.equals(context.getAdmin()))
            {
                if (ownerGroupCode == null || ownerGroupCode.length() == 0 || !allowedGroups.contains(ownerGroupCode))
                {
                    reject(result, "USER_GROUP_NOT_MATCH_DOCUMENT_GROUP", "二次过滤拒绝：用户不属于文档所属用户组");
                    continue;
                }
            }

            if (scopeCode == null || scopeCode.length() == 0 || !allowedScopes.contains(scopeCode))
            {
                reject(result, "USER_SCOPE_NOT_MATCH_DOCUMENT_SCOPE", "二次过滤拒绝：用户不具备该知悉范围");
                continue;
            }

            pass(result, "二次过滤通过：文档状态、密级、用户组与知悉范围均满足要求");
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
                || "PUBLIC".equalsIgnoreCase(result.getLevel());
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

    /**
     * 当前 PermissionContext 还没有直接保存用户密级。
     * 第一版通过 admin 和业务默认值做兼容：
     * - admin 按 CONFIDENTIAL 处理；
     * - 普通用户暂按 INTERNAL 处理；
     * 后续可把 sys_user_security_attr.secret_level 注入 PermissionContext。
     */
    private String resolveUserSecretLevel(PermissionContext context)
    {
        if (context != null && Boolean.TRUE.equals(context.getAdmin()))
        {
            return "CONFIDENTIAL";
        }
        return "INTERNAL";
    }
}
