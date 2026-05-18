package com.ruoyi.system.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.ruoyi.system.domain.permission.GroupInfo;
import com.ruoyi.system.domain.permission.PolicyInfo;

/**
 * 权限上下文查询 Mapper
 *
 * 用于从用户组、策略、绑定关系中查询当前用户的权限基础信息。
 */
public interface PermissionContextMapper
{
    /**
     * 查询用户所属用户组。
     *
     * @param userId 用户ID
     * @return 用户组列表
     */
    List<GroupInfo> selectGroupsByUserId(@Param("userId") Long userId);

    /**
     * 查询用户生效策略。
     *
     * 支持两类绑定：
     * 1. 策略直接绑定用户；
     * 2. 策略绑定用户组，用户通过用户组继承策略。
     *
     * @param userId 用户ID
     * @return 策略列表
     */
    List<PolicyInfo> selectPoliciesByUserId(@Param("userId") Long userId);
}
