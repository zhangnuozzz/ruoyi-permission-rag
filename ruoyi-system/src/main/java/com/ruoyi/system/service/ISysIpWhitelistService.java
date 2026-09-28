package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.SysIpWhitelist;

/**
 * IP白名单 Service接口
 */
public interface ISysIpWhitelistService
{
    public SysIpWhitelist selectSysIpWhitelistById(Long whitelistId);

    public boolean isIpAllowed(String ipaddr);

    public List<SysIpWhitelist> selectSysIpWhitelistList(SysIpWhitelist sysIpWhitelist);

    public int insertSysIpWhitelist(SysIpWhitelist sysIpWhitelist);

    public int updateSysIpWhitelist(SysIpWhitelist sysIpWhitelist);

    public int deleteSysIpWhitelistByIds(Long[] whitelistIds);

    public int deleteSysIpWhitelistById(Long whitelistId);
}
