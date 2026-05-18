package com.ruoyi.system.service;

import com.ruoyi.system.domain.permission.PermissionContext;

/**
 * 权限上下文服务接口
 *
 * 用于根据当前登录用户组装权限上下文。
 */
public interface IPermissionContextService
{
    /**
     * 根据用户信息构建权限上下文。
     *
     * @param userId 用户ID
     * @param userName 用户名
     * @param admin 是否管理员
     * @return 权限上下文
     */
    PermissionContext buildContext(Long userId, String userName, Boolean admin);
}
