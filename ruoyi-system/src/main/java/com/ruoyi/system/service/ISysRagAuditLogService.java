package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.SysRagAuditLog;

/**
 * RAG检索审计日志Service接口
 * 
 * @author zhangnuo
 * @date 2026-05-05
 */
public interface ISysRagAuditLogService 
{
    /**
     * 查询RAG检索审计日志
     * 
     * @param id RAG检索审计日志主键
     * @return RAG检索审计日志
     */
    public SysRagAuditLog selectSysRagAuditLogById(Long id);

    /**
     * 查询RAG检索审计日志列表
     * 
     * @param sysRagAuditLog RAG检索审计日志
     * @return RAG检索审计日志集合
     */
    public List<SysRagAuditLog> selectSysRagAuditLogList(SysRagAuditLog sysRagAuditLog);

    /**
     * 新增RAG检索审计日志
     * 
     * @param sysRagAuditLog RAG检索审计日志
     * @return 结果
     */
    public int insertSysRagAuditLog(SysRagAuditLog sysRagAuditLog);

    /**
     * 修改RAG检索审计日志
     * 
     * @param sysRagAuditLog RAG检索审计日志
     * @return 结果
     */
    public int updateSysRagAuditLog(SysRagAuditLog sysRagAuditLog);

    /**
     * 批量删除RAG检索审计日志
     * 
     * @param ids 需要删除的RAG检索审计日志主键集合
     * @return 结果
     */
    public int deleteSysRagAuditLogByIds(Long[] ids);

    /**
     * 删除RAG检索审计日志信息
     * 
     * @param id RAG检索审计日志主键
     * @return 结果
     */
    public int deleteSysRagAuditLogById(Long id);
}
