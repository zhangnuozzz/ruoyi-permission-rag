package com.ruoyi.system.domain.export;

import java.util.Date;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.system.domain.SysAccessLog;

/** 访问监控导出记录，补齐定位编号与发生时间。 */
public final class AccessLogExportRow extends SysAccessLog
{
    private static final long serialVersionUID = 1L;

    @Excel(name = "日志编号", sort = 0)
    private final Long recordId;

    @Excel(name = "访问时间", dateFormat = "yyyy-MM-dd HH:mm:ss", width = 24)
    private final Date accessTime;

    public AccessLogExportRow(SysAccessLog log)
    {
        recordId = log.getAccessId();
        accessTime = log.getCreateTime() == null ? null : new Date(log.getCreateTime().getTime());
        setUserId(log.getUserId());
        setUserName(log.getUserName());
        setIpaddr(log.getIpaddr());
        setRequestUri(log.getRequestUri());
        setRequestMethod(log.getRequestMethod());
        setStatus(log.getStatus());
        setErrorMsg(log.getErrorMsg());
        setCostTime(log.getCostTime());
    }
}
