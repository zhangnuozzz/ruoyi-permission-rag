package com.ruoyi.system.service.impl;

import com.ruoyi.system.domain.permission.PermissionContext;
import com.ruoyi.system.domain.permission.PolicyDecisionResult;
import com.ruoyi.system.service.IPolicyDecisionService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 策略决策服务实现类
 *
 * 第一阶段只实现最小可用版本：
 * 1. 如果权限上下文本身不允许访问，则直接拒绝；
 * 2. 如果用户没有任何 scopeCode，则拒绝；
 * 3. 如果有 scopeCode，则生成 metadataFilter；
 * 4. 后续再扩展时间策略、拒绝策略优先级、复杂表达式解析。
 */
@Service
public class PolicyDecisionServiceImpl implements IPolicyDecisionService
{
    @Override
    public PolicyDecisionResult decide(PermissionContext context)
    {
        PolicyDecisionResult result = new PolicyDecisionResult();

        if (context == null)
        {
            result.setAllowAccess(false);
            result.getDenyReasons().add("权限上下文为空");
            result.setMessage("请求被拒绝：权限上下文为空");
            return result;
        }

        if (Boolean.FALSE.equals(context.getAllowAccess()))
        {
            result.setAllowAccess(false);
            result.setDenyReasons(context.getDenyReasons());
            result.setMessage("请求被拒绝：权限上下文已判定不可访问");
            return result;
        }

        List<String> scopeCodes = context.getScopeCodes();
        if (scopeCodes == null || scopeCodes.isEmpty())
        {
            result.setAllowAccess(false);
            result.getDenyReasons().add("当前用户没有可访问的知悉范围");
            result.setMessage("请求被拒绝：当前用户没有可访问的知悉范围");
            return result;
        }

        result.setAllowAccess(true);
        result.setScopeCodes(scopeCodes);
        result.setMetadataFilter(buildScopeFilter(scopeCodes));
        result.setMessage("策略决策通过，已生成安全检索过滤条件");

        return result;
    }

    /**
     * 根据 scopeCode 生成元数据过滤表达式。
     *
     * 当前先生成一个通用表达式字符串：
     * scope_code in ["INTERNAL", "DOC_ADMIN"]
     *
     * 后续接 Milvus Java SDK 时，可以根据 SDK 语法再微调。
     */
    private String buildScopeFilter(List<String> scopeCodes)
    {
        StringBuilder builder = new StringBuilder();
        builder.append("scope_code in [");

        for (int i = 0; i < scopeCodes.size(); i++)
        {
            if (i > 0)
            {
                builder.append(", ");
            }
            builder.append("\"").append(scopeCodes.get(i)).append("\"");
        }

        builder.append("]");
        return builder.toString();
    }
}
