package com.ruoyi.system.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.SysAccessLogMapper;
import com.ruoyi.system.domain.SysAccessLog;
import com.ruoyi.system.service.ISysAccessLogService;

/**
 * 系统访问监控日志 Service业务层处理
 */
@Service
public class SysAccessLogServiceImpl implements ISysAccessLogService
{
    @Autowired
    private SysAccessLogMapper sysAccessLogMapper;

    @Override
    public SysAccessLog selectSysAccessLogById(Long accessId)
    {
        return sysAccessLogMapper.selectSysAccessLogById(accessId);
    }

    @Override
    public List<SysAccessLog> selectSysAccessLogList(SysAccessLog sysAccessLog)
    {
        return sysAccessLogMapper.selectSysAccessLogList(sysAccessLog);
    }

    @Override
    public int insertSysAccessLog(SysAccessLog sysAccessLog)
    {
        return sysAccessLogMapper.insertSysAccessLog(sysAccessLog);
    }

    @Override
    public int deleteSysAccessLogByIds(Long[] accessIds)
    {
        return sysAccessLogMapper.deleteSysAccessLogByIds(accessIds);
    }

    @Override
    public int deleteSysAccessLogById(Long accessId)
    {
        return sysAccessLogMapper.deleteSysAccessLogById(accessId);
    }

    @Override
    public void cleanSysAccessLog()
    {
        sysAccessLogMapper.cleanSysAccessLog();
    }
}
