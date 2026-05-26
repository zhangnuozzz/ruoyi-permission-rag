package com.fufu.ragserver.mapper;

import com.fufu.ragserver.domain.SysRagFile;
import java.util.List;

/**
 * RAG文件元数据Mapper接口
 *
 * @author fufu
 * @date 2026-05-12
 */
public interface SysRagFileMapper
{
    int insertSysRagFile(SysRagFile sysRagFile);

    SysRagFile selectSysRagFileByFileId(String fileId);

    List<SysRagFile> selectSysRagFileList();
}
