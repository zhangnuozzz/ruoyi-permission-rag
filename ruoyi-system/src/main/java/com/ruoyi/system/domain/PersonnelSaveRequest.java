package com.ruoyi.system.domain;

import java.util.Map;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import com.ruoyi.common.core.domain.entity.SysUser;

/**
 * 人员资料与访问配置的单次提交请求。
 */
public class PersonnelSaveRequest
{
    @Valid
    @NotNull(message = "人员资料不能为空")
    private SysUser user;

    @NotNull(message = "权限配置不能为空")
    private Map<String, Object> access;

    public SysUser getUser()
    {
        return user;
    }

    public void setUser(SysUser user)
    {
        this.user = user;
    }

    public Map<String, Object> getAccess()
    {
        return access;
    }

    public void setAccess(Map<String, Object> access)
    {
        this.access = access;
    }
}
