package com.ruoyi.system.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.SysPolicyMapper;
import com.ruoyi.system.domain.SysPolicy;
import com.ruoyi.system.service.ISysPolicyService;

/**
 * 策略管理Service业务层处理
 * 
 * @author zhangnuo
 * @date 2026-04-20
 */
@Service
public class SysPolicyServiceImpl implements ISysPolicyService 
{
    @Autowired
    private SysPolicyMapper sysPolicyMapper;

    /**
     * 查询策略管理
     * 
     * @param id 策略管理主键
     * @return 策略管理
     */
    @Override
    public SysPolicy selectSysPolicyById(Long id)
    {
        return sysPolicyMapper.selectSysPolicyById(id);
    }

    /**
     * 查询策略管理列表
     * 
     * @param sysPolicy 策略管理
     * @return 策略管理
     */
    @Override
    public List<SysPolicy> selectSysPolicyList(SysPolicy sysPolicy)
    {
        return sysPolicyMapper.selectSysPolicyList(sysPolicy);
    }

    /**
     * 新增策略管理
     * 
     * @param sysPolicy 策略管理
     * @return 结果
     */
    @Override
    public int insertSysPolicy(SysPolicy sysPolicy)
    {
        sysPolicy.setCreateTime(DateUtils.getNowDate());
        return sysPolicyMapper.insertSysPolicy(sysPolicy);
    }

    /**
     * 修改策略管理
     * 
     * @param sysPolicy 策略管理
     * @return 结果
     */
    @Override
    public int updateSysPolicy(SysPolicy sysPolicy)
    {
        sysPolicy.setUpdateTime(DateUtils.getNowDate());
        return sysPolicyMapper.updateSysPolicy(sysPolicy);
    }

    /**
     * 批量删除策略管理
     * 
     * @param ids 需要删除的策略管理主键
     * @return 结果
     */
    @Override
    public int deleteSysPolicyByIds(Long[] ids)
    {
        return sysPolicyMapper.deleteSysPolicyByIds(ids);
    }

    /**
     * 删除策略管理信息
     * 
     * @param id 策略管理主键
     * @return 结果
     */
    @Override
    public int deleteSysPolicyById(Long id)
    {
        return sysPolicyMapper.deleteSysPolicyById(id);
    }
}
