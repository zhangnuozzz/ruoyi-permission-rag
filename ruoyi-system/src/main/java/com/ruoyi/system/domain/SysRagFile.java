package com.ruoyi.system.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * RAG文件元数据对象 sys_rag_file
 *
 * @author fufu
 * @date 2026-05-05
 */
public class SysRagFile extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 文件ID */
    private String fileId;

    /** 文件名 */
    private String fileName;

    /** 上传文件的用户ID */
    private Long uploadUserId;

    /** 上传文件的用户账号 */
    private String uploadUserName;

    /** 文件密级 */
    private String securityLevel;

    /** 所属用户组ID */
    private String groupId;

    /** 所属用户组名称 */
    private String groupName;

    /** MinIO对象名 */
    private String minioObjectName;

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

    public Long getUploadUserId()
    {
        return uploadUserId;
    }

    public void setUploadUserId(Long uploadUserId)
    {
        this.uploadUserId = uploadUserId;
    }

    public String getUploadUserName()
    {
        return uploadUserName;
    }

    public void setUploadUserName(String uploadUserName)
    {
        this.uploadUserName = uploadUserName;
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

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("fileId", getFileId())
            .append("fileName", getFileName())
            .append("uploadUserId", getUploadUserId())
            .append("uploadUserName", getUploadUserName())
            .append("securityLevel", getSecurityLevel())
            .append("groupId", getGroupId())
            .append("groupName", getGroupName())
            .append("minioObjectName", getMinioObjectName())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .toString();
    }
}
