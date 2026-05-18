package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.SysPolicy;

/**
 * 权限策略定义Service接口
 * 
 * @author zhangnuo
 * @date 2026-05-05
 */
public interface ISysPolicyService 
{
    /**
     * 查询权限策略定义
     * 
     * @param id 权限策略定义主键
     * @return 权限策略定义
     */
    public SysPolicy selectSysPolicyById(Long id);

    /**
     * 查询权限策略定义列表
     * 
     * @param sysPolicy 权限策略定义
     * @return 权限策略定义集合
     */
    public List<SysPolicy> selectSysPolicyList(SysPolicy sysPolicy);

    /**
     * 新增权限策略定义
     * 
     * @param sysPolicy 权限策略定义
     * @return 结果
     */
    public int insertSysPolicy(SysPolicy sysPolicy);

    /**
     * 修改权限策略定义
     * 
     * @param sysPolicy 权限策略定义
     * @return 结果
     */
    public int updateSysPolicy(SysPolicy sysPolicy);

    /**
     * 批量删除权限策略定义
     * 
     * @param ids 需要删除的权限策略定义主键集合
     * @return 结果
     */
    public int deleteSysPolicyByIds(Long[] ids);

    /**
     * 删除权限策略定义信息
     * 
     * @param id 权限策略定义主键
     * @return 结果
     */
    public int deleteSysPolicyById(Long id);
}
