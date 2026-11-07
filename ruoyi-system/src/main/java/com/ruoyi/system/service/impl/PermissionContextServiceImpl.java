package com.ruoyi.system.service.impl;

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
import com.ruoyi.system.domain.permission.AccessPolicyRule;
import com.ruoyi.system.mapper.PermissionContextMapper;
import com.ruoyi.system.service.IPermissionContextService;

/** 使用当前人员归属和可信请求来源构建附加策略上下文。 */
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
        org.springframework.web.context.request.RequestAttributes attributes =
                org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
        javax.servlet.http.HttpServletRequest request = attributes instanceof org.springframework.web.context.request.ServletRequestAttributes
                ? ((org.springframework.web.context.request.ServletRequestAttributes) attributes).getRequest() : null;
        context.setRequestIp(com.ruoyi.common.utils.ip.IpUtils.getIpAddr(request));
        if (policies != null && policies.stream().anyMatch(policy -> policy != null && AccessPolicyRule.requiresRisk(policy.getEnvExpr())))
            context.setUserRiskLevel(permissionContextMapper.selectUserRiskLevel(userId));

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

    private void evaluateBasicRules(PermissionContext context)
    {
        String denial = AccessPolicyRule.admissionDenial(context);
        context.setAllowAccess(denial == null);
        if (denial != null) context.getDenyReasons().add(denial);
    }
}
