package com.ruoyi.system.domain.permission;

import java.io.Serializable;

/**
 * 权限策略信息对象
 *
 * 用于权限上下文中描述当前用户命中的策略。
 */
public class PolicyInfo implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 策略ID */
    private Long policyId;

    /** 策略编码 */
    private String policyCode;

    /** 策略名称 */
    private String policyName;

    /** 策略效果（0允许 1拒绝） */
    private String effect;

    /** 主体类型（USER/GROUP） */
    private String subjectType;

    /** 主体条件表达式 */
    private String subjectExpr;

    /** 资源条件表达式 */
    private String resourceExpr;

    /** 环境条件表达式 */
    private String envExpr;

    /** 优先级，数值越小优先级越高 */
    private Integer priority;

    /** 状态（0正常 1停用） */
    private String status;

    public Long getPolicyId()
    {
        return policyId;
    }

    public void setPolicyId(Long policyId)
    {
        this.policyId = policyId;
    }

    public String getPolicyCode()
    {
        return policyCode;
    }

    public void setPolicyCode(String policyCode)
    {
        this.policyCode = policyCode;
    }

    public String getPolicyName()
    {
        return policyName;
    }

    public void setPolicyName(String policyName)
    {
        this.policyName = policyName;
    }

    public String getEffect()
    {
        return effect;
    }

    public void setEffect(String effect)
    {
        this.effect = effect;
    }

    public String getSubjectType()
    {
        return subjectType;
    }

    public void setSubjectType(String subjectType)
    {
        this.subjectType = subjectType;
    }

    public String getSubjectExpr()
    {
        return subjectExpr;
    }

    public void setSubjectExpr(String subjectExpr)
    {
        this.subjectExpr = subjectExpr;
    }

    public String getResourceExpr()
    {
        return resourceExpr;
    }

    public void setResourceExpr(String resourceExpr)
    {
        this.resourceExpr = resourceExpr;
    }

    public String getEnvExpr()
    {
        return envExpr;
    }

    public void setEnvExpr(String envExpr)
    {
        this.envExpr = envExpr;
    }

    public Integer getPriority()
    {
        return priority;
    }

    public void setPriority(Integer priority)
    {
        this.priority = priority;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }
}
