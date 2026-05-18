package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.SysPolicyBind;

/**
 * 策略绑定管理Service接口
 * 
 * @author zhangnuo
 * @date 2026-05-05
 */
public interface ISysPolicyBindService 
{
    /**
     * 查询策略绑定管理
     * 
     * @param id 策略绑定管理主键
     * @return 策略绑定管理
     */
    public SysPolicyBind selectSysPolicyBindById(Long id);

    /**
     * 查询策略绑定管理列表
     * 
     * @param sysPolicyBind 策略绑定管理
     * @return 策略绑定管理集合
     */
    public List<SysPolicyBind> selectSysPolicyBindList(SysPolicyBind sysPolicyBind);

    /**
     * 新增策略绑定管理
     * 
     * @param sysPolicyBind 策略绑定管理
     * @return 结果
     */
    public int insertSysPolicyBind(SysPolicyBind sysPolicyBind);

    /**
     * 修改策略绑定管理
     * 
     * @param sysPolicyBind 策略绑定管理
     * @return 结果
     */
    public int updateSysPolicyBind(SysPolicyBind sysPolicyBind);

    /**
     * 批量删除策略绑定管理
     * 
     * @param ids 需要删除的策略绑定管理主键集合
     * @return 结果
     */
    public int deleteSysPolicyBindByIds(Long[] ids);

    /**
     * 删除策略绑定管理信息
     * 
     * @param id 策略绑定管理主键
     * @return 结果
     */
    public int deleteSysPolicyBindById(Long id);
}
