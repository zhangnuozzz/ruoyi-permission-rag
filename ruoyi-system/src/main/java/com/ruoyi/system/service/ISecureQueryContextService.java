package com.ruoyi.system.service;

import com.ruoyi.system.domain.security.SecureQueryContext;

/**
 * VACP 查询安全上下文服务
 */
public interface ISecureQueryContextService
{
    /**
     * 构建检索前安全查询上下文。
     *
     * @param userId 用户ID
     * @param userName 用户名
     * @param admin 是否管理员
     * @param query 原始查询
     * @param topK 原始 topK
     * @return 查询安全上下文
     */
    SecureQueryContext buildContext(Long userId, String userName, Boolean admin, String query, Integer topK);
}
