package com.ruoyi.system.service.impl;

import com.ruoyi.system.domain.rag.RagAuditLog;
import com.ruoyi.system.mapper.RagAuditLogMapper;
import com.ruoyi.system.mapper.BbacSecurityMapper;
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
    private BbacSecurityMapper bbacSecurityMapper;

    @Autowired
    private ISysRagBehaviorAlertService sysRagBehaviorAlertService;

    @Override
    public void record(RagAuditLog auditLog)
    {
        if (auditLog != null)
        {
            ragAuditLogMapper.insertRagAuditLog(auditLog);

            /*
             * BBAC 连续失败闭环：
             * allow_access = 1 表示放行，成功后连续失败次数清零；
             * allow_access = 0 表示拒绝，连续失败次数加 1；
             * 第 5 次连续失败时 Mapper 会立即锁定用户 30 分钟。
             */
            try
            {
                if (auditLog.getUserId() != null)
                {
                    if ("1".equals(auditLog.getAllowAccess()))
                    {
                        bbacSecurityMapper.resetFailCount(auditLog.getUserId());
                    }
                    else if ("0".equals(auditLog.getAllowAccess()))
                    {
                        bbacSecurityMapper.increaseFailCount(auditLog.getUserId());
                    }
                }
            }
            catch (Exception e)
            {
                // BBAC 状态维护失败不能影响主检索流程。
            }

            /*
             * 审计闭环：
             * 每次检索审计写入后，自动触发行为分析。
             * 重复告警由 sys_rag_behavior_alert 的唯一索引自动忽略。
             */
            try
            {
                sysRagBehaviorAlertService.analyzeRagAuditLogById(auditLog.getId());
            }
            catch (Exception e)
            {
                // 行为分析失败不能影响主检索流程。
            }
        }
    }
}
