package com.ruoyi.system.domain.rag;

import java.io.Serializable;
import java.util.Date;

/**
 * RAG 检索审计日志对象
 *
 * 用于记录每一次安全检索请求的权限决策与访问行为。
 */
public class RagAuditLog implements Serializable
{
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private String userName;
    private String queryText;
    private String groupCodes;
    private String scopeCodes;
    private String metadataFilter;
    private String allowAccess;
    private String denyReasons;
    private Long costTime;
    private Date createTime;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

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

    public String getQueryText()
    {
        return queryText;
    }

    public void setQueryText(String queryText)
    {
        this.queryText = queryText;
    }

    public String getGroupCodes()
    {
        return groupCodes;
    }

    public void setGroupCodes(String groupCodes)
    {
        this.groupCodes = groupCodes;
    }

    public String getScopeCodes()
    {
        return scopeCodes;
    }

    public void setScopeCodes(String scopeCodes)
    {
        this.scopeCodes = scopeCodes;
    }

    public String getMetadataFilter()
    {
        return metadataFilter;
    }

    public void setMetadataFilter(String metadataFilter)
    {
        this.metadataFilter = metadataFilter;
    }

    public String getAllowAccess()
    {
        return allowAccess;
    }

    public void setAllowAccess(String allowAccess)
    {
        this.allowAccess = allowAccess;
    }

    public String getDenyReasons()
    {
        return denyReasons;
    }

    public void setDenyReasons(String denyReasons)
    {
        this.denyReasons = denyReasons;
    }

    public Long getCostTime()
    {
        return costTime;
    }

    public void setCostTime(Long costTime)
    {
        this.costTime = costTime;
    }

    public Date getCreateTime()
    {
        return createTime;
    }

    public void setCreateTime(Date createTime)
    {
        this.createTime = createTime;
    }
}
