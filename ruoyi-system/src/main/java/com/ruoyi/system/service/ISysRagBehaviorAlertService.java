package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.SysRagBehaviorAlert;

public interface ISysRagBehaviorAlertService
{
    public SysRagBehaviorAlert selectSysRagBehaviorAlertById(Long id);

    public List<SysRagBehaviorAlert> selectSysRagBehaviorAlertList(SysRagBehaviorAlert sysRagBehaviorAlert);

    public int insertSysRagBehaviorAlert(SysRagBehaviorAlert sysRagBehaviorAlert);

    public int updateSysRagBehaviorAlert(SysRagBehaviorAlert sysRagBehaviorAlert);

    public int deleteSysRagBehaviorAlertByIds(Long[] ids);

    public int deleteSysRagBehaviorAlertById(Long id);

    public int analyzeRagAuditLogs();
}
