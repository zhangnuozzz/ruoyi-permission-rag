package com.ruoyi.system.domain.rag;

import java.io.Serializable;

/**
 * RAG 检索结果对象
 *
 * 用于模拟向量数据库返回的文档片段结果。
 * 后续接入 Milvus 后，可由真实检索结果转换得到该对象。
 */
public class RagSearchResult implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 文档ID */
    private String docId;

    /** 切块ID */
    private String chunkId;

    /** 文档标题 */
    private String title;

    /** 文档片段内容 */
    private String content;

    /** 文档片段摘要 */
    private String summary;

    /** 知悉范围编码 */
    private String scopeCode;

    /** 密级 */
    private String level;

    /** 是否通过二次过滤 */
    private Boolean passed;

    /** 过滤说明 */
    private String filterReason;

    /** 向量相似度分数 */
    private Float score;

    public String getDocId()
    {
        return docId;
    }

    public void setDocId(String docId)
    {
        this.docId = docId;
    }

    public String getChunkId()
    {
        return chunkId;
    }

    public void setChunkId(String chunkId)
    {
        this.chunkId = chunkId;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getContent()
    {
        return content;
    }

    public void setContent(String content)
    {
        this.content = content;
    }

    public String getSummary()
    {
        return summary;
    }

    public void setSummary(String summary)
    {
        this.summary = summary;
    }

    public String getScopeCode()
    {
        return scopeCode;
    }

    public void setScopeCode(String scopeCode)
    {
        this.scopeCode = scopeCode;
    }

    public String getLevel()
    {
        return level;
    }

    public void setLevel(String level)
    {
        this.level = level;
    }

    public Boolean getPassed()
    {
        return passed;
    }

    public void setPassed(Boolean passed)
    {
        this.passed = passed;
    }

    public String getFilterReason()
    {
        return filterReason;
    }

    public void setFilterReason(String filterReason)
    {
        this.filterReason = filterReason;
    }

    public Float getScore()
    {
        return score;
    }

    public void setScore(Float score)
    {
        this.score = score;
    }
}
