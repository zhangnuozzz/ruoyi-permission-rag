package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.SysIpBlacklist;

/**
 * IP黑名单 Service接口
 */
public interface ISysIpBlacklistService
{
    public SysIpBlacklist selectSysIpBlacklistById(Long blacklistId);

    public boolean isIpBlocked(String ipaddr);

    public List<SysIpBlacklist> selectSysIpBlacklistList(SysIpBlacklist sysIpBlacklist);

    public int insertSysIpBlacklist(SysIpBlacklist sysIpBlacklist);

    public int updateSysIpBlacklist(SysIpBlacklist sysIpBlacklist);

    public int deleteSysIpBlacklistByIds(Long[] blacklistIds);

    public int deleteSysIpBlacklistById(Long blacklistId);
}
