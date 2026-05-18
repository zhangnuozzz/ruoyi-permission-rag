package com.ruoyi.system.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 策略绑定管理对象 sys_policy_bind
 * 
 * @author zhangnuo
 * @date 2026-05-05
 */
public class SysPolicyBind extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 策略ID */
    @Excel(name = "策略ID")
    private Long policyId;

    /** 绑定类型（USER/GROUP） */
    @Excel(name = "绑定类型", readConverterExp = "U=SER/GROUP")
    private String bindType;

    /** 绑定目标ID */
    @Excel(name = "绑定目标ID")
    private Long bindTargetId;

    /** 状态（0正常 1停用） */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;

    public void setId(Long id) 
    {
        this.id = id;
    }

    public Long getId() 
    {
        return id;
    }

    public void setPolicyId(Long policyId) 
    {
        this.policyId = policyId;
    }

    public Long getPolicyId() 
    {
        return policyId;
    }

    public void setBindType(String bindType) 
    {
        this.bindType = bindType;
    }

    public String getBindType() 
    {
        return bindType;
    }

    public void setBindTargetId(Long bindTargetId) 
    {
        this.bindTargetId = bindTargetId;
    }

    public Long getBindTargetId() 
    {
        return bindTargetId;
    }

    public void setStatus(String status) 
    {
        this.status = status;
    }

    public String getStatus() 
    {
        return status;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("policyId", getPolicyId())
            .append("bindType", getBindType())
            .append("bindTargetId", getBindTargetId())
            .append("status", getStatus())
            .append("remark", getRemark())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .toString();
    }
}
