package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.SysUserSecurityAttr;

/**
 * 用户安全属性Mapper接口
 */
public interface SysUserSecurityAttrMapper
{
    public SysUserSecurityAttr selectSysUserSecurityAttrById(Long id);

    public SysUserSecurityAttr selectSysUserSecurityAttrByUserId(Long userId);

    public List<SysUserSecurityAttr> selectSysUserSecurityAttrList(SysUserSecurityAttr sysUserSecurityAttr);

    public int insertSysUserSecurityAttr(SysUserSecurityAttr sysUserSecurityAttr);

    public int updateSysUserSecurityAttr(SysUserSecurityAttr sysUserSecurityAttr);

    public int deleteSysUserSecurityAttrById(Long id);

    public int deleteSysUserSecurityAttrByIds(Long[] ids);
}
