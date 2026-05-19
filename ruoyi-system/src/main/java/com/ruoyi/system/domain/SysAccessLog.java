package com.ruoyi.system.domain;

import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 系统访问监控日志对象 sys_access_log
 */
public class SysAccessLog extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long accessId;

    @Excel(name = "用户ID")
    private Long userId;

    @Excel(name = "用户名")
    private String userName;

    @Excel(name = "访问IP")
    private String ipaddr;

    @Excel(name = "请求地址")
    private String requestUri;

    @Excel(name = "请求方式")
    private String requestMethod;

    @Excel(name = "访问状态", readConverterExp = "0=成功,1=失败")
    private String status;

    @Excel(name = "错误信息")
    private String errorMsg;

    @Excel(name = "耗时毫秒")
    private Long costTime;

    public Long getAccessId()
    {
        return accessId;
    }

    public void setAccessId(Long accessId)
    {
        this.accessId = accessId;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public String getUserName()
    {
        return userName;
    }

    public void setUserName(String userName)
    {
        this.userName = userName;
    }

    public String getIpaddr()
    {
        return ipaddr;
    }

    public void setIpaddr(String ipaddr)
    {
        this.ipaddr = ipaddr;
    }

    public String getRequestUri()
    {
        return requestUri;
    }

    public void setRequestUri(String requestUri)
    {
        this.requestUri = requestUri;
    }

    public String getRequestMethod()
    {
        return requestMethod;
    }

    public void setRequestMethod(String requestMethod)
    {
        this.requestMethod = requestMethod;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getErrorMsg()
    {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg)
    {
        this.errorMsg = errorMsg;
    }

    public Long getCostTime()
    {
        return costTime;
    }

    public void setCostTime(Long costTime)
    {
        this.costTime = costTime;
    }
}
