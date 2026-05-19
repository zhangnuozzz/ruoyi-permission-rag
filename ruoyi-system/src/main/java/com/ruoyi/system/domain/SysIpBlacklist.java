package com.ruoyi.system.domain;

import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * IP黑名单对象 sys_ip_blacklist
 */
public class SysIpBlacklist extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long blacklistId;

    @Excel(name = "IP地址")
    private String ipaddr;

    @Excel(name = "封禁原因")
    private String reason;

    @Excel(name = "状态", readConverterExp = "0=启用,1=停用")
    private String status;

    public Long getBlacklistId()
    {
        return blacklistId;
    }

    public void setBlacklistId(Long blacklistId)
    {
        this.blacklistId = blacklistId;
    }

    public String getIpaddr()
    {
        return ipaddr;
    }

    public void setIpaddr(String ipaddr)
    {
        this.ipaddr = ipaddr;
    }

    public String getReason()
    {
        return reason;
    }

    public void setReason(String reason)
    {
        this.reason = reason;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }
}
