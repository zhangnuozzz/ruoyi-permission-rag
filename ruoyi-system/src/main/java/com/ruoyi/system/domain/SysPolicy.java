package com.ruoyi.system.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 策略管理对象 sys_policy
 * 
 * @author zhangnuo
 * @date 2026-04-20
 */
public class SysPolicy extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 策略编码 */
    @Excel(name = "策略编码")
    private String policyCode;

    /** 策略名称 */
    @Excel(name = "策略名称")
    private String policyName;

    /** 策略效果（0允许 1拒绝） */
    @Excel(name = "策略效果", readConverterExp = "0=允许,1=拒绝")
    private String effect;

    /** 主体类型（USER/GROUP） */
    @Excel(name = "主体类型", readConverterExp = "U=SER/GROUP")
    private String subjectType;

    /** 主体条件表达式 */
    @Excel(name = "主体条件表达式")
    private String subjectExpr;

    /** 资源条件表达式 */
    @Excel(name = "资源条件表达式")
    private String resourceExpr;

    /** 环境条件表达式 */
    @Excel(name = "环境条件表达式")
    private String envExpr;

    /** 优先级，数值越小优先级越高 */
    @Excel(name = "优先级，数值越小优先级越高")
    private Long priority;

    /** 状态（0正常 1停用） */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;

    /** 删除标志（0存在 2删除） */
    private String delFlag;

    public void setId(Long id) 
    {
        this.id = id;
    }

    public Long getId() 
    {
        return id;
    }

    public void setPolicyCode(String policyCode) 
    {
        this.policyCode = policyCode;
    }

    public String getPolicyCode() 
    {
        return policyCode;
    }

    public void setPolicyName(String policyName) 
    {
        this.policyName = policyName;
    }

    public String getPolicyName() 
    {
        return policyName;
    }

    public void setEffect(String effect) 
    {
        this.effect = effect;
    }

    public String getEffect() 
    {
        return effect;
    }

    public void setSubjectType(String subjectType) 
    {
        this.subjectType = subjectType;
    }

    public String getSubjectType() 
    {
        return subjectType;
    }

    public void setSubjectExpr(String subjectExpr) 
    {
        this.subjectExpr = subjectExpr;
    }

    public String getSubjectExpr() 
    {
        return subjectExpr;
    }

    public void setResourceExpr(String resourceExpr) 
    {
        this.resourceExpr = resourceExpr;
    }

    public String getResourceExpr() 
    {
        return resourceExpr;
    }

    public void setEnvExpr(String envExpr) 
    {
        this.envExpr = envExpr;
    }

    public String getEnvExpr() 
    {
        return envExpr;
    }

    public void setPriority(Long priority) 
    {
        this.priority = priority;
    }

    public Long getPriority() 
    {
        return priority;
    }

    public void setStatus(String status) 
    {
        this.status = status;
    }

    public String getStatus() 
    {
        return status;
    }

    public void setDelFlag(String delFlag) 
    {
        this.delFlag = delFlag;
    }

    public String getDelFlag() 
    {
        return delFlag;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("policyCode", getPolicyCode())
            .append("policyName", getPolicyName())
            .append("effect", getEffect())
            .append("subjectType", getSubjectType())
            .append("subjectExpr", getSubjectExpr())
            .append("resourceExpr", getResourceExpr())
            .append("envExpr", getEnvExpr())
            .append("priority", getPriority())
            .append("status", getStatus())
            .append("remark", getRemark())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("delFlag", getDelFlag())
            .toString();
    }
}
