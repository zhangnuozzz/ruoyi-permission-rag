package com.ruoyi.system.domain;

/**
 * RAG文件处理结果
 *
 * @author fufu
 * @date 2026-05-05
 */
public class RagFileProcessResult
{
    private String fileId;
    private String fileName;
    private String securityLevel;
    private String groupId;
    private String groupName;
    private String minioObjectName;
    private int chunkCount;

    public String getFileId()
    {
        return fileId;
    }

    public void setFileId(String fileId)
    {
        this.fileId = fileId;
    }

    public String getFileName()
    {
        return fileName;
    }

    public void setFileName(String fileName)
    {
        this.fileName = fileName;
    }

    public String getSecurityLevel()
    {
        return securityLevel;
    }

    public void setSecurityLevel(String securityLevel)
    {
        this.securityLevel = securityLevel;
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

    public String getMinioObjectName()
    {
        return minioObjectName;
    }

    public void setMinioObjectName(String minioObjectName)
    {
        this.minioObjectName = minioObjectName;
    }

    public int getChunkCount()
    {
        return chunkCount;
    }

    public void setChunkCount(int chunkCount)
    {
        this.chunkCount = chunkCount;
    }
}
