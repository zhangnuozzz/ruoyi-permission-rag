package com.ruoyi.system.mapper;

import com.ruoyi.system.domain.SysRagFile;

/**
 * RAG文件元数据Mapper接口
 *
 * @author fufu
 * @date 2026-05-05
 */
public interface SysRagFileMapper
{
    /**
     * 新增RAG文件元数据
     *
     * @param sysRagFile RAG文件元数据
     * @return 结果
     */
    public int insertSysRagFile(SysRagFile sysRagFile);

    /**
     * 查询RAG文件元数据
     *
     * @param fileId 文件ID
     * @return RAG文件元数据
     */
    public SysRagFile selectSysRagFileByFileId(String fileId);
}
