package com.ruoyi.system.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.domain.SysIpBlacklist;
import com.ruoyi.system.mapper.SysIpBlacklistMapper;
import com.ruoyi.system.service.ISysIpBlacklistService;

/**
 * IP黑名单 Service业务层处理
 */
@Service
public class SysIpBlacklistServiceImpl implements ISysIpBlacklistService
{
    @Autowired
    private SysIpBlacklistMapper sysIpBlacklistMapper;

    @Override
    public SysIpBlacklist selectSysIpBlacklistById(Long blacklistId)
    {
        return sysIpBlacklistMapper.selectSysIpBlacklistById(blacklistId);
    }

    @Override
    public boolean isIpBlocked(String ipaddr)
    {
        if (ipaddr == null || ipaddr.length() == 0)
        {
            return false;
        }
        return sysIpBlacklistMapper.selectEnabledByIpaddr(ipaddr) != null;
    }

    @Override
    public List<SysIpBlacklist> selectSysIpBlacklistList(SysIpBlacklist sysIpBlacklist)
    {
        return sysIpBlacklistMapper.selectSysIpBlacklistList(sysIpBlacklist);
    }

    @Override
    public int insertSysIpBlacklist(SysIpBlacklist sysIpBlacklist)
    {
        return sysIpBlacklistMapper.insertSysIpBlacklist(sysIpBlacklist);
    }

    @Override
    public int updateSysIpBlacklist(SysIpBlacklist sysIpBlacklist)
    {
        return sysIpBlacklistMapper.updateSysIpBlacklist(sysIpBlacklist);
    }

    @Override
    public int deleteSysIpBlacklistByIds(Long[] blacklistIds)
    {
        return sysIpBlacklistMapper.deleteSysIpBlacklistByIds(blacklistIds);
    }

    @Override
    public int deleteSysIpBlacklistById(Long blacklistId)
    {
        return sysIpBlacklistMapper.deleteSysIpBlacklistById(blacklistId);
    }
}
