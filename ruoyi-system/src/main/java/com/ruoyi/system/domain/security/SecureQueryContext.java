package com.ruoyi.system.domain.security;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * VACP 查询安全上下文
 *
 * 用于在真正执行 RAG 检索前，统一封装用户权限画像、
 * 安全查询约束、metadata filter、topK 限制和风险状态。
 */
public class SecureQueryContext implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 当前用户ID */
    private Long userId;

    /** 当前用户名 */
    private String userName;

    /** 是否管理员 */
    private Boolean admin = false;

    /** 原始查询 */
    private String rawQuery;

    /** 净化后的查询 */
    private String sanitizedQuery;

    /** 用户密级 */
    private String userSecretLevel;

    /** 用户访问状态 */
    private String userAccessStatus;

    /** 用户风险等级 */
    private String userRiskLevel;

    /** 用户所属组编码 */
    private List<String> groupCodes = new ArrayList<String>();

    /** 用户可访问知悉范围 */
    private List<String> scopeCodes = new ArrayList<String>();

    /** 文档状态约束 */
    private String metadataStatus = "ACTIVE";

    /** metadata filter */
    private String metadataFilter;

    /** 原始 topK */
    private Integer rawTopK;

    /** 安全调整后的 topK */
    private Integer safeTopK;

    /** 是否允许查询 */
    private Boolean allowQuery = true;

    /** 是否受限查询 */
    private Boolean limitedQuery = false;

    /** 风险分数 */
    private Integer riskScore = 0;

    /** 拒绝或限制原因 */
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

    public Boolean getAdmin()
    {
        return admin;
    }

    public void setAdmin(Boolean admin)
    {
        this.admin = admin;
    }

    public String getRawQuery()
    {
        return rawQuery;
    }

    public void setRawQuery(String rawQuery)
    {
        this.rawQuery = rawQuery;
    }

    public String getSanitizedQuery()
    {
        return sanitizedQuery;
    }

    public void setSanitizedQuery(String sanitizedQuery)
    {
        this.sanitizedQuery = sanitizedQuery;
    }

    public String getUserSecretLevel()
    {
        return userSecretLevel;
    }

    public void setUserSecretLevel(String userSecretLevel)
    {
        this.userSecretLevel = userSecretLevel;
    }

    public String getUserAccessStatus()
    {
        return userAccessStatus;
    }

    public void setUserAccessStatus(String userAccessStatus)
    {
        this.userAccessStatus = userAccessStatus;
    }

    public String getUserRiskLevel()
    {
        return userRiskLevel;
    }

    public void setUserRiskLevel(String userRiskLevel)
    {
        this.userRiskLevel = userRiskLevel;
    }

    public List<String> getGroupCodes()
    {
        return groupCodes;
    }

    public void setGroupCodes(List<String> groupCodes)
    {
        this.groupCodes = groupCodes;
    }

    public List<String> getScopeCodes()
    {
        return scopeCodes;
    }

    public void setScopeCodes(List<String> scopeCodes)
    {
        this.scopeCodes = scopeCodes;
    }

    public String getMetadataStatus()
    {
        return metadataStatus;
    }

    public void setMetadataStatus(String metadataStatus)
    {
        this.metadataStatus = metadataStatus;
    }

    public String getMetadataFilter()
    {
        return metadataFilter;
    }

    public void setMetadataFilter(String metadataFilter)
    {
        this.metadataFilter = metadataFilter;
    }

    public Integer getRawTopK()
    {
        return rawTopK;
    }

    public void setRawTopK(Integer rawTopK)
    {
        this.rawTopK = rawTopK;
    }

    public Integer getSafeTopK()
    {
        return safeTopK;
    }

    public void setSafeTopK(Integer safeTopK)
    {
        this.safeTopK = safeTopK;
    }

    public Boolean getAllowQuery()
    {
        return allowQuery;
    }

    public void setAllowQuery(Boolean allowQuery)
    {
        this.allowQuery = allowQuery;
    }

    public Boolean getLimitedQuery()
    {
        return limitedQuery;
    }

    public void setLimitedQuery(Boolean limitedQuery)
    {
        this.limitedQuery = limitedQuery;
    }

    public Integer getRiskScore()
    {
        return riskScore;
    }

    public void setRiskScore(Integer riskScore)
    {
        this.riskScore = riskScore;
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
