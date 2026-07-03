package com.ruoyi.system.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * VACP BBAC 行为访问控制 Mapper
 *
 * 对齐原型文档：
 * 1. 校验来源 IP，黑白名单机制；
 * 2. 单位时间访问次数 <= 阈值；
 * 3. 连续访问失败次数 >= N -> 临时封禁；
 * 4. 重复 query pattern -> 限制访问。
 */
public interface BbacSecurityMapper
{
    /**
     * 查询 IP 是否处于启用状态的黑名单。
     */
    @Select("select count(1) from sys_ip_blacklist where ipaddr = #{ip} and status = '0'")
    int countActiveBlacklistIp(@Param("ip") String ip);

    /**
     * 统计当前用户 1 分钟内 RAG 请求次数。
     */
    @Select("select count(1) from sys_rag_audit_log " +
            "where user_id = #{userId} and create_time >= date_sub(now(), interval 1 minute)")
    int countUserRequestsLastMinute(@Param("userId") Long userId);

    /**
     * 统计当前用户 5 分钟内重复 query pattern 次数。
     */
    @Select("select count(1) from sys_rag_audit_log " +
            "where user_id = #{userId} " +
            "and query_text = #{queryText} " +
            "and create_time >= date_sub(now(), interval 5 minute)")
    int countRepeatedQueryLastFiveMinutes(@Param("userId") Long userId, @Param("queryText") String queryText);

    /**
     * 如果临时封禁已过期，自动恢复 ACTIVE。
     */
    @Update("update sys_user_security_attr " +
            "set access_status = 'ACTIVE', lock_until = null, lock_reason = null, update_by = 'system', update_time = now() " +
            "where user_id = #{userId} and access_status = 'LOCKED' and lock_until is not null and lock_until <= now()")
    int unlockExpiredUser(@Param("userId") Long userId);

    /**
     * 连续失败次数达到阈值后临时封禁。
     */
    @Update("update sys_user_security_attr " +
            "set access_status = 'LOCKED', lock_until = date_add(now(), interval #{minutes} minute), " +
            "lock_reason = #{reason}, update_by = 'system', update_time = now() " +
            "where user_id = #{userId}")
    int lockUserTemporarily(@Param("userId") Long userId,
                            @Param("minutes") Integer minutes,
                            @Param("reason") String reason);
}
