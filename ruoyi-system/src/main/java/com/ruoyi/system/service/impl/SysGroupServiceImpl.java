package com.ruoyi.system.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.SysGroupMapper;
import com.ruoyi.system.domain.SysGroup;
import com.ruoyi.system.service.ISysGroupService;

/**
 * 用户组Service业务层处理
 * 
 * @author zhangnuo
 * @date 2026-04-20
 */
@Service
public class SysGroupServiceImpl implements ISysGroupService 
{
    @Autowired
    private SysGroupMapper sysGroupMapper;

    /**
     * 查询用户组
     * 
     * @param id 用户组主键
     * @return 用户组
     */
    @Override
    public SysGroup selectSysGroupById(Long id)
    {
        return sysGroupMapper.selectSysGroupById(id);
    }

    /**
     * 查询用户组列表
     * 
     * @param sysGroup 用户组
     * @return 用户组
     */
    @Override
    public List<SysGroup> selectSysGroupList(SysGroup sysGroup)
    {
        return sysGroupMapper.selectSysGroupList(sysGroup);
    }

    /**
     * 新增用户组
     * 
     * @param sysGroup 用户组
     * @return 结果
     */
    @Override
    public int insertSysGroup(SysGroup sysGroup)
    {
        sysGroup.setCreateTime(DateUtils.getNowDate());
        return sysGroupMapper.insertSysGroup(sysGroup);
    }

    /**
     * 修改用户组
     * 
     * @param sysGroup 用户组
     * @return 结果
     */
    @Override
    public int updateSysGroup(SysGroup sysGroup)
    {
        sysGroup.setUpdateTime(DateUtils.getNowDate());
        return sysGroupMapper.updateSysGroup(sysGroup);
    }

    /**
     * 批量删除用户组
     * 
     * @param ids 需要删除的用户组主键
     * @return 结果
     */
    @Override
    public int deleteSysGroupByIds(Long[] ids)
    {
        return sysGroupMapper.deleteSysGroupByIds(ids);
    }

    /**
     * 删除用户组信息
     * 
     * @param id 用户组主键
     * @return 结果
     */
    @Override
    public int deleteSysGroupById(Long id)
    {
        return sysGroupMapper.deleteSysGroupById(id);
    }
}
