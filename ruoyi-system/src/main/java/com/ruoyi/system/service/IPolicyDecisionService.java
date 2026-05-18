package com.ruoyi.system.service;

import com.ruoyi.system.domain.permission.PermissionContext;
import com.ruoyi.system.domain.permission.PolicyDecisionResult;

/**
 * 策略决策服务接口
 *
 * 用于将权限上下文转换为检索阶段可使用的安全过滤条件。
 */
public interface IPolicyDecisionService
{
    /**
     * 根据权限上下文生成策略决策结果。
     *
     * @param context 权限上下文
     * @return 策略决策结果
     */
    PolicyDecisionResult decide(PermissionContext context);
}
