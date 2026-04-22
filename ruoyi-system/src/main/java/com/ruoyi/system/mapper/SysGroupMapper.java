package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.SysGroup;

/**
 * 用户组Mapper接口
 * 
 * @author zhangnuo
 * @date 2026-04-20
 */
public interface SysGroupMapper 
{
    /**
     * 查询用户组
     * 
     * @param id 用户组主键
     * @return 用户组
     */
    public SysGroup selectSysGroupById(Long id);

    /**
     * 查询用户组列表
     * 
     * @param sysGroup 用户组
     * @return 用户组集合
     */
    public List<SysGroup> selectSysGroupList(SysGroup sysGroup);

    /**
     * 新增用户组
     * 
     * @param sysGroup 用户组
     * @return 结果
     */
    public int insertSysGroup(SysGroup sysGroup);

    /**
     * 修改用户组
     * 
     * @param sysGroup 用户组
     * @return 结果
     */
    public int updateSysGroup(SysGroup sysGroup);

    /**
     * 删除用户组
     * 
     * @param id 用户组主键
     * @return 结果
     */
    public int deleteSysGroupById(Long id);

    /**
     * 批量删除用户组
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteSysGroupByIds(Long[] ids);
}
