package com.ruoyi.system.domain.rag;

import java.io.Serializable;

/**
 * RAG 检索请求对象
 *
 * 支持两种检索模式：
 * 1. useRemote = false：使用 sys_rag_doc 模拟候选结果，保证平台侧演示稳定；
 * 2. useRemote = true：调用 RAG Server 真实检索接口，进入真实联调链路。
 */
public class RagSearchRequest implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 用户原始查询内容 */
    private String query;

    /** 期望召回条数 */
    private Integer topK;

    /** 是否调用远程 RAG Server */
    private Boolean useRemote;

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

    public Boolean getUseRemote()
    {
        return useRemote;
    }

    public void setUseRemote(Boolean useRemote)
    {
        this.useRemote = useRemote;
    }
}
