package com.ruoyi.system.service.impl;

import com.ruoyi.system.domain.rag.RagAuditLog;
import com.ruoyi.system.mapper.RagAuditLogMapper;
import com.ruoyi.system.service.IRagAuditLogService;
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

    @Override
    public void record(RagAuditLog auditLog)
    {
        if (auditLog != null)
        {
            ragAuditLogMapper.insertRagAuditLog(auditLog);
        }
    }
}
