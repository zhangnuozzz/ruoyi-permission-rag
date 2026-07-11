package com.fufu.ragserver.domain;

/**
 * RAG向量检索结果。
 */
public class RagSearchResult
{
    private String docId;
    private String chunkId;
    private String title;
    private String content;
    private String summary;
    private String scopeCode;
    private String level;
    private Float score;
    private Boolean passed;
    private String filterReason;

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

    public Float getScore()
    {
        return score;
    }

    public void setScore(Float score)
    {
        this.score = score;
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
}
