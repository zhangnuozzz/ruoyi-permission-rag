package com.ruoyi.system.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.utils.SecurityUtils;

/**
 * 用户组成员管理
 *
 * 不新增业务表，继续使用 sys_user_group_rel。
 * 将原来的“用户-组关系管理”能力并入“用户组管理”。
 */
@RestController
@RequestMapping("/system/group/member")
public class SysGroupMemberController extends BaseController
{
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 查询各用户组成员数量
     */
    @PreAuthorize("@ss.hasPermi('system:group:list')")
    @GetMapping("/counts")
    public AjaxResult counts()
    {
        String sql =
                "select g.id as groupId, count(r.id) as memberCount " +
                "from sys_group g " +
                "left join sys_user_group_rel r on r.group_id = g.id " +
                "where g.del_flag = '0' " +
                "group by g.id";

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        return AjaxResult.success(rows);
    }

    /**
     * 查询指定用户组成员
     */
    @PreAuthorize("@ss.hasPermi('system:group:list')")
    @GetMapping("/list/{groupId}")
    public AjaxResult list(@PathVariable Long groupId)
    {
        String sql =
                "select " +
                "u.user_id as userId, " +
                "u.user_name as userName, " +
                "u.nick_name as nickName, " +
                "u.status as status, " +
                "r.create_time as joinTime " +
                "from sys_user_group_rel r " +
                "inner join sys_user u on u.user_id = r.user_id " +
                "where r.group_id = ? " +
                "and u.del_flag = '0' " +
                "order by u.user_id";

        return AjaxResult.success(
                jdbcTemplate.queryForList(sql, groupId)
        );
    }

    /**
     * 查询尚未加入当前组的用户
     */
    @PreAuthorize("@ss.hasPermi('system:group:list')")
    @GetMapping("/candidates/{groupId}")
    public AjaxResult candidates(
            @PathVariable Long groupId,
            @RequestParam(value = "keyword", required = false) String keyword)
    {
        String value = keyword == null ? "" : keyword.trim();

        String sql =
                "select " +
                "u.user_id as userId, " +
                "u.user_name as userName, " +
                "u.nick_name as nickName, " +
                "u.status as status " +
                "from sys_user u " +
                "where u.del_flag = '0' " +
                "and not exists (" +
                "   select 1 from sys_user_group_rel r " +
                "   where r.user_id = u.user_id " +
                "   and r.group_id = ?" +
                ") " +
                "and (" +
                "   ? = '' " +
                "   or u.user_name like concat('%', ?, '%') " +
                "   or u.nick_name like concat('%', ?, '%')" +
                ") " +
                "order by u.user_id";

        return AjaxResult.success(
                jdbcTemplate.queryForList(
                        sql,
                        groupId,
                        value,
                        value,
                        value
                )
        );
    }

    /**
     * 添加成员
     */
    @PreAuthorize("@ss.hasPermi('system:group:edit')")
    @PostMapping
    public AjaxResult add(@RequestBody Map<String, Object> body)
    {
        Object groupIdObj = body.get("groupId");
        Object userIdObj = body.get("userId");

        if (groupIdObj == null || userIdObj == null)
        {
            return AjaxResult.error("用户组和用户不能为空");
        }

        Long groupId = Long.valueOf(String.valueOf(groupIdObj));
        Long userId = Long.valueOf(String.valueOf(userIdObj));

        Integer groupCount = jdbcTemplate.queryForObject(
                "select count(1) from sys_group " +
                "where id = ? and del_flag = '0'",
                Integer.class,
                groupId
        );

        Integer userCount = jdbcTemplate.queryForObject(
                "select count(1) from sys_user " +
                "where user_id = ? and del_flag = '0'",
                Integer.class,
                userId
        );

        if (groupCount == null || groupCount == 0)
        {
            return AjaxResult.error("用户组不存在");
        }

        if (userCount == null || userCount == 0)
        {
            return AjaxResult.error("用户不存在");
        }

        Integer exists = jdbcTemplate.queryForObject(
                "select count(1) from sys_user_group_rel " +
                "where user_id = ? and group_id = ?",
                Integer.class,
                userId,
                groupId
        );

        if (exists != null && exists > 0)
        {
            return AjaxResult.success("用户已在该用户组中");
        }

        int rows = jdbcTemplate.update(
                "insert into sys_user_group_rel " +
                "(user_id, group_id, remark, create_by, create_time) " +
                "values (?, ?, ?, ?, now())",
                userId,
                groupId,
                "通过用户组管理页面添加",
                SecurityUtils.getUsername()
        );

        return toAjax(rows);
    }

    /**
     * 移出成员
     */
    @PreAuthorize("@ss.hasPermi('system:group:edit')")
    @DeleteMapping("/{groupId}/{userId}")
    public AjaxResult remove(
            @PathVariable Long groupId,
            @PathVariable Long userId)
    {
        int rows = jdbcTemplate.update(
                "delete from sys_user_group_rel " +
                "where group_id = ? and user_id = ?",
                groupId,
                userId
        );

        return toAjax(rows);
    }
}
