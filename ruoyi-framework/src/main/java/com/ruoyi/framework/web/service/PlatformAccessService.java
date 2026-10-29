package com.ruoyi.framework.web.service;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.system.mapper.SysRoleMapper;

/** 管理平台与普通用户门户的接口边界。 */
@Component("platformAccess")
public class PlatformAccessService
{
    private static final Logger log = LoggerFactory.getLogger(PlatformAccessService.class);
    private static final Set<String> PORTAL_GET = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "/getInfo", "/rag/context", "/rag/file/mine/groups", "/rag/file/mine/list")));
    private static final Set<String> PORTAL_POST = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "/rag/search", "/rag/file/mine/upload", "/rag/file/mine/status")));

    @Autowired
    private SysRoleMapper roleMapper;

    public boolean canAccess(Authentication authentication, HttpServletRequest request)
    {
        if (!isAuthenticatedUser(authentication) || request == null) return false;
        String path = request.getServletPath();
        if (("GET".equals(request.getMethod()) && PORTAL_GET.contains(path))
                || ("POST".equals(request.getMethod()) && PORTAL_POST.contains(path))) return true;
        return isPlatformUser(authentication);
    }

    public boolean isPlatformUser()
    {
        return isPlatformUser(SecurityContextHolder.getContext().getAuthentication());
    }

    public boolean isPlatformUser(Authentication authentication)
    {
        if (!isAuthenticatedUser(authentication)) return false;
        SysUser user = ((LoginUser) authentication.getPrincipal()).getUser();
        if (user.isAdmin()) return true;
        try
        {
            // 使用当前角色归属，避免旧登录缓存继续保留已经撤销的管理身份。
            List<SysRole> roles = roleMapper.selectRolePermissionByUserId(user.getUserId());
            if (roles != null)
            {
                for (SysRole role : roles)
                {
                    if (role != null && "admin".equals(role.getRoleKey())
                            && "0".equals(role.getStatus()) && "0".equals(role.getDelFlag())) return true;
                }
            }
        }
        catch (Exception exception)
        {
            log.warn("核对管理身份失败，拒绝管理访问，userId={}", user.getUserId(), exception);
        }
        return false;
    }

    private boolean isAuthenticatedUser(Authentication authentication)
    {
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof LoginUser)) return false;
        SysUser user = ((LoginUser) authentication.getPrincipal()).getUser();
        return user != null && user.getUserId() != null
                && "0".equals(user.getStatus()) && "0".equals(user.getDelFlag());
    }
}
