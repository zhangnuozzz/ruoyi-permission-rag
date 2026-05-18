package com.ruoyi.system.mapper;

import com.ruoyi.system.domain.rag.RagAuditLog;

/**
 * RAG 检索审计日志 Mapper
 */
public interface RagAuditLogMapper
{
    /**
     * 插入审计日志
     *
     * @param auditLog 审计日志
     * @return 影响行数
     */
    int insertRagAuditLog(RagAuditLog auditLog);
}
