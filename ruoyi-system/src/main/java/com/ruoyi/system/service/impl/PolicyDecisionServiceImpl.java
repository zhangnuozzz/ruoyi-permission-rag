package com.ruoyi.system.service.impl;

import com.ruoyi.system.domain.permission.PermissionContext;
import com.ruoyi.system.domain.permission.PolicyDecisionResult;
import com.ruoyi.system.service.IPolicyDecisionService;

import org.springframework.stereotype.Service;

/**
 * 附加策略决策。
 *
 * 核心访问资格不由策略授予：
 *
 * PUBLIC：
 * 公开数据；
 *
 * INTERNAL：
 * 人员密级；
 *
 * SECRET / CONFIDENTIAL：
 * 人员密级 + 唯一知悉用户组。
 *
 * sys_policy / sys_policy_bind 只用于追加限制，
 * 不能让一个本来没有知悉范围的人获得国家秘密访问权限。
 */
@Service
public class PolicyDecisionServiceImpl
        implements IPolicyDecisionService
{
    @Override
    public PolicyDecisionResult decide(
            PermissionContext context)
    {
        PolicyDecisionResult result =
                new PolicyDecisionResult();

        if (context == null)
        {
            result.setAllowAccess(false);
            result.getDenyReasons().add(
                    "权限上下文为空"
            );
            result.setMessage(
                    "请求被拒绝：权限上下文为空"
            );
            return result;
        }

        /*
         * PermissionContext 中命中的 DENY
         * 属于额外安全限制。
         */
        if (Boolean.FALSE.equals(
                context.getAllowAccess()))
        {
            result.setAllowAccess(false);

            result.setDenyReasons(
                    context.getDenyReasons()
            );

            result.setMessage(
                    "请求被附加安全策略限制"
            );

            return result;
        }

        /*
         * 没有 scope、没有用户组、没有策略，
         * 均不应在这里自动拒绝。
         */
        result.setAllowAccess(true);

        result.setScopeCodes(
                context.getScopeCodes()
        );

        /*
         * 最终真正的 Milvus 前置过滤表达式
         * 由 SecureQueryContext 根据：
         *
         * 人员密级 + 用户组集合
         *
         * 统一生成。
         */
        result.setMetadataFilter(
                "chunk_id != \"\""
        );

        result.setMessage(
                "基础访问资格通过；附加策略未拒绝本次请求"
        );

        return result;
    }
}
