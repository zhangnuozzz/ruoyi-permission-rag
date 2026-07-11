package com.fufu.ragserver.domain;

/**
 * 上传用户上下文
 *
 * @author fufu
 * @date 2026-05-12
 */
public class UploadUserContext
{
    private Long userId;
    private String username;
    private String groupId;
    private String groupName;

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public String getUsername()
    {
        return username;
    }

    public void setUsername(String username)
    {
        this.username = username;
    }

    public String getGroupId()
    {
        return groupId;
    }

    public void setGroupId(String groupId)
    {
        this.groupId = groupId;
    }

    public String getGroupName()
    {
        return groupName;
    }

    public void setGroupName(String groupName)
    {
        this.groupName = groupName;
    }
}
