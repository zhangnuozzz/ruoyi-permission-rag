package com.ruoyi.system.service.impl;

import java.util.List;
import com.ruoyi.common.exception.CustomException;
import com.ruoyi.common.utils.ip.IpUtils;
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
        validate(sysIpBlacklist);
        if (sysIpBlacklist.getStatus() == null)
        {
            sysIpBlacklist.setStatus("0");
        }
        return sysIpBlacklistMapper.insertSysIpBlacklist(sysIpBlacklist);
    }

    @Override
    public int updateSysIpBlacklist(SysIpBlacklist sysIpBlacklist)
    {
        validate(sysIpBlacklist);
        if (sysIpBlacklist.getBlacklistId() == null)
        {
            throw new CustomException("记录编号不能为空");
        }
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
    private void validate(SysIpBlacklist entry)
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
