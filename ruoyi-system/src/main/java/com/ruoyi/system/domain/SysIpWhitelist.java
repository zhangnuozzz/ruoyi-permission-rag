package com.ruoyi.system.domain;

import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * IP白名单对象 sys_ip_whitelist
 */
public class SysIpWhitelist extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long whitelistId;

    @Excel(name = "IP地址")
    private String ipaddr;

    @Excel(name = "放行原因")
    private String reason;

    @Excel(name = "状态", readConverterExp = "0=启用,1=停用")
    private String status;

    public Long getWhitelistId()
    {
        return whitelistId;
    }

    public void setWhitelistId(Long whitelistId)
    {
        this.whitelistId = whitelistId;
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
