package com.ruoyi.system.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * VACP 查询安全上下文 Mapper
 */
public interface SecureQueryContextMapper
{
    /**
     * 查询用户安全属性
     */
    @Select("select user_id, user_name, secret_level, access_status, risk_level, fail_count " +
            "from sys_user_security_attr where user_id = #{userId} limit 1")
    Map<String, Object> selectUserSecurityAttr(@Param("userId") Long userId);

    /**
     * 查询用户所属有效用户组编码
     */
    @Select("select g.group_code " +
            "from sys_user_group_rel r " +
            "inner join sys_group g on r.group_id = g.id " +
            "where r.user_id = #{userId} and g.status = '0' and g.del_flag = '0'")
    List<String> selectGroupCodesByUserId(@Param("userId") Long userId);

    /**
     * 查询用户可访问知悉范围
     */
    @Select("select distinct g.scope_code " +
            "from sys_user_group_rel r " +
            "inner join sys_group g on r.group_id = g.id " +
            "where r.user_id = #{userId} and g.status = '0' and g.del_flag = '0' and g.scope_code is not null")
    List<String> selectScopeCodesByUserId(@Param("userId") Long userId);
}
