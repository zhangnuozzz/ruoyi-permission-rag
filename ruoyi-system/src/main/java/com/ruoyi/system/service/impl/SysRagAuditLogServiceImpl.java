package com.ruoyi.system.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.SysRagAuditLogMapper;
import com.ruoyi.system.domain.SysRagAuditLog;
import com.ruoyi.system.service.ISysRagAuditLogService;

/**
 * RAG检索审计日志Service业务层处理
 * 
 * @author zhangnuo
 * @date 2026-05-05
 */
@Service
public class SysRagAuditLogServiceImpl implements ISysRagAuditLogService 
{
    @Autowired
    private SysRagAuditLogMapper sysRagAuditLogMapper;

    /**
     * 查询RAG检索审计日志
     * 
     * @param id RAG检索审计日志主键
     * @return RAG检索审计日志
     */
    @Override
    public SysRagAuditLog selectSysRagAuditLogById(Long id)
    {
        return sysRagAuditLogMapper.selectSysRagAuditLogById(id);
    }

    /**
     * 查询RAG检索审计日志列表
     * 
     * @param sysRagAuditLog RAG检索审计日志
     * @return RAG检索审计日志
     */
    @Override
    public List<SysRagAuditLog> selectSysRagAuditLogList(SysRagAuditLog sysRagAuditLog)
    {
        return sysRagAuditLogMapper.selectSysRagAuditLogList(sysRagAuditLog);
    }

    /**
     * 新增RAG检索审计日志
     * 
     * @param sysRagAuditLog RAG检索审计日志
     * @return 结果
     */
    @Override
    public int insertSysRagAuditLog(SysRagAuditLog sysRagAuditLog)
    {
        sysRagAuditLog.setCreateTime(DateUtils.getNowDate());
        return sysRagAuditLogMapper.insertSysRagAuditLog(sysRagAuditLog);
    }

    /**
     * 修改RAG检索审计日志
     * 
     * @param sysRagAuditLog RAG检索审计日志
     * @return 结果
     */
    @Override
    public int updateSysRagAuditLog(SysRagAuditLog sysRagAuditLog)
    {
        return sysRagAuditLogMapper.updateSysRagAuditLog(sysRagAuditLog);
    }

    /**
     * 批量删除RAG检索审计日志
     * 
     * @param ids 需要删除的RAG检索审计日志主键
     * @return 结果
     */
    @Override
    public int deleteSysRagAuditLogByIds(Long[] ids)
    {
        return sysRagAuditLogMapper.deleteSysRagAuditLogByIds(ids);
    }

    /**
     * 删除RAG检索审计日志信息
     * 
     * @param id RAG检索审计日志主键
     * @return 结果
     */
    @Override
    public int deleteSysRagAuditLogById(Long id)
    {
        return sysRagAuditLogMapper.deleteSysRagAuditLogById(id);
    }
}
