package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.SysAccessLog;

/**
 * 系统访问监控日志 Mapper接口
 */
public interface SysAccessLogMapper
{
    public SysAccessLog selectSysAccessLogById(Long accessId);

    public List<SysAccessLog> selectSysAccessLogList(SysAccessLog sysAccessLog);

    public int insertSysAccessLog(SysAccessLog sysAccessLog);

    public int deleteSysAccessLogById(Long accessId);

    public int deleteSysAccessLogByIds(Long[] accessIds);

    public void cleanSysAccessLog();
}
