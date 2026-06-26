package com.ruoyi.system.domain.rag;

import java.io.Serializable;
import java.util.Date;

/**
 * RAG 检索审计日志对象
 *
 * 用于记录每一次安全检索请求的权限决策、过滤结果与完整 JSON 审计信息。
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

    /** 用户权限上下文 JSON */
    private String userContextJson;

    private String metadataFilter;

    /** RAG 检索请求 JSON */
    private String requestJson;

    /** RAG Server 原始候选结果 JSON */
    private String rawResultsJson;

    /** 二次过滤通过结果 JSON */
    private String passedResultsJson;

    /** 二次过滤拦截结果 JSON */
    private String blockedResultsJson;

    /** 最终返回前端响应 JSON */
    private String responseJson;

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

    public String getUserContextJson()
    {
        return userContextJson;
    }

    public void setUserContextJson(String userContextJson)
    {
        this.userContextJson = userContextJson;
    }

    public String getMetadataFilter()
    {
        return metadataFilter;
    }

    public void setMetadataFilter(String metadataFilter)
    {
        this.metadataFilter = metadataFilter;
    }

    public String getRequestJson()
    {
        return requestJson;
    }

    public void setRequestJson(String requestJson)
    {
        this.requestJson = requestJson;
    }

    public String getRawResultsJson()
    {
        return rawResultsJson;
    }

    public void setRawResultsJson(String rawResultsJson)
    {
        this.rawResultsJson = rawResultsJson;
    }

    public String getPassedResultsJson()
    {
        return passedResultsJson;
    }

    public void setPassedResultsJson(String passedResultsJson)
    {
        this.passedResultsJson = passedResultsJson;
    }

    public String getBlockedResultsJson()
    {
        return blockedResultsJson;
    }

    public void setBlockedResultsJson(String blockedResultsJson)
    {
        this.blockedResultsJson = blockedResultsJson;
    }

    public String getResponseJson()
    {
        return responseJson;
    }

    public void setResponseJson(String responseJson)
    {
        this.responseJson = responseJson;
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
