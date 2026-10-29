package com.ruoyi.system.service.impl;

import java.time.LocalTime;
import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import com.ruoyi.system.domain.permission.GroupInfo;
import com.ruoyi.system.domain.permission.PermissionContext;
import com.ruoyi.system.domain.permission.PolicyInfo;
import com.ruoyi.system.domain.permission.WorkingHoursRule;
import com.ruoyi.system.mapper.PermissionContextMapper;
import com.ruoyi.system.service.IPermissionContextService;

/**
 * 权限上下文服务实现
 *
 * 第一版目标：
 * 1. 根据 userId 查询用户所属用户组；
 * 2. 根据 userId 查询通过用户/用户组绑定生效的策略；
 * 3. 汇总 groupCodes、scopeCodes、policies；
 * 4. 形成后续请求过滤与安全检索可复用的 PermissionContext。
 */
@Service
public class PermissionContextServiceImpl implements IPermissionContextService
{
    @Autowired
    private PermissionContextMapper permissionContextMapper;

    private Clock clock = Clock.systemDefaultZone();

    @Override
    public PermissionContext buildContext(Long userId, String userName, Boolean admin)
    {
        PermissionContext context = new PermissionContext();
        context.setUserId(userId);
        context.setUserName(userName);
        context.setAdmin(Boolean.TRUE.equals(admin));
        context.setRequestTime(LocalDateTime.now(clock).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        List<GroupInfo> groups = permissionContextMapper.selectGroupsByUserId(userId);
        List<PolicyInfo> policies = permissionContextMapper.selectPoliciesByUserId(userId);

        context.setGroups(groups);
        context.setPolicies(policies);

        if (!CollectionUtils.isEmpty(groups))
        {
            context.setGroupCodes(groups.stream()
                    .map(GroupInfo::getGroupCode)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList()));

            context.setScopeCodes(groups.stream()
                    .map(GroupInfo::getScopeCode)
                    .filter(Objects::nonNull)
                    .filter(scopeCode -> !"".equals(scopeCode.trim()))
                    .distinct()
                    .collect(Collectors.toList()));
        }

        evaluateBasicRules(context);

        return context;
    }

    /**
     * 第一版基础策略判断。
     *
     * 说明：
     * 这里只做最小可用判断，不引入复杂表达式引擎。
     * 后续可逐步替换为更完整的策略决策服务。
     */
    private void evaluateBasicRules(PermissionContext context)
    {
        /*
         * 用户组不是“是否能够使用系统”的前置条件。
         *
         * 没有用户组：
         * 仍然可以访问 PUBLIC；
         * 密级满足时仍然可以访问 INTERNAL。
         *
         * 用户组只在 SECRET / CONFIDENTIAL
         * 国家秘密文档访问时作为知悉范围使用。
         *
         * 同样，没有附加策略并不代表拒绝访问。
         * 策略用于进一步限制，而不是产生基础知悉权限。
         */

        if (context.getPolicies() == null)
        {
            context.setAllowAccess(false);
            context.getDenyReasons().add("POLICY_CONTEXT_UNAVAILABLE");
            return;
        }
        for (PolicyInfo policy : context.getPolicies())
        {
            if (policy == null || (!WorkingHoursRule.isDeny(policy.getEffect())
                    && !WorkingHoursRule.isAllow(policy.getEffect())))
            {
                context.setAllowAccess(false);
                context.getDenyReasons().add("INVALID_ACCESS_POLICY");
                return;
            }
            if (!WorkingHoursRule.isDeny(policy.getEffect()))
            {
                continue;
            }
            if (!WorkingHoursRule.supports(policy.getEffect(), policy.getResourceExpr(), policy.getEnvExpr()))
            {
                context.setAllowAccess(false);
                context.getDenyReasons().add("UNSUPPORTED_ACCESS_POLICY");
                return;
            }
            if (WorkingHoursRule.appliesToAll(policy.getResourceExpr())
                    && WorkingHoursRule.isOutside(policy.getEnvExpr(), LocalTime.now(clock)))
            {
                context.setAllowAccess(false);
                context.getDenyReasons().add("OUT_OF_WORKING_HOURS");
                return;
            }
        }
        context.setAllowAccess(true);
    }
}
