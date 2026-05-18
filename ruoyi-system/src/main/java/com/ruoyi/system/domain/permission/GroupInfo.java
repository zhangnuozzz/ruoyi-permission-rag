package com.ruoyi.system.domain.permission;

import java.io.Serializable;

/**
 * 用户组信息对象
 *
 * 用于权限上下文中描述当前用户所属的用户组。
 */
public class GroupInfo implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 用户组ID */
    private Long groupId;

    /** 用户组编码 */
    private String groupCode;

    /** 用户组名称 */
    private String groupName;

    /** 知悉范围编码 */
    private String scopeCode;

    /** 状态（0正常 1停用） */
    private String status;

    public Long getGroupId()
    {
        return groupId;
    }

    public void setGroupId(Long groupId)
    {
        this.groupId = groupId;
    }

    public String getGroupCode()
    {
        return groupCode;
    }

    public void setGroupCode(String groupCode)
    {
        this.groupCode = groupCode;
    }

    public String getGroupName()
    {
        return groupName;
    }

    public void setGroupName(String groupName)
    {
        this.groupName = groupName;
    }

    public String getScopeCode()
    {
        return scopeCode;
    }

    public void setScopeCode(String scopeCode)
    {
        this.scopeCode = scopeCode;
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
