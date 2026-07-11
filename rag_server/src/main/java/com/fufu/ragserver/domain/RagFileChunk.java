package com.fufu.ragserver.domain;

/**
 * RAG文件切块写入对象
 *
 * @author fufu
 * @date 2026-05-12
 */
public class RagFileChunk
{
    private String chunkId;
    private String content;
    private String fileId;
    private String securityLevel;
    private String scopeCode;
    private String groupId;
    private String groupName;

    public String getChunkId()
    {
        return chunkId;
    }

    public void setChunkId(String chunkId)
    {
        this.chunkId = chunkId;
    }

    public String getContent()
    {
        return content;
    }

    public void setContent(String content)
    {
        this.content = content;
    }

    public String getFileId()
    {
        return fileId;
    }

    public void setFileId(String fileId)
    {
        this.fileId = fileId;
    }

    public String getSecurityLevel()
    {
        return securityLevel;
    }

    public void setSecurityLevel(String securityLevel)
    {
        this.securityLevel = securityLevel;
    }

    public String getScopeCode()
    {
        return scopeCode;
    }

    public void setScopeCode(String scopeCode)
    {
        this.scopeCode = scopeCode;
    }

    public String getGroupId()
    {
        return groupId;
    }

    public void setGroupId(String groupId)
    {
        this.groupId = groupId;
    }

    public String getGroupName()
    {
        return groupName;
    }

    public void setGroupName(String groupName)
    {
        this.groupName = groupName;
    }
}
