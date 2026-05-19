package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.SysIpBlacklist;

/**
 * IP黑名单 Mapper接口
 */
public interface SysIpBlacklistMapper
{
    public SysIpBlacklist selectSysIpBlacklistById(Long blacklistId);

    public SysIpBlacklist selectEnabledByIpaddr(String ipaddr);

    public List<SysIpBlacklist> selectSysIpBlacklistList(SysIpBlacklist sysIpBlacklist);

    public int insertSysIpBlacklist(SysIpBlacklist sysIpBlacklist);

    public int updateSysIpBlacklist(SysIpBlacklist sysIpBlacklist);

    public int deleteSysIpBlacklistById(Long blacklistId);

    public int deleteSysIpBlacklistByIds(Long[] blacklistIds);
}
