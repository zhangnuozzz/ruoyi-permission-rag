package com.fufu.ragserver.domain;

import java.util.List;

/**
 * RAG安全检索请求对象。
 *
 * 平台侧会在请求进入向量检索前完成身份验证、权限上下文加载和策略决策，
 * 这里接收已经改写后的安全查询请求。
 */
public class RagSearchRequest
{
    private String query;
    private Integer topK;
    private String metadataFilter;
    private List<String> scopeCodes;

    public String getQuery()
    {
        return query;
    }

    public void setQuery(String query)
    {
        this.query = query;
    }

    public Integer getTopK()
    {
        return topK;
    }

    public void setTopK(Integer topK)
    {
        this.topK = topK;
    }

    public String getMetadataFilter()
    {
        return metadataFilter;
    }

    public void setMetadataFilter(String metadataFilter)
    {
        this.metadataFilter = metadataFilter;
    }

    public List<String> getScopeCodes()
    {
        return scopeCodes;
    }

    public void setScopeCodes(List<String> scopeCodes)
    {
        this.scopeCodes = scopeCodes;
    }
}
