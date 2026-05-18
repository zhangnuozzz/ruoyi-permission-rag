package com.ruoyi.system.service.impl;

import com.ruoyi.system.domain.permission.PermissionContext;
import com.ruoyi.system.domain.rag.RagSearchResult;
import com.ruoyi.system.service.IRagSecondFilterService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * RAG 检索结果二次过滤 Service 实现
 *
 * 当前版本只基于 scopeCode 做最小过滤：
 * 1. 用户拥有该 scopeCode，则结果保留；
 * 2. 用户不拥有该 scopeCode，则结果丢弃；
 * 3. 后续可扩展密级 level、策略优先级、时间条件等规则。
 */
@Service
public class RagSecondFilterServiceImpl implements IRagSecondFilterService
{
    @Override
    public List<RagSearchResult> filter(PermissionContext context, List<RagSearchResult> results)
    {
        List<RagSearchResult> filtered = new ArrayList<RagSearchResult>();

        if (context == null || results == null || results.isEmpty())
        {
            return filtered;
        }

        List<String> allowedScopes = context.getScopeCodes();
        if (allowedScopes == null || allowedScopes.isEmpty())
        {
            return filtered;
        }

        for (RagSearchResult result : results)
        {
            if (result == null)
            {
                continue;
            }

            if (allowedScopes.contains(result.getScopeCode()))
            {
                result.setPassed(true);
                result.setFilterReason("二次过滤通过：用户具备该知悉范围");
                filtered.add(result);
            }
            else
            {
                result.setPassed(false);
                result.setFilterReason("二次过滤拒绝：用户不具备该知悉范围");
            }
        }

        return filtered;
    }
}
