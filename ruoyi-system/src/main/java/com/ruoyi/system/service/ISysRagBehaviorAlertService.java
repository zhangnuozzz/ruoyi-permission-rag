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

    /** 全量分析审计日志并生成行为告警 */
    public int analyzeRagAuditLogs();

    /**
     * 只分析指定的一条 RAG 审计日志。
     * 用于每次检索结束后实时生成行为告警，避免每次都全量扫描历史日志。
     */
    public int analyzeRagAuditLogById(Long auditLogId);

    /** 处理告警：CONFIRM / IGNORE / BLOCK_USER / LIMIT_USER */
    public int handleAlert(Long id, String action, String handledBy, String handleRemark);

    /** 兼容旧接口 */
    public int handleAlert(Long id, String handledBy, String handleRemark);

    /** 导出 syslog 风格文本 */
    public String exportSyslog(SysRagBehaviorAlert query);
}
