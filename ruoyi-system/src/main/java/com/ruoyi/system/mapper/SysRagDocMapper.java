package com.ruoyi.system.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.SysRagDoc;

/**
 * 文档权限标签Mapper接口
 *
 * @author zhangnuo
 * @date 2026-05-06
 */
public interface SysRagDocMapper
{
    /**
     * 查询文档权限标签
     *
     * @param id 文档权限标签主键
     * @return 文档权限标签
     */
    public SysRagDoc selectSysRagDocById(Long id);

    /** 锁定并读取当前已提交标签，避免使用普通查询的会话缓存。 */
    public SysRagDoc selectSysRagDocByIdForUpdate(Long id);

    /**
     * 根据文档ID查询文档权限标签
     *
     * @param docId 文档ID
     * @return 文档权限标签
     */
    public SysRagDoc selectSysRagDocByDocId(String docId);

    List<SysRagDoc> selectSysRagDocsByDocIds(@Param("docIds") List<String> docIds);

    /**
     * 查询文档权限标签列表
     *
     * @param sysRagDoc 文档权限标签
     * @return 文档权限标签集合
     */
    public List<SysRagDoc> selectSysRagDocList(SysRagDoc sysRagDoc);

    /**
     * 新增文档权限标签
     *
     * @param sysRagDoc 文档权限标签
     * @return 结果
     */
    public int insertSysRagDoc(SysRagDoc sysRagDoc);

    /**
     * 修改文档权限标签
     *
     * @param sysRagDoc 文档权限标签
     * @return 结果
     */
    public int updateSysRagDoc(SysRagDoc sysRagDoc);

    /**
     * 删除文档权限标签
     *
     * @param id 文档权限标签主键
     * @return 结果
     */
    public int deleteSysRagDocById(Long id);

    /**
     * 批量删除文档权限标签
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteSysRagDocByIds(Long[] ids);
}
