package com.ruoyi.system.service.impl;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.system.domain.permission.GroupInfo;
import com.ruoyi.system.domain.permission.PermissionContext;
import com.ruoyi.system.domain.permission.PolicyInfo;
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

    @Override
    public PermissionContext buildContext(Long userId, String userName, Boolean admin)
    {
        PermissionContext context = new PermissionContext();
        context.setUserId(userId);
        context.setUserName(userName);
        context.setAdmin(Boolean.TRUE.equals(admin));
        context.setRequestTime(DateUtils.getTime());

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
        if (Boolean.TRUE.equals(context.getAdmin()))
        {
            context.setAllowAccess(true);
            return;
        }

        if (CollectionUtils.isEmpty(context.getGroups()))
        {
            context.setAllowAccess(false);
            context.getDenyReasons().add("NO_GROUP_ASSIGNED");
            return;
        }

        if (CollectionUtils.isEmpty(context.getPolicies()))
        {
            context.setAllowAccess(false);
            context.getDenyReasons().add("NO_EFFECTIVE_POLICY");
            return;
        }

        // 第一版仅识别简单的“非工作时间禁止访问”表达式。
        // 例如：time not in [09:00-18:00]
        for (PolicyInfo policy : context.getPolicies())
        {
            if ("1".equals(policy.getEffect()) && containsOffHoursRule(policy.getEnvExpr()))
            {
                if (isOutsideWorkingHours())
                {
                    context.setAllowAccess(false);
                    context.getDenyReasons().add("OUT_OF_WORKING_HOURS");
                    return;
                }
            }
        }

        context.setAllowAccess(true);
    }

    private boolean containsOffHoursRule(String envExpr)
    {
        if (envExpr == null)
        {
            return false;
        }
        return envExpr.contains("time not in") && envExpr.contains("09:00-18:00");
    }

    private boolean isOutsideWorkingHours()
    {
        LocalTime now = LocalTime.now();
        LocalTime start = LocalTime.parse("09:00", DateTimeFormatter.ofPattern("HH:mm"));
        LocalTime end = LocalTime.parse("18:00", DateTimeFormatter.ofPattern("HH:mm"));
        return now.isBefore(start) || now.isAfter(end);
    }
}
