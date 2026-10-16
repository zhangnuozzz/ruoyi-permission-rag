package com.ruoyi.system.service.impl;

import java.util.List;
import java.util.Map;
import com.ruoyi.common.exception.CustomException;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
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

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
    @Transactional(rollbackFor = Exception.class)
    public int insertSysRagDoc(SysRagDoc sysRagDoc)
    {
        if (sysRagDoc == null)
        {
            throw new CustomException("文档标签不能为空");
        }
        lockDocumentGroup(sysRagDoc.getSecurityLevel(), sysRagDoc.getOwnerGroupCode());
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
    @Transactional(rollbackFor = Exception.class)
    public int updateSysRagDoc(SysRagDoc sysRagDoc)
    {
        if (sysRagDoc == null || sysRagDoc.getId() == null || sysRagDoc.getId() <= 0)
        {
            throw new CustomException("文档标签 ID 必须为正整数");
        }
        List<Map<String, Object>> current = jdbcTemplate.queryForList(
                "select security_level, owner_group_code from sys_rag_doc "
                + "where id = ? and del_flag = '0' for update", sysRagDoc.getId());
        if (current.size() != 1)
        {
            throw new CustomException("文档标签不存在或已删除");
        }
        Map<String, Object> document = current.get(0);
        String level = sysRagDoc.getSecurityLevel();
        if (level == null || level.isEmpty())
        {
            level = (String) document.get("security_level");
        }
        String groupCode = sysRagDoc.getOwnerGroupCode() == null
                ? (String) document.get("owner_group_code") : sysRagDoc.getOwnerGroupCode();
        lockDocumentGroup(level, groupCode);
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

    /**
     * 涉密标签写入持有组行锁，避免与用户组删除交错产生失效归属。
     */
    private void lockDocumentGroup(String level, String groupCode)
    {
        String value = level == null ? "" : level.trim();
        if (!"SECRET".equalsIgnoreCase(value) && !"CONFIDENTIAL".equalsIgnoreCase(value))
        {
            return;
        }
        if (groupCode == null || groupCode.trim().isEmpty())
        {
            throw new CustomException("秘密、机密文档必须绑定一个知悉用户组");
        }
        if ("GROUP_PUBLIC".equalsIgnoreCase(groupCode.trim()))
        {
            throw new CustomException("公开兼容分组不用于秘密、机密文档，请选择知悉用户组");
        }
        List<String> groups = jdbcTemplate.queryForList(
                "select status from sys_group where group_code = ? and del_flag = '0' for update",
                String.class, groupCode);
        if (groups.size() != 1 || !"0".equals(groups.get(0)))
        {
            throw new CustomException("文档绑定用户组不存在或已停用，请重新选择");
        }
    }
}
