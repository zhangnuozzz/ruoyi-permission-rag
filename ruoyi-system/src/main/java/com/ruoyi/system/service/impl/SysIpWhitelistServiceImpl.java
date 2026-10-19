package com.ruoyi.system.service.impl;

import java.util.List;
import com.ruoyi.common.exception.CustomException;
import com.ruoyi.common.utils.ip.IpUtils;
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
        validate(sysIpWhitelist);
        if (sysIpWhitelist.getStatus() == null)
        {
            sysIpWhitelist.setStatus("0");
        }
        return sysIpWhitelistMapper.insertSysIpWhitelist(sysIpWhitelist);
    }

    @Override
    public int updateSysIpWhitelist(SysIpWhitelist sysIpWhitelist)
    {
        validate(sysIpWhitelist);
        if (sysIpWhitelist.getWhitelistId() == null)
        {
            throw new CustomException("记录编号不能为空");
        }
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
    private void validate(SysIpWhitelist entry)
    {
        if (entry == null)
        {
            throw new CustomException("IP 规则不能为空");
        }
        String ip = IpUtils.normalizeAddress(entry.getIpaddr());
        if (ip == null)
        {
            throw new CustomException("请输入有效的单个 IPv4 或 IPv6 地址，不支持主机名、端口或网段");
        }
        if (entry.getStatus() != null && !"0".equals(entry.getStatus()) && !"1".equals(entry.getStatus()))
        {
            throw new CustomException("状态只能为启用或停用");
        }
        entry.setIpaddr(ip);
    }
}
