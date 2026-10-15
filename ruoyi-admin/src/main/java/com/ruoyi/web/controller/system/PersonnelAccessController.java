package com.ruoyi.web.controller.system;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.SysUserSecurityAttr;
import com.ruoyi.system.service.ISysUserSecurityAttrService;

/**
 * 人员权限配置聚合接口
 *
 * 统一维护：
 * 1. 人员安全密级；
 * 2. 访问状态；
 * 3. 访问时间窗口；
 * 4. 用户所属多个知悉用户组。
 *
 * 用户组集合即用户的知悉范围集合。
 */
@RestController
@RequestMapping("/system/personnelAccess")
public class PersonnelAccessController extends BaseController
{
    @Autowired
    private ISysUserSecurityAttrService securityAttrService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 查询人员权限资料。
     */
    @PreAuthorize("@ss.hasPermi('system:user:query')")
    @GetMapping("/{userId}")
    public AjaxResult getProfile(@PathVariable Long userId)
    {
        Map<String, Object> result = new LinkedHashMap<String, Object>();

        SysUserSecurityAttr attr =
                securityAttrService.selectSysUserSecurityAttrByUserId(userId);

        List<Map<String, Object>> groups = jdbcTemplate.queryForList(
                "select " +
                "g.id as groupId, " +
                "g.group_code as groupCode, " +
                "g.group_name as groupName " +
                "from sys_user_group_rel r " +
                "inner join sys_group g on g.id = r.group_id " +
                "where r.user_id = ? " +
                "and g.del_flag = '0' " +
                "and g.status = '0' " +
                "order by g.id",
                userId
        );

        List<Long> groupIds = new ArrayList<Long>();

        for (Map<String, Object> group : groups)
        {
            Object value = group.get("groupId");

            if (value instanceof Number)
            {
                groupIds.add(((Number) value).longValue());
            }
            else if (value != null)
            {
                groupIds.add(Long.valueOf(String.valueOf(value)));
            }
        }

        result.put("securityAttr", attr);
        result.put("groupIds", groupIds);
        result.put("groups", groups);

        return AjaxResult.success(result);
    }

    /**
     * 保存人员权限资料。
     *
     * 核心含义：
     * secretLevel = 人员密级
     * groupIds    = 人员知悉用户组集合
     */
    @PreAuthorize("@ss.hasPermi('system:user:edit')")
    @PutMapping("/{userId}")
    @Transactional
    public AjaxResult saveProfile(
            @PathVariable Long userId,
            @RequestBody Map<String, Object> body)
    {
        String userName = str(body.get("userName"));
        String nickName = str(body.get("nickName"));
        String secretLevel = normalizeLevel(str(body.get("secretLevel")));
        String accessStatus = str(body.get("accessStatus"));
        String accessStartTime = str(body.get("accessStartTime"));
        String accessEndTime = str(body.get("accessEndTime"));

        if (isBlank(accessStatus))
        {
            accessStatus = "ACTIVE";
        }

        SysUserSecurityAttr existing =
                securityAttrService.selectSysUserSecurityAttrByUserId(userId);

        SysUserSecurityAttr attr = new SysUserSecurityAttr();

        attr.setUserId(userId);
        attr.setUserName(userName);
        attr.setNickName(nickName);
        attr.setSecretLevel(secretLevel);
        attr.setAccessStatus(accessStatus);
        attr.setAccessStartTime(accessStartTime);
        attr.setAccessEndTime(accessEndTime);

        if (existing == null)
        {
            attr.setRiskLevel("LOW");
            attr.setFailCount(0);
            attr.setCreateBy(SecurityUtils.getUsername());

            securityAttrService.insertSysUserSecurityAttr(attr);
        }
        else
        {
            attr.setId(existing.getId());

            // 风险数据属于运行状态，不由管理员手工赋值
            attr.setRiskLevel(existing.getRiskLevel());
            attr.setFailCount(existing.getFailCount());
            attr.setLastAccessIp(existing.getLastAccessIp());
            attr.setLastAccessTime(existing.getLastAccessTime());

            attr.setUpdateBy(SecurityUtils.getUsername());

            securityAttrService.updateSysUserSecurityAttr(attr);
        }

        // 用户组集合就是该用户的知悉范围集合
        jdbcTemplate.update(
                "delete from sys_user_group_rel where user_id = ?",
                userId
        );

        Object groupIdsObj = body.get("groupIds");

        if (groupIdsObj instanceof List)
        {
            List<?> groupIds = (List<?>) groupIdsObj;

            for (Object groupIdObj : groupIds)
            {
                if (groupIdObj == null)
                {
                    continue;
                }

                Long groupId =
                        Long.valueOf(String.valueOf(groupIdObj));

                Integer valid = jdbcTemplate.queryForObject(
                        "select count(1) " +
                        "from sys_group " +
                        "where id = ? " +
                        "and status = '0' " +
                        "and del_flag = '0'",
                        Integer.class,
                        groupId
                );

                if (valid != null && valid > 0)
                {
                    jdbcTemplate.update(
                            "insert into sys_user_group_rel " +
                            "(user_id, group_id, remark, create_by, create_time) " +
                            "values (?, ?, ?, ?, now())",
                            userId,
                            groupId,
                            "人员权限管理配置",
                            SecurityUtils.getUsername()
                    );
                }
            }
        }

        return AjaxResult.success();
    }

    private String normalizeLevel(String level)
    {
        if ("INTERNAL".equalsIgnoreCase(level))
        {
            return "INTERNAL";
        }

        if ("SECRET".equalsIgnoreCase(level))
        {
            return "SECRET";
        }

        if ("CONFIDENTIAL".equalsIgnoreCase(level))
        {
            return "CONFIDENTIAL";
        }

        return "PUBLIC";
    }

    private boolean isBlank(String value)
    {
        return value == null || value.trim().length() == 0;
    }

    private String str(Object value)
    {
        return value == null ? null : String.valueOf(value);
    }
}
