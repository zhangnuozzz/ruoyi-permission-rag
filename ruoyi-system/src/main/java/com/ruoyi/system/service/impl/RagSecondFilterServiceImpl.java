package com.ruoyi.system.service.impl;

import com.ruoyi.system.domain.SysRagDoc;
import com.ruoyi.system.domain.permission.PermissionContext;
import com.ruoyi.system.domain.rag.RagSearchResult;
import com.ruoyi.system.mapper.AccessDecisionMapper;
import com.ruoyi.system.mapper.SysRagDocMapper;
import com.ruoyi.system.service.IRagSecondFilterService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.sql.Time;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * RAG 检索结果二次权限过滤。
 *
 * PUBLIC：
 * 公开非密，不检查用户组。
 *
 * INTERNAL：
 * 工作秘密，只检查人员密级。
 *
 * SECRET / CONFIDENTIAL：
 * 同时检查人员密级和唯一知悉用户组。
 *
 * scope_code、group_secret_level 不参与核心鉴权。
 */
@Service
public class RagSecondFilterServiceImpl
        implements IRagSecondFilterService
{
    @Autowired
    private SysRagDocMapper sysRagDocMapper;

    @Autowired
    private AccessDecisionMapper accessDecisionMapper;

    @Override
    public List<RagSearchResult> filter(
            PermissionContext context,
            List<RagSearchResult> results)
    {
        List<RagSearchResult> filtered =
                new ArrayList<RagSearchResult>();

        if (results == null || results.isEmpty())
        {
            return filtered;
        }

        Long userId =
                context == null
                        ? null
                        : context.getUserId();

        Map<String, Object> userAttr =
                userId == null
                        ? null
                        : accessDecisionMapper
                        .selectUserSecurityAttr(userId);

        List<String> userGroups =
                userId == null
                        ? new ArrayList<String>()
                        : accessDecisionMapper
                        .selectUserGroupCodes(userId);

        for (RagSearchResult result : results)
        {
            if (result == null)
            {
                continue;
            }

            SysRagDoc doc = null;

            if (result.getDocId() != null
                    && result.getDocId().length() > 0)
            {
                doc = sysRagDocMapper
                        .selectSysRagDocByDocId(
                                result.getDocId()
                        );
            }

            if (doc == null)
            {
                reject(
                        result,
                        "DOCUMENT_METADATA_NOT_FOUND",
                        "未找到平台文档安全元数据"
                );
                continue;
            }

            enrichResult(result, doc);

            String level =
                    normalizeLevel(
                            result.getLevel()
                    );

            if ("UNKNOWN".equals(level))
            {
                reject(result, "INVALID_DOCUMENT_SECURITY_LEVEL", "文档密级缺失或非法");
                continue;
            }

            /*
             * 文档状态必须有效。
             */
            if (!isActive(
                    result.getMetadataStatus()))
            {
                reject(
                        result,
                        "DOCUMENT_STATUS_NOT_ACTIVE",
                        "文档状态不是 ACTIVE"
                );
                continue;
            }

            /*
             * PUBLIC：
             * 公开非密数据。
             */
            if ("PUBLIC".equals(level))
            {
                pass(
                        result,
                        "PUBLIC：公开非密数据允许访问"
                );
                filtered.add(result);
                continue;
            }

            /*
             * 非公开数据必须存在人员安全属性。
             */
            if (userAttr == null)
            {
                reject(
                        result,
                        "USER_SECURITY_ATTR_NOT_FOUND",
                        "人员安全属性不存在"
                );
                continue;
            }

            String accessStatus =
                    toStr(
                            userAttr.get(
                                    "access_status"
                            )
                    );

            if (!"ACTIVE".equalsIgnoreCase(
                    accessStatus))
            {
                reject(
                        result,
                        "USER_ACCESS_STATUS_NOT_ACTIVE",
                        "人员访问状态不是 ACTIVE"
                );
                continue;
            }

            if (!isInAccessTimeWindow(
                    userAttr.get(
                            "access_start_time"
                    ),
                    userAttr.get(
                            "access_end_time"
                    )))
            {
                reject(
                        result,
                        "OUT_OF_USER_ACCESS_TIME_WINDOW",
                        "当前时间不在人员允许访问时间窗口内"
                );
                continue;
            }

            String userLevel =
                    normalizeLevel(
                            toStr(
                                    userAttr.get(
                                            "secret_level"
                                    )
                            )
                    );

            /*
             * 所有非公开数据先检查人员密级。
             */
            if (secretRank(userLevel)
                    < secretRank(level))
            {
                reject(
                        result,
                        "USER_SECRET_LEVEL_LOWER_THAN_DOCUMENT",
                        "人员密级低于文档密级"
                );
                continue;
            }

            /*
             * INTERNAL：
             * 工作秘密，密级满足即可。
             */
            if ("INTERNAL".equals(level))
            {
                pass(
                        result,
                        "INTERNAL：工作秘密，人员密级满足"
                );
                filtered.add(result);
                continue;
            }

            /*
             * SECRET / CONFIDENTIAL：
             * 国家秘密需要唯一知悉用户组。
             */
            String documentGroup =
                    result.getOwnerGroupCode();

            if (documentGroup == null
                    || documentGroup.length() == 0)
            {
                reject(
                        result,
                        "STATE_SECRET_DOCUMENT_GROUP_NOT_CONFIGURED",
                        "国家秘密文档未配置唯一知悉用户组"
                );
                continue;
            }

            /*
             * doc.group ∈ user.groups
             */
            if (CollectionUtils.isEmpty(userGroups)
                    || !userGroups.contains(
                            documentGroup))
            {
                reject(
                        result,
                        "USER_NOT_IN_DOCUMENT_KNOWLEDGE_GROUP",
                        "人员不属于文档唯一知悉用户组"
                );
                continue;
            }

            /*
             * 不检查：
             * scope_code
             * group_secret_level
             */
            pass(
                    result,
                    "国家秘密：人员密级和知悉用户组均满足"
            );

            filtered.add(result);
        }

        return filtered;
    }

    private void enrichResult(
            RagSearchResult result,
            SysRagDoc doc)
    {
        result.setDocId(
                doc.getDocId()
        );

        result.setTitle(
                doc.getDocName()
        );

        result.setScopeCode(
                doc.getScopeCode()
        );

        result.setLevel(
                doc.getSecurityLevel()
        );

        result.setOwnerGroupCode(
                doc.getOwnerGroupCode()
        );

        result.setOwnerGroupName(
                doc.getOwnerGroupName()
        );

        result.setMetadataStatus(
                doc.getMetadataStatus()
        );
    }

    private void pass(
            RagSearchResult result,
            String reason)
    {
        result.setPassed(true);
        result.setBlockedReason("");
        result.setFilterReason(reason);
    }

    private void reject(
            RagSearchResult result,
            String blockedReason,
            String reason)
    {
        result.setPassed(false);
        result.setBlockedReason(blockedReason);
        result.setFilterReason(reason);
    }

    private boolean isActive(String status)
    {
        return "ACTIVE".equalsIgnoreCase(
                status
        );
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

    private boolean isInAccessTimeWindow(
            Object startObj,
            Object endObj)
    {
        if (startObj == null || endObj == null)
        {
            return true;
        }

        LocalTime start =
                toLocalTime(startObj);

        LocalTime end =
                toLocalTime(endObj);

        if (start == null
                || end == null
                || start.equals(end))
        {
            return true;
        }

        LocalTime now =
                LocalTime.now();

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
            return ((Time) value)
                    .toLocalTime();
        }

        try
        {
            String text =
                    String.valueOf(value);

            if (text.length() >= 8)
            {
                text =
                        text.substring(0, 8);
            }

            return LocalTime.parse(text);
        }
        catch (Exception e)
        {
            return null;
        }
    }

    private String toStr(Object value)
    {
        return value == null
                ? null
                : String.valueOf(value);
    }
}
