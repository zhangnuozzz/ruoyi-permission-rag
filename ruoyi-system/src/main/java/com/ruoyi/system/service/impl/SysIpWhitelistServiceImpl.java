package com.ruoyi.system.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.domain.SysIpWhitelist;
import com.ruoyi.system.mapper.SysIpWhitelistMapper;
import com.ruoyi.system.service.ISysIpWhitelistService;

/**
 * IP白名单 Service业务层处理
 */
@Service
public class SysIpWhitelistServiceImpl implements ISysIpWhitelistService
{
    @Autowired
    private SysIpWhitelistMapper sysIpWhitelistMapper;

    @Override
    public SysIpWhitelist selectSysIpWhitelistById(Long whitelistId)
    {
        return sysIpWhitelistMapper.selectSysIpWhitelistById(whitelistId);
    }

    @Override
    public boolean isIpAllowed(String ipaddr)
    {
        if (ipaddr == null || ipaddr.length() == 0)
        {
            return false;
        }
        return sysIpWhitelistMapper.selectEnabledByIpaddr(ipaddr) != null;
    }

    @Override
    public List<SysIpWhitelist> selectSysIpWhitelistList(SysIpWhitelist sysIpWhitelist)
    {
        return sysIpWhitelistMapper.selectSysIpWhitelistList(sysIpWhitelist);
    }

    @Override
    public int insertSysIpWhitelist(SysIpWhitelist sysIpWhitelist)
    {
        return sysIpWhitelistMapper.insertSysIpWhitelist(sysIpWhitelist);
    }

    @Override
    public int updateSysIpWhitelist(SysIpWhitelist sysIpWhitelist)
    {
        return sysIpWhitelistMapper.updateSysIpWhitelist(sysIpWhitelist);
    }

    @Override
    public int deleteSysIpWhitelistByIds(Long[] whitelistIds)
    {
        return sysIpWhitelistMapper.deleteSysIpWhitelistByIds(whitelistIds);
    }

    @Override
    public int deleteSysIpWhitelistById(Long whitelistId)
    {
        return sysIpWhitelistMapper.deleteSysIpWhitelistById(whitelistId);
    }
}
