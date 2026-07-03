package com.ruoyi.system.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * VACP 访问决策 Mapper
 */
public interface AccessDecisionMapper
{
    /**
     * 查询用户安全属性
     */
    @Select("select user_id, user_name, secret_level, access_status, access_start_time, access_end_time, " +
            "risk_level, fail_count, department, phone, employee_no, last_access_ip, last_access_time " +
            "from sys_user_security_attr where user_id = #{userId} limit 1")
    Map<String, Object> selectUserSecurityAttr(@Param("userId") Long userId);

    /**
     * 查询文档元数据
     */
    @Select("select id, doc_id, doc_name, upload_user_id, upload_user_name, scope_code, security_level, " +
            "owner_group_code, doc_group_id, owner_group_name, owner_group_secret_level, metadata_status, status " +
            "from sys_rag_doc " +
            "where del_flag = '0' and (doc_id = #{docId} or cast(id as char) = #{docId}) limit 1")
    Map<String, Object> selectDocMetadata(@Param("docId") String docId);

    /**
     * 查询用户所属用户组编码
     */
    @Select("select g.group_code " +
            "from sys_user_group_rel r " +
            "inner join sys_group g on r.group_id = g.id " +
            "where r.user_id = #{userId} and g.status = '0' and g.del_flag = '0'")
    List<String> selectUserGroupCodes(@Param("userId") Long userId);

    /**
     * 查询用户在指定用户组上的组密级。
     * 用于实现：用户对应用户组群和文档用户组密级共同的用户组的密级 >= 文档密级。
     */
    @Select("select g.group_secret_level " +
            "from sys_user_group_rel r " +
            "inner join sys_group g on r.group_id = g.id " +
            "where r.user_id = #{userId} " +
            "and g.group_code = #{groupCode} " +
            "and g.status = '0' and g.del_flag = '0' " +
            "limit 1")
    String selectUserGroupSecretLevel(@Param("userId") Long userId, @Param("groupCode") String groupCode);
}
