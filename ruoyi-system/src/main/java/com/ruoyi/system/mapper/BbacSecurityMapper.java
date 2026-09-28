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
     * 查询 IP 是否处于启用状态的白名单。
     */
    @Select("select count(1) from sys_ip_whitelist where ipaddr = #{ip} and status = '0'")
    int countActiveWhitelistIp(@Param("ip") String ip);

    /**
     * 访问被拒绝后记录一次连续失败。
     * 第 5 次连续失败时立即锁定用户 30 分钟。
     */
    @Update("update sys_user_security_attr set " +
            "access_status = case when coalesce(fail_count, 0) + 1 >= 5 then 'LOCKED' else access_status end, " +
            "lock_until = case when coalesce(fail_count, 0) + 1 >= 5 then date_add(now(), interval 30 minute) else lock_until end, " +
            "lock_reason = case when coalesce(fail_count, 0) + 1 >= 5 then 'BBAC_CONTINUOUS_FAILURE_GE_5' else lock_reason end, " +
            "fail_count = least(coalesce(fail_count, 0) + 1, 999), " +
            "update_by = 'system', update_time = now() " +
            "where user_id = #{userId}")
    int increaseFailCount(@Param("userId") Long userId);

    /**
     * 一次正常访问成功后，连续失败次数清零。
     */
    @Update("update sys_user_security_attr " +
            "set fail_count = 0, update_by = 'system', update_time = now() " +
            "where user_id = #{userId} and access_status <> 'LOCKED' and fail_count <> 0")
    int resetFailCount(@Param("userId") Long userId);

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
            "set access_status = 'ACTIVE', fail_count = 0, lock_until = null, lock_reason = null, " +
            "update_by = 'system', update_time = now() " +
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
