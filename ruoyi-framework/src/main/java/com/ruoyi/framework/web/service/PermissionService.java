package com.ruoyi.framework.web.service;

import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.utils.ServletUtils;

/**
 * Spring Security 权限表达式使用的角色及操作权限校验。
 * @author ruoyi
 */
@Service("ss")
public class PermissionService
{
    private static final String ALL_PERMISSION = "*:*:*";
    private static final String SUPER_ADMIN = "admin";
    private static final String DELIMITER = ",";

    @Autowired
    private TokenService tokenService;

    public boolean hasPermi(String permission)
    {
        LoginUser loginUser = tokenService.getLoginUser(ServletUtils.getRequest());
        return loginUser != null && loginUser.getUser() != null
                && hasPermission(loginUser.getPermissions(), permission);
    }

    public boolean lacksPermi(String permission)
    {
        return !hasPermi(permission);
    }

    public boolean hasAnyPermi(String permissions)
    {
        if (permissions == null || permissions.trim().isEmpty())
        {
            return false;
        }
        LoginUser loginUser = tokenService.getLoginUser(ServletUtils.getRequest());
        if (loginUser == null || loginUser.getUser() == null)
        {
            return false;
        }
        for (String permission : permissions.split(DELIMITER))
        {
            if (hasPermission(loginUser.getPermissions(), permission))
            {
                return true;
            }
        }
        return false;
    }

    public boolean hasRole(String role)
    {
        return hasRole(tokenService.getLoginUser(ServletUtils.getRequest()), role);
    }

    public boolean lacksRole(String role)
    {
        return !hasRole(role);
    }

    public boolean hasAnyRoles(String roles)
    {
        if (roles == null || roles.trim().isEmpty())
        {
            return false;
        }
        LoginUser loginUser = tokenService.getLoginUser(ServletUtils.getRequest());
        for (String role : roles.split(DELIMITER))
        {
            if (hasRole(loginUser, role))
            {
                return true;
            }
        }
        return false;
    }

    private boolean hasRole(LoginUser loginUser, String role)
    {
        if (role == null || role.trim().isEmpty() || loginUser == null
                || loginUser.getUser() == null || CollectionUtils.isEmpty(loginUser.getUser().getRoles()))
        {
            return false;
        }
        String required = role.trim();
        for (SysRole assigned : loginUser.getUser().getRoles())
        {
            if (assigned == null || !"0".equals(assigned.getStatus()) || !"0".equals(assigned.getDelFlag()))
            {
                continue;
            }
            String key = assigned.getRoleKey();
            if (SUPER_ADMIN.equals(key) || required.equals(key))
            {
                return true;
            }
        }
        return false;
    }

    private boolean hasPermission(Set<String> permissions, String permission)
    {
        if (permission == null || permission.trim().isEmpty() || CollectionUtils.isEmpty(permissions))
        {
            return false;
        }
        return permissions.contains(ALL_PERMISSION) || permissions.contains(permission.trim());
    }
}
