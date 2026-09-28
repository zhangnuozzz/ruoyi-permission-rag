package com.ruoyi.system.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 权限策略版本历史对象 sys_policy_version
 */
public class SysPolicyVersion
{
    private Long id;
    private Long policyId;
    private Integer versionNo;
    private String policyCode;
    private String policyName;
    private String effect;
    private String subjectType;
    private String subjectExpr;
    private String resourceExpr;
    private String envExpr;
    private Integer priority;
    private String status;
    private String remark;
    private String changeType;
    private String changeBy;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date changeTime;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getPolicyId()
    {
        return policyId;
    }

    public void setPolicyId(Long policyId)
    {
        this.policyId = policyId;
    }

    public Integer getVersionNo()
    {
        return versionNo;
    }

    public void setVersionNo(Integer versionNo)
    {
        this.versionNo = versionNo;
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

    public String getRemark()
    {
        return remark;
    }

    public void setRemark(String remark)
    {
        this.remark = remark;
    }

    public String getChangeType()
    {
        return changeType;
    }

    public void setChangeType(String changeType)
    {
        this.changeType = changeType;
    }

    public String getChangeBy()
    {
        return changeBy;
    }

    public void setChangeBy(String changeBy)
    {
        this.changeBy = changeBy;
    }

    public Date getChangeTime()
    {
        return changeTime;
    }

    public void setChangeTime(Date changeTime)
    {
        this.changeTime = changeTime;
    }
}
