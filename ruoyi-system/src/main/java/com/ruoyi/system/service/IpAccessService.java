package com.ruoyi.system.service;

import com.ruoyi.common.utils.ip.IpUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** IP 规则的统一判定入口，白名单只豁免 IP 黑名单，不授予文档权限。 */
@Service
public class IpAccessService
{
    public enum Decision
    {
        ALLOW, WHITELIST, BLACKLIST, INVALID
    }

    @Autowired
    private ISysIpWhitelistService whitelistService;

    @Autowired
    private ISysIpBlacklistService blacklistService;

    public Decision evaluate(String address)
    {
        String ip = IpUtils.normalizeAddress(address);
        if (ip == null)
        {
            return Decision.INVALID;
        }
        if (whitelistService.isIpAllowed(ip))
        {
            return Decision.WHITELIST;
        }
        return blacklistService.isIpBlocked(ip) ? Decision.BLACKLIST : Decision.ALLOW;
    }
}
