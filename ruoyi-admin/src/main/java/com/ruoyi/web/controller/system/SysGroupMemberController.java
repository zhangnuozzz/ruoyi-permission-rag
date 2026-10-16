package com.ruoyi.system.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.exception.CustomException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.service.ISysUserService;

/**
 * 维护用户组的知悉人员关系，不改变人员密级和系统角色。
 */
@RestController
@RequestMapping("/system/group/member")
public class SysGroupMemberController extends BaseController
{
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ISysUserService userService;

    @PreAuthorize("@ss.hasPermi('system:group:list')")
    @GetMapping("/counts")
    public AjaxResult counts()
    {
        String sql =
                "select g.id as groupId, count(distinct u.user_id) as memberCount " +
                "from sys_group g " +
                "left join sys_user_group_rel r on r.group_id = g.id " +
                "left join sys_user u on u.user_id = r.user_id and u.del_flag = '0' " +
                "where g.del_flag = '0' group by g.id";
        return AjaxResult.success(jdbcTemplate.queryForList(sql));
    }

    @PreAuthorize("@ss.hasPermi('system:group:list')")
    @GetMapping("/list/{groupId}")
    public AjaxResult list(@PathVariable Long groupId)
    {
        requireGroup(groupId, false, false);
        String sql =
                "select u.user_id as userId, u.user_name as userName, " +
                "u.nick_name as nickName, u.status as status, min(r.create_time) as joinTime " +
                "from sys_user_group_rel r " +
                "inner join sys_user u on u.user_id = r.user_id " +
                "where r.group_id = ? and u.del_flag = '0' " +
                "group by u.user_id, u.user_name, u.nick_name, u.status order by u.user_id";
        return AjaxResult.success(jdbcTemplate.queryForList(sql, groupId));
    }

    @PreAuthorize("@ss.hasPermi('system:group:list')")
    @GetMapping("/candidates/{groupId}")
    public AjaxResult candidates(@PathVariable Long groupId,
            @RequestParam(value = "keyword", required = false) String keyword)
    {
        requireGroup(groupId, false, false);
        String value = keyword == null ? "" : keyword.trim();
        String sql =
                "select u.user_id as userId, u.user_name as userName, " +
                "u.nick_name as nickName, u.status as status from sys_user u " +
                "where u.del_flag = '0' " +
                "and not exists (select 1 from sys_user_group_rel r " +
                "where r.user_id = u.user_id and r.group_id = ?) " +
                "and (? = '' or u.user_name like concat('%', ?, '%') " +
                "or u.nick_name like concat('%', ?, '%')) order by u.user_id";
        return AjaxResult.success(jdbcTemplate.queryForList(sql, groupId, value, value, value));
    }

    @PreAuthorize("@ss.hasPermi('system:group:edit')")
    @Log(title = "用户组成员", businessType = BusinessType.INSERT)
    @PostMapping
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult add(@RequestBody Map<String, Object> body)
    {
        if (body == null)
        {
            throw new CustomException("用户组和人员不能为空");
        }
        Long groupId = positiveId(body.get("groupId"), "用户组");
        Long userId = positiveId(body.get("userId"), "人员");
        lockManageableUser(userId);
        requireGroup(groupId, true, true);
        List<Long> existingMembers = jdbcTemplate.queryForList(
                "select id from sys_user_group_rel where user_id = ? and group_id = ? for update",
                Long.class, userId, groupId);
        if (!existingMembers.isEmpty())
        {
            return AjaxResult.success("用户已在该用户组中");
        }
        int rows = jdbcTemplate.update(
                "insert into sys_user_group_rel " +
                "(user_id, group_id, remark, create_by, create_time) values (?, ?, ?, ?, now())",
                userId, groupId, "用户组成员管理", SecurityUtils.getUsername());
        if (rows != 1)
        {
            throw new CustomException("成员添加失败，请刷新后重试");
        }
        return AjaxResult.success();
    }

    @PreAuthorize("@ss.hasPermi('system:group:edit')")
    @Log(title = "用户组成员", businessType = BusinessType.DELETE)
    @DeleteMapping("/{groupId}/{userId}")
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult remove(@PathVariable Long groupId, @PathVariable Long userId)
    {
        groupId = positiveId(groupId, "用户组");
        userId = positiveId(userId, "人员");
        lockManageableUser(userId);
        // 停用组不再新增成员，但保留移出既有成员的维护能力。
        requireGroup(groupId, false, true);
        jdbcTemplate.update("delete from sys_user_group_rel where group_id = ? and user_id = ?",
                groupId, userId);
        return AjaxResult.success();
    }

    private void lockManageableUser(Long userId)
    {
        SysUser target = userService.selectUserById(userId);
        if (target == null || !"0".equals(target.getDelFlag()))
        {
            throw new CustomException("人员不存在或已删除");
        }
        SysUser operator = SecurityUtils.getLoginUser().getUser();
        if (!operator.isAdmin())
        {
            if (target.isAdmin())
            {
                throw new CustomException("无权修改超级管理员的用户组");
            }
            SysUser filter = new SysUser();
            filter.setUserName(target.getUserName());
            List<SysUser> visibleUsers = userService.selectUserList(filter);
            if (visibleUsers == null || visibleUsers.stream()
                    .noneMatch(user -> userId.equals(user.getUserId())))
            {
                throw new CustomException("该人员不在当前账号的数据管理范围内");
            }
        }
        // 与人员权限保存保持相同锁定顺序，串行写入同一人员的成员关系。
        List<Long> users = jdbcTemplate.queryForList(
                "select user_id from sys_user where user_id = ? and del_flag = '0' for update",
                Long.class, userId);
        if (users.isEmpty())
        {
            throw new CustomException("人员不存在或已删除");
        }
    }

    private void requireGroup(Long groupId, boolean adding, boolean locking)
    {
        positiveId(groupId, "用户组");
        List<String> statuses = jdbcTemplate.queryForList(
                "select status from sys_group where id = ? and del_flag = '0'" +
                (locking ? " for update" : ""),
                String.class, groupId);
        if (statuses.isEmpty())
        {
            throw new CustomException("用户组不存在或已删除");
        }
        if (adding && !"0".equals(statuses.get(0)))
        {
            throw new CustomException("用户组已停用，不能添加成员");
        }
    }

    private Long positiveId(Object value, String label)
    {
        try
        {
            Long id = Long.valueOf(String.valueOf(value));
            if (id > 0)
            {
                return id;
            }
        }
        catch (NumberFormatException ignored)
        {
            // 解析失败统一按业务参数错误处理。
        }
        throw new CustomException(label + " ID 必须为正整数");
    }
}
