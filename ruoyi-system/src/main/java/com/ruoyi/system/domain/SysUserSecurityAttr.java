package com.ruoyi.system.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 用户安全属性对象 sys_user_security_attr
 *
 * 用于 VACP 零信任安全向量检索中的用户密级、访问状态、访问时间窗口和风险属性管理。
 */
public class SysUserSecurityAttr extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;

    @Excel(name = "用户ID")
    private Long userId;

    @Excel(name = "用户名")
    private String userName;

    @Excel(name = "用户昵称")
    private String nickName;

    @Excel(name = "用户密级", readConverterExp = "PUBLIC=公开,INTERNAL=内部,SECRET=秘密,CONFIDENTIAL=机密")
    private String secretLevel;

    @Excel(name = "访问状态", readConverterExp = "ACTIVE=启用,DISABLED=禁用,LOCKED=锁定")
    private String accessStatus;

    @Excel(name = "访问开始时间")
    private String accessStartTime;

    @Excel(name = "访问结束时间")
    private String accessEndTime;

    @Excel(name = "风险等级", readConverterExp = "LOW=低,MEDIUM=中,HIGH=高")
    private String riskLevel;

    @Excel(name = "连续失败次数")
    private Integer failCount;

    @Excel(name = "最近访问IP")
    private String lastAccessIp;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "最近访问时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date lastAccessTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getNickName() { return nickName; }
    public void setNickName(String nickName) { this.nickName = nickName; }

    public String getSecretLevel() { return secretLevel; }
    public void setSecretLevel(String secretLevel) { this.secretLevel = secretLevel; }

    public String getAccessStatus() { return accessStatus; }
    public void setAccessStatus(String accessStatus) { this.accessStatus = accessStatus; }

    public String getAccessStartTime() { return accessStartTime; }
    public void setAccessStartTime(String accessStartTime) { this.accessStartTime = accessStartTime; }

    public String getAccessEndTime() { return accessEndTime; }
    public void setAccessEndTime(String accessEndTime) { this.accessEndTime = accessEndTime; }

    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }

    public Integer getFailCount() { return failCount; }
    public void setFailCount(Integer failCount) { this.failCount = failCount; }

    public String getLastAccessIp() { return lastAccessIp; }
    public void setLastAccessIp(String lastAccessIp) { this.lastAccessIp = lastAccessIp; }

    public Date getLastAccessTime() { return lastAccessTime; }
    public void setLastAccessTime(Date lastAccessTime) { this.lastAccessTime = lastAccessTime; }
}
