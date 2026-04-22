package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.SysPolicy;

/**
 * 策略管理Mapper接口
 * 
 * @author zhangnuo
 * @date 2026-04-20
 */
public interface SysPolicyMapper 
{
    /**
     * 查询策略管理
     * 
     * @param id 策略管理主键
     * @return 策略管理
     */
    public SysPolicy selectSysPolicyById(Long id);

    /**
     * 查询策略管理列表
     * 
     * @param sysPolicy 策略管理
     * @return 策略管理集合
     */
    public List<SysPolicy> selectSysPolicyList(SysPolicy sysPolicy);

    /**
     * 新增策略管理
     * 
     * @param sysPolicy 策略管理
     * @return 结果
     */
    public int insertSysPolicy(SysPolicy sysPolicy);

    /**
     * 修改策略管理
     * 
     * @param sysPolicy 策略管理
     * @return 结果
     */
    public int updateSysPolicy(SysPolicy sysPolicy);

    /**
     * 删除策略管理
     * 
     * @param id 策略管理主键
     * @return 结果
     */
    public int deleteSysPolicyById(Long id);

    /**
     * 批量删除策略管理
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteSysPolicyByIds(Long[] ids);
}
