package com.ruoyi.system.service;

import com.ruoyi.system.domain.permission.AccessDecisionResult;

/**
 * VACP 访问控制决策服务
 */
public interface IAccessDecisionService
{
    /**
     * 根据当前用户和目标文档生成访问决策
     *
     * @param userId 用户ID
     * @param userName 用户名
     * @param admin 是否管理员
     * @param docId 文档ID
     * @return 访问决策结果
     */
    AccessDecisionResult decide(Long userId, String userName, Boolean admin, String docId);
}
