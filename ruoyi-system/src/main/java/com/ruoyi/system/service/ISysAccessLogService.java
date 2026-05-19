package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.SysAccessLog;

/**
 * 系统访问监控日志 Service接口
 */
public interface ISysAccessLogService
{
    public SysAccessLog selectSysAccessLogById(Long accessId);

    public List<SysAccessLog> selectSysAccessLogList(SysAccessLog sysAccessLog);

    public int insertSysAccessLog(SysAccessLog sysAccessLog);

    public int deleteSysAccessLogByIds(Long[] accessIds);

    public int deleteSysAccessLogById(Long accessId);

    public void cleanSysAccessLog();
}
