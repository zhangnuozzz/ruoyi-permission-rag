package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.SysUserSecurityAttr;

/**
 * 用户安全属性Service接口
 */
public interface ISysUserSecurityAttrService
{
    public SysUserSecurityAttr selectSysUserSecurityAttrById(Long id);

    public SysUserSecurityAttr selectSysUserSecurityAttrByUserId(Long userId);

    public List<SysUserSecurityAttr> selectSysUserSecurityAttrList(SysUserSecurityAttr sysUserSecurityAttr);

    public int insertSysUserSecurityAttr(SysUserSecurityAttr sysUserSecurityAttr);

    public int updateSysUserSecurityAttr(SysUserSecurityAttr sysUserSecurityAttr);

    public int deleteSysUserSecurityAttrByIds(Long[] ids);

    public int deleteSysUserSecurityAttrById(Long id);
}
