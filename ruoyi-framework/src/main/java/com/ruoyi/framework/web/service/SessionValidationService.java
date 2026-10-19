package com.ruoyi.framework.web.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.system.mapper.SysRoleMapper;
import com.ruoyi.system.service.ISysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** 按当前账号与授权状态复核既有会话，不以登录时的权限快照授予访问。 */
@Service
public class SessionValidationService
{
    @Autowired
    private ISysUserService userService;

    @Autowired
    private SysRoleMapper roleMapper;

    @Autowired
    private SysPermissionService permissionService;

    public boolean validateAndRefresh(LoginUser session)
    {
        if (session == null || session.getUser() == null || session.getUser().getUserId() == null
                || session.getToken() == null || session.getToken().isEmpty()
                || session.getExpireTime() == null || session.getExpireTime() <= System.currentTimeMillis())
        {
            return false;
        }
        SysUser cached = session.getUser();
        SysUser current = userService.selectUserById(cached.getUserId());
        if (current == null || !Objects.equals(cached.getUserId(), current.getUserId())
                || !Objects.equals(cached.getUserName(), current.getUserName())
                || !"0".equals(current.getStatus()) || !"0".equals(current.getDelFlag())
                || cached.getPassword() == null || cached.getPassword().isEmpty()
                || !Objects.equals(cached.getPassword(), current.getPassword()))
        {
            return false;
        }
        List<SysRole> assigned = roleMapper.selectRolePermissionByUserId(current.getUserId());
        if (assigned == null)
        {
            throw new IllegalStateException("当前角色查询未返回有效结果");
        }
        List<SysRole> activeRoles = new ArrayList<>();
        for (SysRole role : assigned)
        {
            if (role != null && "0".equals(role.getStatus()) && "0".equals(role.getDelFlag()))
            {
                activeRoles.add(role);
            }
        }
        Set<String> permissions = permissionService.getMenuPermission(current);
        if (permissions == null)
        {
            throw new IllegalStateException("当前菜单权限查询未返回有效结果");
        }
        current.setRoles(activeRoles);
        // 全部查询成功后才替换身份快照，避免异常时保留部分刷新状态。
        session.setUser(current);
        session.setPermissions(new HashSet<>(permissions));
        return true;
    }
}
