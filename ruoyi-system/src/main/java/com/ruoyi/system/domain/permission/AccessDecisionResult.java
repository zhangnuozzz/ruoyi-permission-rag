package com.ruoyi.system.domain.permission;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * VACP 访问控制决策结果
 *
 * decision:
 * ALLOW   - 允许访问
 * DENY    - 拒绝访问
 * LIMITED - 受限访问
 */
public class AccessDecisionResult implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 当前用户ID */
    private Long userId;

    /** 当前用户名 */
    private String userName;

    /** 文档ID */
    private String docId;

    /** 文档名称 */
    private String docName;

    /** 决策结果：ALLOW / DENY / LIMITED */
    private String decision;

    /** 是否允许访问 */
    private Boolean allowAccess = false;

    /** 是否受限访问 */
    private Boolean limitedAccess = false;

    /** 风险分数 */
    private Integer riskScore = 0;

    /** 命中的策略类型 */
    private String policyHit;

    /** 元数据过滤表达式 */
    private String metadataFilter;

    /** 决策原因 */
    private List<String> reasons = new ArrayList<String>();

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public String getUserName()
    {
        return userName;
    }

    public void setUserName(String userName)
    {
        this.userName = userName;
    }

    public String getDocId()
    {
        return docId;
    }

    public void setDocId(String docId)
    {
        this.docId = docId;
    }

    public String getDocName()
    {
        return docName;
    }

    public void setDocName(String docName)
    {
        this.docName = docName;
    }

    public String getDecision()
    {
        return decision;
    }

    public void setDecision(String decision)
    {
        this.decision = decision;
    }

    public Boolean getAllowAccess()
    {
        return allowAccess;
    }

    public void setAllowAccess(Boolean allowAccess)
    {
        this.allowAccess = allowAccess;
    }

    public Boolean getLimitedAccess()
    {
        return limitedAccess;
    }

    public void setLimitedAccess(Boolean limitedAccess)
    {
        this.limitedAccess = limitedAccess;
    }

    public Integer getRiskScore()
    {
        return riskScore;
    }

    public void setRiskScore(Integer riskScore)
    {
        this.riskScore = riskScore;
    }

    public String getPolicyHit()
    {
        return policyHit;
    }

    public void setPolicyHit(String policyHit)
    {
        this.policyHit = policyHit;
    }

    public String getMetadataFilter()
    {
        return metadataFilter;
    }

    public void setMetadataFilter(String metadataFilter)
    {
        this.metadataFilter = metadataFilter;
    }

    public List<String> getReasons()
    {
        return reasons;
    }

    public void setReasons(List<String> reasons)
    {
        this.reasons = reasons;
    }
}
