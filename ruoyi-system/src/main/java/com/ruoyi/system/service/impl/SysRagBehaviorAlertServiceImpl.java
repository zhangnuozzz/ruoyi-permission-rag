package com.ruoyi.system.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.domain.SysRagAuditLog;
import com.ruoyi.system.domain.SysRagBehaviorAlert;
import com.ruoyi.system.mapper.SysRagBehaviorAlertMapper;
import com.ruoyi.system.service.ISysRagAuditLogService;
import com.ruoyi.system.service.ISysRagBehaviorAlertService;

/**
 * RAG行为分析告警Service业务层处理
 */
@Service
public class SysRagBehaviorAlertServiceImpl implements ISysRagBehaviorAlertService
{
    @Autowired
    private SysRagBehaviorAlertMapper sysRagBehaviorAlertMapper;

    @Autowired
    private ISysRagAuditLogService sysRagAuditLogService;

    @Override
    public SysRagBehaviorAlert selectSysRagBehaviorAlertById(Long id)
    {
        return sysRagBehaviorAlertMapper.selectSysRagBehaviorAlertById(id);
    }

    @Override
    public List<SysRagBehaviorAlert> selectSysRagBehaviorAlertList(SysRagBehaviorAlert sysRagBehaviorAlert)
    {
        return sysRagBehaviorAlertMapper.selectSysRagBehaviorAlertList(sysRagBehaviorAlert);
    }

    @Override
    public int insertSysRagBehaviorAlert(SysRagBehaviorAlert sysRagBehaviorAlert)
    {
        return sysRagBehaviorAlertMapper.insertSysRagBehaviorAlert(sysRagBehaviorAlert);
    }

    @Override
    public int updateSysRagBehaviorAlert(SysRagBehaviorAlert sysRagBehaviorAlert)
    {
        return sysRagBehaviorAlertMapper.updateSysRagBehaviorAlert(sysRagBehaviorAlert);
    }

    @Override
    public int deleteSysRagBehaviorAlertByIds(Long[] ids)
    {
        return sysRagBehaviorAlertMapper.deleteSysRagBehaviorAlertByIds(ids);
    }

    @Override
    public int deleteSysRagBehaviorAlertById(Long id)
    {
        return sysRagBehaviorAlertMapper.deleteSysRagBehaviorAlertById(id);
    }

    @Override
    public int analyzeRagAuditLogs()
    {
        int count = 0;
        List<SysRagAuditLog> logs = sysRagAuditLogService.selectSysRagAuditLogList(new SysRagAuditLog());

        for (SysRagAuditLog log : logs)
        {
            if (log == null || log.getId() == null)
            {
                continue;
            }

            String allowAccess = log.getAllowAccess();
            if ("0".equals(allowAccess))
            {
                count += insertAlertIgnoreDuplicate(log, "DENY_ACCESS", "high", "访问请求被权限策略拒绝");
            }

            String blocked = log.getBlockedResultsJson();
            if (blocked != null && blocked.trim().length() > 2 && !"[]".equals(blocked.trim()))
            {
                count += insertAlertIgnoreDuplicate(log, "SECOND_FILTER_BLOCK", "medium", "数据过滤模块二次校验时拦截了不符合权限的数据");
            }

            Long costTime = log.getCostTime();
            if (costTime != null && costTime.longValue() >= 5000L)
            {
                count += insertAlertIgnoreDuplicate(log, "SLOW_QUERY", "medium", "RAG检索耗时超过5000毫秒，可能存在异常慢查询");
            }
        }

        return count;
    }

    private int insertAlertIgnoreDuplicate(SysRagAuditLog log, String type, String level, String reason)
    {
        try
        {
            SysRagBehaviorAlert alert = new SysRagBehaviorAlert();
            alert.setSourceLogId(log.getId());
            alert.setUserId(log.getUserId());
            alert.setUserName(log.getUserName());
            alert.setAlertType(type);
            alert.setAlertLevel(level);
            alert.setAlertReason(reason);
            alert.setQueryText(log.getQueryText());
            alert.setAllowAccess(log.getAllowAccess());
            alert.setCostTime(log.getCostTime());
            alert.setStatus("unhandled");
            return sysRagBehaviorAlertMapper.insertSysRagBehaviorAlert(alert);
        }
        catch (Exception e)
        {
            return 0;
        }
    }
}
