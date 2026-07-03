package com.ruoyi.system.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * RAG检索审计日志对象 sys_rag_audit_log
 * 
 * @author zhangnuo
 * @date 2026-05-05
 */
public class SysRagAuditLog extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 用户ID */
    @Excel(name = "用户ID")
    private Long userId;

    /** 用户名 */
    @Excel(name = "用户名")
    private String userName;

    /** 原始查询内容 */
    @Excel(name = "原始查询内容")
    private String queryText;

    /** 用户组编码集合 */
    @Excel(name = "用户组编码集合")
    private String groupCodes;

    /** 知悉范围编码集合 */
    @Excel(name = "知悉范围编码集合")
    private String scopeCodes;

    /** 用户权限上下文JSON */
    private String userContextJson;

    /** 请求JSON */
    private String requestJson;

    /** RAG Server原始候选结果JSON */
    private String rawResultsJson;

    /** 二次过滤通过结果JSON */
    private String passedResultsJson;

    /** 二次过滤拦截结果JSON */
    private String blockedResultsJson;

    /** 最终返回前端响应JSON */
    private String responseJson;

    /** 元数据过滤条件 */
    @Excel(name = "元数据过滤条件")
    private String metadataFilter;

    /** 是否放行（0放行 1拒绝） */
    @Excel(name = "是否放行", readConverterExp = "1=放行,0=拒绝")
    private String allowAccess;

    /** 拒绝原因 */
    @Excel(name = "拒绝原因")
    private String denyReasons;

    /** 风险分数 */
    @Excel(name = "风险分数")
    private Integer riskScore;

    /** 是否受限查询 */
    @Excel(name = "是否受限查询", readConverterExp = "1=是,0=否")
    private String limitedQuery;

    /** 通过结果数量 */
    @Excel(name = "通过结果数量")
    private Integer passedCount;

    /** 拦截结果数量 */
    @Excel(name = "拦截结果数量")
    private Integer blockedCount;

    /** 拦截原因汇总 */
    @Excel(name = "拦截原因汇总")
    private String blockedReasons;

    /** 查询安全上下文JSON */
    private String secureContextJson;

    /** 耗时，单位毫秒 */
    @Excel(name = "耗时，单位毫秒")
    private Long costTime;

    public void setId(Long id) 
    {
        this.id = id;
    }

    public Long getId() 
    {
        return id;
    }

    public void setUserId(Long userId) 
    {
        this.userId = userId;
    }

    public Long getUserId() 
    {
        return userId;
    }

    public void setUserName(String userName) 
    {
        this.userName = userName;
    }

    public String getUserName() 
    {
        return userName;
    }

    public void setQueryText(String queryText) 
    {
        this.queryText = queryText;
    }

    public String getQueryText() 
    {
        return queryText;
    }

    public void setGroupCodes(String groupCodes) 
    {
        this.groupCodes = groupCodes;
    }

    public String getGroupCodes() 
    {
        return groupCodes;
    }

    public void setScopeCodes(String scopeCodes) 
    {
        this.scopeCodes = scopeCodes;
    }

    public String getScopeCodes() 
    {
        return scopeCodes;
    }


    public void setUserContextJson(String userContextJson)
    {
        this.userContextJson = userContextJson;
    }

    public String getUserContextJson()
    {
        return userContextJson;
    }

    public void setRequestJson(String requestJson)
    {
        this.requestJson = requestJson;
    }

    public String getRequestJson()
    {
        return requestJson;
    }

    public void setRawResultsJson(String rawResultsJson)
    {
        this.rawResultsJson = rawResultsJson;
    }

    public String getRawResultsJson()
    {
        return rawResultsJson;
    }

    public void setPassedResultsJson(String passedResultsJson)
    {
        this.passedResultsJson = passedResultsJson;
    }

    public String getPassedResultsJson()
    {
        return passedResultsJson;
    }

    public void setBlockedResultsJson(String blockedResultsJson)
    {
        this.blockedResultsJson = blockedResultsJson;
    }

    public String getBlockedResultsJson()
    {
        return blockedResultsJson;
    }

    public void setResponseJson(String responseJson)
    {
        this.responseJson = responseJson;
    }

    public String getResponseJson()
    {
        return responseJson;
    }

    public void setMetadataFilter(String metadataFilter) 
    {
        this.metadataFilter = metadataFilter;
    }

    public String getMetadataFilter() 
    {
        return metadataFilter;
    }

    public void setAllowAccess(String allowAccess) 
    {
        this.allowAccess = allowAccess;
    }

    public String getAllowAccess() 
    {
        return allowAccess;
    }

    public void setDenyReasons(String denyReasons) 
    {
        this.denyReasons = denyReasons;
    }

    public String getDenyReasons() 
    {
        return denyReasons;
    }

    public Integer getRiskScore()
    {
        return riskScore;
    }

    public void setRiskScore(Integer riskScore)
    {
        this.riskScore = riskScore;
    }

    public String getLimitedQuery()
    {
        return limitedQuery;
    }

    public void setLimitedQuery(String limitedQuery)
    {
        this.limitedQuery = limitedQuery;
    }

    public Integer getPassedCount()
    {
        return passedCount;
    }

    public void setPassedCount(Integer passedCount)
    {
        this.passedCount = passedCount;
    }

    public Integer getBlockedCount()
    {
        return blockedCount;
    }

    public void setBlockedCount(Integer blockedCount)
    {
        this.blockedCount = blockedCount;
    }

    public String getBlockedReasons()
    {
        return blockedReasons;
    }

    public void setBlockedReasons(String blockedReasons)
    {
        this.blockedReasons = blockedReasons;
    }

    public String getSecureContextJson()
    {
        return secureContextJson;
    }

    public void setSecureContextJson(String secureContextJson)
    {
        this.secureContextJson = secureContextJson;
    }

    public void setCostTime(Long costTime) 
    {
        this.costTime = costTime;
    }

    public Long getCostTime() 
    {
        return costTime;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("userId", getUserId())
            .append("userName", getUserName())
            .append("queryText", getQueryText())
            .append("groupCodes", getGroupCodes())
            .append("scopeCodes", getScopeCodes())
            .append("userContextJson", getUserContextJson())
            .append("metadataFilter", getMetadataFilter())
            .append("requestJson", getRequestJson())
            .append("rawResultsJson", getRawResultsJson())
            .append("passedResultsJson", getPassedResultsJson())
            .append("blockedResultsJson", getBlockedResultsJson())
            .append("responseJson", getResponseJson())
            .append("allowAccess", getAllowAccess())
            .append("denyReasons", getDenyReasons())
            .append("riskScore", getRiskScore())
            .append("limitedQuery", getLimitedQuery())
            .append("passedCount", getPassedCount())
            .append("blockedCount", getBlockedCount())
            .append("blockedReasons", getBlockedReasons())
            .append("secureContextJson", getSecureContextJson())
            .append("costTime", getCostTime())
            .append("createTime", getCreateTime())
            .toString();
    }
}
