package com.ruoyi.system.service;

import com.ruoyi.system.domain.rag.RagAuditLog;

/**
 * RAG 检索审计日志 Service
 */
public interface IRagAuditLogService
{
    /**
     * 记录审计日志
     *
     * @param auditLog 审计日志
     */
    void record(RagAuditLog auditLog);
}
