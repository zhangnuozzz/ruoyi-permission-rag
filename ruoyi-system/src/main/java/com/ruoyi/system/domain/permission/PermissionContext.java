package com.ruoyi.system.domain.permission;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 权限上下文对象
 *
 * 用于描述一次请求中当前登录用户的权限画像。
 * 后续请求过滤、策略判断、安全检索、审计日志都可以基于该对象展开。
 */
public class PermissionContext implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 当前用户ID */
    private Long userId;

    /** 当前用户名 */
    private String userName;

    /** 是否管理员 */
    private Boolean admin = false;

    /** 当前用户所属用户组 */
    private List<GroupInfo> groups = new ArrayList<>();

    /** 当前用户命中的策略 */
    private List<PolicyInfo> policies = new ArrayList<>();

    /** 当前用户所属组编码集合 */
    private List<String> groupCodes = new ArrayList<>();

    /** 当前用户可访问的知悉范围编码集合 */
    private List<String> scopeCodes = new ArrayList<>();

    /** 请求时间 */
    private String requestTime;

    /** 是否允许本次请求继续执行 */
    private Boolean allowAccess = true;

    /** 拒绝原因 */
    private List<String> denyReasons = new ArrayList<>();

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

    public Boolean getAdmin()
    {
        return admin;
    }

    public void setAdmin(Boolean admin)
    {
        this.admin = admin;
    }

    public List<GroupInfo> getGroups()
    {
        return groups;
    }

    public void setGroups(List<GroupInfo> groups)
    {
        this.groups = groups;
    }

    public List<PolicyInfo> getPolicies()
    {
        return policies;
    }

    public void setPolicies(List<PolicyInfo> policies)
    {
        this.policies = policies;
    }

    public List<String> getGroupCodes()
    {
        return groupCodes;
    }

    public void setGroupCodes(List<String> groupCodes)
    {
        this.groupCodes = groupCodes;
    }

    public List<String> getScopeCodes()
    {
        return scopeCodes;
    }

    public void setScopeCodes(List<String> scopeCodes)
    {
        this.scopeCodes = scopeCodes;
    }

    public String getRequestTime()
    {
        return requestTime;
    }

    public void setRequestTime(String requestTime)
    {
        this.requestTime = requestTime;
    }

    public Boolean getAllowAccess()
    {
        return allowAccess;
    }

    public void setAllowAccess(Boolean allowAccess)
    {
        this.allowAccess = allowAccess;
    }

    public List<String> getDenyReasons()
    {
        return denyReasons;
    }

    public void setDenyReasons(List<String> denyReasons)
    {
        this.denyReasons = denyReasons;
    }
}
