package com.ruoyi.system.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.SysRagDocMapper;
import com.ruoyi.system.domain.SysRagDoc;
import com.ruoyi.system.service.ISysRagDocService;

/**
 * 文档权限标签Service业务层处理
 * 
 * @author zhangnuo
 * @date 2026-05-06
 */
@Service
public class SysRagDocServiceImpl implements ISysRagDocService 
{
    @Autowired
    private SysRagDocMapper sysRagDocMapper;

    /**
     * 查询文档权限标签
     * 
     * @param id 文档权限标签主键
     * @return 文档权限标签
     */
    @Override
    public SysRagDoc selectSysRagDocById(Long id)
    {
        return sysRagDocMapper.selectSysRagDocById(id);
    }

    /**
     * 查询文档权限标签列表
     * 
     * @param sysRagDoc 文档权限标签
     * @return 文档权限标签
     */
    @Override
    public List<SysRagDoc> selectSysRagDocList(SysRagDoc sysRagDoc)
    {
        return sysRagDocMapper.selectSysRagDocList(sysRagDoc);
    }

    /**
     * 新增文档权限标签
     * 
     * @param sysRagDoc 文档权限标签
     * @return 结果
     */
    @Override
    public int insertSysRagDoc(SysRagDoc sysRagDoc)
    {
        sysRagDoc.setCreateTime(DateUtils.getNowDate());
        return sysRagDocMapper.insertSysRagDoc(sysRagDoc);
    }

    /**
     * 修改文档权限标签
     * 
     * @param sysRagDoc 文档权限标签
     * @return 结果
     */
    @Override
    public int updateSysRagDoc(SysRagDoc sysRagDoc)
    {
        sysRagDoc.setUpdateTime(DateUtils.getNowDate());
        return sysRagDocMapper.updateSysRagDoc(sysRagDoc);
    }

    /**
     * 批量删除文档权限标签
     * 
     * @param ids 需要删除的文档权限标签主键
     * @return 结果
     */
    @Override
    public int deleteSysRagDocByIds(Long[] ids)
    {
        return sysRagDocMapper.deleteSysRagDocByIds(ids);
    }

    /**
     * 删除文档权限标签信息
     * 
     * @param id 文档权限标签主键
     * @return 结果
     */
    @Override
    public int deleteSysRagDocById(Long id)
    {
        return sysRagDocMapper.deleteSysRagDocById(id);
    }
}
