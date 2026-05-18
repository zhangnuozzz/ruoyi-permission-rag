package com.ruoyi.system.domain.rag;

import java.io.Serializable;

/**
 * RAG 检索请求对象
 *
 * 第一阶段先只承载用户原始查询内容。
 * 后续可以扩展 topK、检索范围、模型参数、会话ID等字段。
 */
public class RagSearchRequest implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 用户原始查询内容 */
    private String query;

    public String getQuery()
    {
        return query;
    }

    public void setQuery(String query)
    {
        this.query = query;
    }
}
