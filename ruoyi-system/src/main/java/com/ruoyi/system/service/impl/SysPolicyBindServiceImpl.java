package com.ruoyi.system.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.SysPolicyBindMapper;
import com.ruoyi.system.domain.SysPolicyBind;
import com.ruoyi.system.service.ISysPolicyBindService;

/**
 * 策略绑定管理Service业务层处理
 * 
 * @author zhangnuo
 * @date 2026-04-20
 */
@Service
public class SysPolicyBindServiceImpl implements ISysPolicyBindService 
{
    @Autowired
    private SysPolicyBindMapper sysPolicyBindMapper;

    /**
     * 查询策略绑定管理
     * 
     * @param id 策略绑定管理主键
     * @return 策略绑定管理
     */
    @Override
    public SysPolicyBind selectSysPolicyBindById(Long id)
    {
        return sysPolicyBindMapper.selectSysPolicyBindById(id);
    }

    /**
     * 查询策略绑定管理列表
     * 
     * @param sysPolicyBind 策略绑定管理
     * @return 策略绑定管理
     */
    @Override
    public List<SysPolicyBind> selectSysPolicyBindList(SysPolicyBind sysPolicyBind)
    {
        return sysPolicyBindMapper.selectSysPolicyBindList(sysPolicyBind);
    }

    /**
     * 新增策略绑定管理
     * 
     * @param sysPolicyBind 策略绑定管理
     * @return 结果
     */
    @Override
    public int insertSysPolicyBind(SysPolicyBind sysPolicyBind)
    {
        sysPolicyBind.setCreateTime(DateUtils.getNowDate());
        return sysPolicyBindMapper.insertSysPolicyBind(sysPolicyBind);
    }

    /**
     * 修改策略绑定管理
     * 
     * @param sysPolicyBind 策略绑定管理
     * @return 结果
     */
    @Override
    public int updateSysPolicyBind(SysPolicyBind sysPolicyBind)
    {
        return sysPolicyBindMapper.updateSysPolicyBind(sysPolicyBind);
    }

    /**
     * 批量删除策略绑定管理
     * 
     * @param ids 需要删除的策略绑定管理主键
     * @return 结果
     */
    @Override
    public int deleteSysPolicyBindByIds(Long[] ids)
    {
        return sysPolicyBindMapper.deleteSysPolicyBindByIds(ids);
    }

    /**
     * 删除策略绑定管理信息
     * 
     * @param id 策略绑定管理主键
     * @return 结果
     */
    @Override
    public int deleteSysPolicyBindById(Long id)
    {
        return sysPolicyBindMapper.deleteSysPolicyBindById(id);
    }
}
