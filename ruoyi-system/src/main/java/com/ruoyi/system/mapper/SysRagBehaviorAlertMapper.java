package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.SysRagBehaviorAlert;

public interface SysRagBehaviorAlertMapper
{
    public SysRagBehaviorAlert selectSysRagBehaviorAlertById(Long id);

    public List<SysRagBehaviorAlert> selectSysRagBehaviorAlertList(SysRagBehaviorAlert sysRagBehaviorAlert);

    public int insertSysRagBehaviorAlert(SysRagBehaviorAlert sysRagBehaviorAlert);

    public int updateSysRagBehaviorAlert(SysRagBehaviorAlert sysRagBehaviorAlert);

    public int deleteSysRagBehaviorAlertById(Long id);

    public int deleteSysRagBehaviorAlertByIds(Long[] ids);
}
