package com.ruoyi.system.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.SysPolicyMapper;
import com.ruoyi.system.domain.SysPolicy;
import com.ruoyi.system.service.ISysPolicyService;

/**
 * 权限策略定义Service业务层处理
 * 
 * @author zhangnuo
 * @date 2026-05-05
 */
@Service
public class SysPolicyServiceImpl implements ISysPolicyService 
{
    @Autowired
    private SysPolicyMapper sysPolicyMapper;

    /**
     * 查询权限策略定义
     * 
     * @param id 权限策略定义主键
     * @return 权限策略定义
     */
    @Override
    public SysPolicy selectSysPolicyById(Long id)
    {
        return sysPolicyMapper.selectSysPolicyById(id);
    }

    /**
     * 查询权限策略定义列表
     * 
     * @param sysPolicy 权限策略定义
     * @return 权限策略定义
     */
    @Override
    public List<SysPolicy> selectSysPolicyList(SysPolicy sysPolicy)
    {
        return sysPolicyMapper.selectSysPolicyList(sysPolicy);
    }

    /**
     * 新增权限策略定义
     * 
     * @param sysPolicy 权限策略定义
     * @return 结果
     */
    @Override
    public int insertSysPolicy(SysPolicy sysPolicy)
    {
        sysPolicy.setCreateTime(DateUtils.getNowDate());
        return sysPolicyMapper.insertSysPolicy(sysPolicy);
    }

    /**
     * 修改权限策略定义
     * 
     * @param sysPolicy 权限策略定义
     * @return 结果
     */
    @Override
    public int updateSysPolicy(SysPolicy sysPolicy)
    {
        sysPolicy.setUpdateTime(DateUtils.getNowDate());
        return sysPolicyMapper.updateSysPolicy(sysPolicy);
    }

    /**
     * 批量删除权限策略定义
     * 
     * @param ids 需要删除的权限策略定义主键
     * @return 结果
     */
    @Override
    public int deleteSysPolicyByIds(Long[] ids)
    {
        return sysPolicyMapper.deleteSysPolicyByIds(ids);
    }

    /**
     * 删除权限策略定义信息
     * 
     * @param id 权限策略定义主键
     * @return 结果
     */
    @Override
    public int deleteSysPolicyById(Long id)
    {
        return sysPolicyMapper.deleteSysPolicyById(id);
    }
}
