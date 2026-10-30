package com.ruoyi.system.domain;

import com.ruoyi.common.annotation.Excel;

/** 策略导出快照，区分保存状态与可执行规则。 */
public final class SysPolicyExportRow extends SysPolicy
{
    private static final long serialVersionUID = 1L;

    @Excel(name = "规则状态", readConverterExp = "READY=可执行（需有效绑定）,DISABLED=已停用,LEGACY=历史配置（不授权）,INVALID=规则无效", width = 30)
    private final String ruleState;

    public SysPolicyExportRow(SysPolicy policy)
    {
        setPolicyCode(policy.getPolicyCode());
        setPolicyName(policy.getPolicyName());
        setEffect(policy.getEffect());
        setSubjectType(policy.getSubjectType());
        setSubjectExpr(policy.getSubjectExpr());
        setResourceExpr(policy.getResourceExpr());
        setEnvExpr(policy.getEnvExpr());
        setPriority(policy.getPriority());
        setStatus(policy.getStatus());
        ruleState = policy.getConfigurationState();
    }
}
