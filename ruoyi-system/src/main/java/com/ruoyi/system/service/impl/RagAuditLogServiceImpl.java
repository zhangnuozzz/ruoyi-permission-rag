package com.ruoyi.system.service.impl;

import com.ruoyi.system.domain.rag.RagAuditLog;
import com.ruoyi.system.mapper.RagAuditLogMapper;
import com.ruoyi.system.service.IRagAuditLogService;
import com.ruoyi.system.service.ISysRagBehaviorAlertService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * RAG 检索审计日志 Service 实现
 */
@Service
public class RagAuditLogServiceImpl implements IRagAuditLogService
{
    @Autowired
    private RagAuditLogMapper ragAuditLogMapper;

    @Autowired
    private ISysRagBehaviorAlertService sysRagBehaviorAlertService;

    @Override
    public void record(RagAuditLog auditLog)
    {
        if (auditLog != null)
        {
            ragAuditLogMapper.insertRagAuditLog(auditLog);

            /*
             * 审计闭环：
             * 每次检索审计写入后，自动触发行为分析。
             * 重复告警由 sys_rag_behavior_alert 的唯一索引自动忽略。
             */
            try
            {
                sysRagBehaviorAlertService.analyzeRagAuditLogs();
            }
            catch (Exception e)
            {
                // 行为分析失败不能影响主检索流程。
            }
        }
    }
}
