package com.fufu.ragserver.domain;

import java.util.Date;

/**
 * RAG文件元数据对象 sys_rag_file
 *
 * @author fufu
 * @date 2026-05-12
 */
public class SysRagFile
{
    private String fileId;
    private String fileName;
    private Long uploadUserId;
    private String uploadUserName;
    private String securityLevel;
    private String scopeCode;
    private String groupId;
    private String groupName;
    private String minioObjectName;
    private String createBy;
    private Date createTime;

    public String getFileId() { return fileId; }
    public void setFileId(String fileId) { this.fileId = fileId; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public Long getUploadUserId() { return uploadUserId; }
    public void setUploadUserId(Long uploadUserId) { this.uploadUserId = uploadUserId; }
    public String getUploadUserName() { return uploadUserName; }
    public void setUploadUserName(String uploadUserName) { this.uploadUserName = uploadUserName; }
    public String getSecurityLevel() { return securityLevel; }
    public void setSecurityLevel(String securityLevel) { this.securityLevel = securityLevel; }
    public String getScopeCode() { return scopeCode; }
    public void setScopeCode(String scopeCode) { this.scopeCode = scopeCode; }
    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }
    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
    public String getMinioObjectName() { return minioObjectName; }
    public void setMinioObjectName(String minioObjectName) { this.minioObjectName = minioObjectName; }
    public String getCreateBy() { return createBy; }
    public void setCreateBy(String createBy) { this.createBy = createBy; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
}
