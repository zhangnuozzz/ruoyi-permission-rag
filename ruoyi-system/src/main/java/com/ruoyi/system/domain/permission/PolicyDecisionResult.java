package com.ruoyi.system.domain.permission;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 策略决策结果对象
 *
 * 用于描述一次检索请求经过权限策略计算后的结果。
 * 后续可用于生成 Milvus metadata filtering 条件。
 */
public class PolicyDecisionResult implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 是否允许访问 */
    private Boolean allowAccess = true;

    /** 拒绝原因 */
    private List<String> denyReasons = new ArrayList<String>();

    /** 用户可访问的知悉范围 */
    private List<String> scopeCodes = new ArrayList<String>();

    /** Milvus / 向量检索可使用的元数据过滤表达式 */
    private String metadataFilter;

    /** 决策说明 */
    private String message;

    public Boolean getAllowAccess()
    {
        return allowAccess;
    }

    public void setAllowAccess(Boolean allowAccess)
    {
        this.allowAccess = allowAccess;
    }

    public List<String> getDenyReasons()
    {
        return denyReasons;
    }

    public void setDenyReasons(List<String> denyReasons)
    {
        this.denyReasons = denyReasons;
    }

    public List<String> getScopeCodes()
    {
        return scopeCodes;
    }

    public void setScopeCodes(List<String> scopeCodes)
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

    public String getMessage()
    {
        return message;
    }

    public void setMessage(String message)
    {
        this.message = message;
    }
}
