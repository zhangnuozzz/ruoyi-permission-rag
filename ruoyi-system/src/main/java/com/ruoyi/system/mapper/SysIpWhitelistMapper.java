package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.SysIpWhitelist;

/**
 * IP白名单 Mapper接口
 */
public interface SysIpWhitelistMapper
{
    public SysIpWhitelist selectSysIpWhitelistById(Long whitelistId);

    public SysIpWhitelist selectEnabledByIpaddr(String ipaddr);

    public List<SysIpWhitelist> selectSysIpWhitelistList(SysIpWhitelist sysIpWhitelist);

    public int insertSysIpWhitelist(SysIpWhitelist sysIpWhitelist);

    public int updateSysIpWhitelist(SysIpWhitelist sysIpWhitelist);

    public int deleteSysIpWhitelistById(Long whitelistId);

    public int deleteSysIpWhitelistByIds(Long[] whitelistIds);
}
