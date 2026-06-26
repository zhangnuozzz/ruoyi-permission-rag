package com.ruoyi.system.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * RAG行为分析告警对象 sys_rag_behavior_alert
 */
public class SysRagBehaviorAlert extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;

    @Excel(name = "来源审计日志ID")
    private Long sourceLogId;

    @Excel(name = "用户ID")
    private Long userId;

    @Excel(name = "用户名")
    private String userName;

    @Excel(name = "告警类型")
    private String alertType;

    @Excel(name = "告警等级")
    private String alertLevel;

    @Excel(name = "告警原因")
    private String alertReason;

    @Excel(name = "触发检索内容")
    private String queryText;

    @Excel(name = "访问决策", readConverterExp = "1=放行,0=拒绝")
    private String allowAccess;

    @Excel(name = "耗时毫秒")
    private Long costTime;

    @Excel(name = "处理状态")
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getSourceLogId() { return sourceLogId; }
    public void setSourceLogId(Long sourceLogId) { this.sourceLogId = sourceLogId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getAlertType() { return alertType; }
    public void setAlertType(String alertType) { this.alertType = alertType; }

    public String getAlertLevel() { return alertLevel; }
    public void setAlertLevel(String alertLevel) { this.alertLevel = alertLevel; }

    public String getAlertReason() { return alertReason; }
    public void setAlertReason(String alertReason) { this.alertReason = alertReason; }

    public String getQueryText() { return queryText; }
    public void setQueryText(String queryText) { this.queryText = queryText; }

    public String getAllowAccess() { return allowAccess; }
    public void setAllowAccess(String allowAccess) { this.allowAccess = allowAccess; }

    public Long getCostTime() { return costTime; }
    public void setCostTime(Long costTime) { this.costTime = costTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public Date getCreateTime() { return createTime; }
    @Override
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
}
