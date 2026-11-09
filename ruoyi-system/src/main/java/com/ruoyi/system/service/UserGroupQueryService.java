package com.ruoyi.system.service;

import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import com.ruoyi.common.exception.CustomException;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import com.ruoyi.system.domain.permission.GroupInfo;
import com.ruoyi.system.mapper.PermissionContextMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** 复用同一次请求的检索前组归属读取；文档二次鉴权独立读取最新归属。 */
@Service
public class UserGroupQueryService
{
    private static final String REQUEST_KEY = UserGroupQueryService.class.getName() + ".memberships";

    @Autowired
    private PermissionContextMapper permissionContextMapper;

    public List<GroupInfo> selectForRequest(Long userId)
    {
        if (userId == null || userId <= 0)
        {
            throw new CustomException("人员编号无效，无法读取用户组");
        }
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes instanceof ServletRequestAttributes
                ? ((ServletRequestAttributes) attributes).getRequest() : null;
        MembershipSnapshot snapshot = request == null ? null
                : (request.getAttribute(REQUEST_KEY) instanceof MembershipSnapshot
                        ? (MembershipSnapshot) request.getAttribute(REQUEST_KEY) : null);
        if (snapshot != null && userId.equals(snapshot.userId))
        {
            return copy(snapshot.groups);
        }
        List<GroupInfo> groups = permissionContextMapper.selectGroupsByUserId(userId);
        if (groups == null)
        {
            throw new CustomException("用户组归属暂时无法核验");
        }
        List<GroupInfo> saved = copy(groups);
        if (request != null)
        {
            request.setAttribute(REQUEST_KEY, new MembershipSnapshot(userId, saved));
        }
        return copy(saved);
    }

    private List<GroupInfo> copy(List<GroupInfo> groups)
    {
        List<GroupInfo> result = new ArrayList<GroupInfo>(groups.size());
        for (GroupInfo source : groups)
        {
            if (source == null)
            {
                throw new CustomException("用户组归属记录无效");
            }
            GroupInfo item = new GroupInfo();
            item.setGroupId(source.getGroupId());
            item.setGroupCode(source.getGroupCode());
            item.setGroupName(source.getGroupName());
            item.setScopeCode(source.getScopeCode());
            item.setStatus(source.getStatus());
            result.add(item);
        }
        return result;
    }

    private static final class MembershipSnapshot
    {
        private final Long userId;
        private final List<GroupInfo> groups;

        private MembershipSnapshot(Long userId, List<GroupInfo> groups)
        {
            this.userId = userId;
            this.groups = groups;
        }
    }
}
