package com.ruoyi.system.service.impl;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.entity.SysDept;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.system.domain.PersonnelSaveRequest;
import com.ruoyi.system.service.IPersonnelAccessService;
import com.ruoyi.system.service.ISysDeptService;
import com.ruoyi.system.service.ISysRoleService;
import com.ruoyi.common.exception.CustomException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.SysUserSecurityAttr;
import com.ruoyi.system.service.ISysUserSecurityAttrService;
import com.ruoyi.system.service.ISysUserService;

/**
 * 人员密级、访问条件及知悉用户组的统一维护入口。
 */
@Service
public class PersonnelAccessServiceImpl implements IPersonnelAccessService
{
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Autowired
    private ISysUserSecurityAttrService securityAttrService;

    @Autowired
    private ISysUserService userService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public Map<String, Object> getProfile(Long userId)
    {
        requireManageableUser(userId, false);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("securityAttr", securityAttrService.selectSysUserSecurityAttrByUserId(userId));

        List<Map<String, Object>> groups = jdbcTemplate.queryForList(
                "select distinct g.id as groupId, g.group_code as groupCode, " +
                "g.group_name as groupName, g.status as groupStatus " +
                "from sys_user_group_rel r inner join sys_group g on g.id = r.group_id " +
                "where r.user_id = ? and g.del_flag = '0' order by g.id",
                userId);

        List<Long> groupIds = new ArrayList<Long>();
        for (Map<String, Object> group : groups)
        {
            groupIds.add(positiveId(group.get("groupId")));
        }
        result.put("groupIds", groupIds);
        result.put("groups", groups);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveAccess(Long userId, Map<String, Object> body)
    {
        writeAccess(requireManageableUser(userId, true), body);
    }

    private void writeAccess(SysUser user, Map<String, Object> body)
    {
        Long userId = user.getUserId();
        if (body == null)
        {
            throw new CustomException("人员权限配置不能为空");
        }
        String secretLevel = requireCode(body.get("secretLevel"), "人员密级",
                "PUBLIC", "INTERNAL", "SECRET", "CONFIDENTIAL");
        String accessStatus = requireCode(body.get("accessStatus"), "访问状态",
                "ACTIVE", "DISABLED", "LOCKED");
        String startTime = parseTime(body.get("accessStartTime"));
        String endTime = parseTime(body.get("accessEndTime"));
        if ((startTime == null) != (endTime == null))
        {
            throw new CustomException("访问时间必须同时填写开始和结束，或同时留空");
        }
        List<Long> groupIds = parseGroupIds(body.get("groupIds"));

        // 序列化同一人员的配置写入，并核对用户组状态及既有成员关系。
        jdbcTemplate.queryForObject(
                "select user_id from sys_user where user_id = ? and del_flag = '0' for update",
                Long.class, userId);
        Set<Long> existingGroupIds = new LinkedHashSet<Long>(jdbcTemplate.queryForList(
                "select group_id from sys_user_group_rel where user_id = ? order by group_id, id for update",
                Long.class, userId));
        validateGroups(groupIds, existingGroupIds);

        saveSecurityAttributes(user, secretLevel, accessStatus, startTime, endTime);
        Set<Long> selectedGroupIds = new LinkedHashSet<Long>(groupIds);
        for (Long groupId : existingGroupIds)
        {
            if (!selectedGroupIds.contains(groupId))
            {
                jdbcTemplate.update("delete from sys_user_group_rel where user_id = ? and group_id = ?",
                        userId, groupId);
            }
        }
        String operator = SecurityUtils.getUsername();
        for (Long groupId : groupIds)
        {
            if (existingGroupIds.contains(groupId))
            {
                continue;
            }
            int rows = jdbcTemplate.update(
                    "insert into sys_user_group_rel " +
                    "(user_id, group_id, remark, create_by, create_time) values (?, ?, ?, ?, now())",
                    userId, groupId, "人员权限管理配置", operator);
            if (rows != 1)
            {
                throw new CustomException("成员关系保存失败，请刷新后重试");
            }
        }
    }

    @Autowired
    private ISysDeptService deptService;

    @Autowired
    private ISysRoleService roleService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPersonnel(PersonnelSaveRequest request)
    {
        requireRequest(request);
        if (request.getUser().getUserId() != null)
        {
            throw new CustomException("新增人员不能指定人员 ID");
        }
        SysUser user = copyIdentity(request.getUser());
        validateDepartment(user.getDeptId(), null);
        validateAssignments(user, Collections.<Long>emptySet(), Collections.<Long>emptySet());
        if (UserConstants.NOT_UNIQUE.equals(userService.checkUserNameUnique(user.getUserName())))
        {
            throw new CustomException("登录账号已存在，请核对后重试");
        }
        validateContact(user);
        String password = request.getUser().getPassword();
        if (password == null || password.trim().isEmpty()
                || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
        {
            throw new CustomException("初始密码不能为空，且 UTF-8 长度不能超过72字节");
        }
        user.setPassword(SecurityUtils.encryptPassword(password));
        user.setCreateBy(SecurityUtils.getUsername());
        if (userService.insertUser(user) != 1 || user.getUserId() == null || user.getUserId() <= 0)
        {
            throw new CustomException("人员创建失败，请核对后重试");
        }
        writeAccess(user, request.getAccess());
        return user.getUserId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePersonnel(Long userId, PersonnelSaveRequest request)
    {
        requireRequest(request);
        SysUser current = requireManageableUser(userId, true);
        jdbcTemplate.queryForObject(
                "select user_id from sys_user where user_id = ? and del_flag = '0' for update",
                Long.class, userId);
        SysUser input = request.getUser();
        if (input.getUserId() != null && !userId.equals(input.getUserId()))
        {
            throw new CustomException("人员 ID 与当前操作对象不一致");
        }
        if (!current.getUserName().equals(requiredText(input.getUserName(), "登录账号", 30)))
        {
            throw new CustomException("已有人员的登录账号不能通过此入口修改");
        }
        if (input.getPassword() != null && !input.getPassword().isEmpty())
        {
            throw new CustomException("修改密码请使用独立的密码重置入口");
        }
        SysUser user = copyIdentity(input);
        user.setUserId(userId);
        user.setUserName(current.getUserName());
        validateDepartment(user.getDeptId(), current.getDeptId());
        Set<Long> currentRoles = new LinkedHashSet<Long>(jdbcTemplate.queryForList(
                "select role_id from sys_user_role where user_id = ? order by role_id for update",
                Long.class, userId));
        Set<Long> currentPosts = new LinkedHashSet<Long>(jdbcTemplate.queryForList(
                "select post_id from sys_user_post where user_id = ? order by post_id for update",
                Long.class, userId));
        validateAssignments(user, currentRoles, currentPosts);
        boolean sameRoles = currentRoles.equals(new LinkedHashSet<Long>(Arrays.asList(user.getRoleIds())));
        boolean samePosts = currentPosts.equals(new LinkedHashSet<Long>(Arrays.asList(user.getPostIds())));
        if (current.isAdmin() && (!sameRoles || !current.getStatus().equals(user.getStatus())))
        {
            throw new CustomException("超级管理员的角色与账号状态不能通过人员配置修改");
        }
        validateContact(user);
        user.setUpdateBy(SecurityUtils.getUsername());
        int rows = sameRoles && samePosts ? userService.updateUserProfile(user) : userService.updateUser(user);
        if (rows != 1)
        {
            throw new CustomException("人员资料保存失败，请刷新后重试");
        }
        // 安全属性或成员关系的任何失败，均回滚本次资料及角色、岗位写入。
        writeAccess(user, request.getAccess());
    }

    private void requireRequest(PersonnelSaveRequest request)
    {
        if (request == null || request.getUser() == null || request.getAccess() == null)
        {
            throw new CustomException("人员资料和权限配置必须同时提交");
        }
    }

    private SysUser copyIdentity(SysUser input)
    {
        SysUser user = new SysUser();
        user.setUserName(requiredText(input.getUserName(), "登录账号", 30));
        user.setNickName(requiredText(input.getNickName(), "姓名", 30));
        user.setDeptId(input.getDeptId());
        user.setEmail(optionalText(input.getEmail(), "邮箱", 50));
        user.setPhonenumber(optionalText(input.getPhonenumber(), "手机号码", 11));
        user.setSex(requireCode(input.getSex(), "性别", "0", "1", "2"));
        user.setStatus(requireCode(input.getStatus(), "账号状态", "0", "1"));
        user.setEthnicity(optionalText(input.getEthnicity(), "民族", 64));
        user.setIdCardNo(optionalText(input.getIdCardNo(), "身份证号码", 64));
        user.setBirthPlace(optionalText(input.getBirthPlace(), "出生地", 255));
        user.setJobTitle(optionalText(input.getJobTitle(), "职务", 128));
        user.setEmployeeNo(optionalText(input.getEmployeeNo(), "工号", 64));
        user.setRemark(optionalText(input.getRemark(), "备注", 500));
        user.setRoleIds(normalizeIds(input.getRoleIds(), "系统角色"));
        user.setPostIds(normalizeIds(input.getPostIds(), "系统岗位"));
        return user;
    }

    private String requiredText(String value, String label, int maximum)
    {
        String text = optionalText(value, label, maximum);
        if (text.isEmpty())
        {
            throw new CustomException(label + "不能为空");
        }
        return text;
    }

    private String optionalText(String value, String label, int maximum)
    {
        String text = value == null ? "" : value.trim();
        if (text.length() > maximum)
        {
            throw new CustomException(label + "长度不能超过" + maximum + "个字符");
        }
        return text;
    }

    private Long[] normalizeIds(Long[] ids, String label)
    {
        if (ids == null)
        {
            throw new CustomException(label + "必须为列表；不配置时请提交空列表");
        }
        Set<Long> result = new LinkedHashSet<Long>();
        for (Long id : ids)
        {
            if (id == null || id <= 0)
            {
                throw new CustomException(label + " ID 必须为正整数");
            }
            result.add(id);
        }
        return result.toArray(new Long[0]);
    }

    private void validateContact(SysUser user)
    {
        if (!user.getEmail().isEmpty())
        {
            if (!user.getEmail().matches("[^\\s@]+@[^\\s@]+")
                    || UserConstants.NOT_UNIQUE.equals(userService.checkEmailUnique(user)))
            {
                throw new CustomException("邮箱格式无效或已被其他人员使用");
            }
        }
        if (!user.getPhonenumber().isEmpty())
        {
            if (!user.getPhonenumber().matches("\\d{11}")
                    || UserConstants.NOT_UNIQUE.equals(userService.checkPhoneUnique(user)))
            {
                throw new CustomException("手机号码格式无效或已被其他人员使用");
            }
        }
    }

    private void validateDepartment(Long deptId, Long currentDeptId)
    {
        if (deptId == null || deptId <= 0)
        {
            throw new CustomException("请选择有效的归属部门");
        }
        List<String> statuses = jdbcTemplate.queryForList(
                "select status from sys_dept where dept_id = ? and del_flag = '0' for update",
                String.class, deptId);
        if (statuses.isEmpty())
        {
            throw new CustomException("归属部门不存在或已删除");
        }
        if (!deptId.equals(currentDeptId))
        {
            if (!"0".equals(statuses.get(0)))
            {
                throw new CustomException("不能将人员分配到停用部门");
            }
            if (!SecurityUtils.getLoginUser().getUser().isAdmin())
            {
                List<SysDept> departments = deptService.selectDeptList(new SysDept());
                if (departments == null || departments.stream().noneMatch(dept -> deptId.equals(dept.getDeptId())))
                {
                    throw new CustomException("归属部门不在当前账号的数据管理范围内");
                }
            }
        }
    }

    private void validateAssignments(SysUser user, Set<Long> currentRoles, Set<Long> currentPosts)
    {
        validateAssignmentIds(user.getRoleIds(), currentRoles, true);
        validateAssignmentIds(user.getPostIds(), currentPosts, false);
    }

    private void validateAssignmentIds(Long[] ids, Set<Long> currentIds, boolean roles)
    {
        if (ids.length == 0)
        {
            return;
        }
        String label = roles ? "系统角色" : "系统岗位";
        String placeholders = String.join(",", Collections.nCopies(ids.length, "?"));
        String sql = roles
                ? "select role_id as id, status, role_key as roleKey from sys_role where del_flag = '0' and role_id in ("
                : "select post_id as id, status from sys_post where post_id in (";
        sql += placeholders + ") order by " + (roles ? "role_id" : "post_id") + " for update";
        List<Map<String, Object>> entries = jdbcTemplate.queryForList(sql, (Object[]) ids);
        if (entries.size() != ids.length)
        {
            throw new CustomException(label + "不存在或已删除，请刷新后核对");
        }
        Set<Long> visibleRoleIds = new LinkedHashSet<Long>();
        if (roles && !SecurityUtils.getLoginUser().getUser().isAdmin())
        {
            for (SysRole role : roleService.selectRoleAll())
            {
                visibleRoleIds.add(role.getRoleId());
            }
        }
        for (Map<String, Object> entry : entries)
        {
            Long id = ((Number) entry.get("id")).longValue();
            boolean existing = currentIds.contains(id);
            String status = String.valueOf(entry.get("status"));
            if (!"0".equals(status) && !(existing && "1".equals(status)))
            {
                throw new CustomException(label + "已停用或状态异常，不能新增关联");
            }
            if (roles && !existing)
            {
                if (id == 1L || "admin".equals(String.valueOf(entry.get("roleKey"))))
                {
                    throw new CustomException("不能通过人员配置授予超级管理员角色");
                }
                if (!SecurityUtils.getLoginUser().getUser().isAdmin() && !visibleRoleIds.contains(id))
                {
                    throw new CustomException("系统角色不在当前账号的数据管理范围内");
                }
            }
        }
    }

    private SysUser requireManageableUser(Long userId, boolean writing)
    {
        if (userId == null || userId <= 0)
        {
            throw new CustomException("请选择有效人员");
        }
        SysUser target = userService.selectUserById(userId);
        if (target == null || !"0".equals(target.getDelFlag()))
        {
            throw new CustomException("人员不存在或已删除");
        }
        SysUser operator = SecurityUtils.getLoginUser().getUser();
        if (!operator.isAdmin())
        {
            if (writing && target.isAdmin())
            {
                throw new CustomException("无权修改超级管理员的人员权限");
            }
            // 复用若依用户列表的数据范围约束，最终按人员 ID 精确确认。
            SysUser filter = new SysUser();
            filter.setUserName(target.getUserName());
            List<SysUser> visibleUsers = userService.selectUserList(filter);
            boolean visible = visibleUsers != null && visibleUsers.stream()
                    .anyMatch(user -> userId.equals(user.getUserId()));
            if (!visible)
            {
                throw new CustomException("该人员不在当前账号的数据管理范围内");
            }
        }
        return target;
    }

    private void validateGroups(List<Long> groupIds, Set<Long> existingGroupIds)
    {
        if (groupIds.isEmpty())
        {
            return;
        }
        String placeholders = String.join(",", Collections.nCopies(groupIds.size(), "?"));
        List<Map<String, Object>> groups = jdbcTemplate.queryForList(
                "select id as groupId, status as groupStatus from sys_group where id in (" +
                placeholders + ") and del_flag = '0' order by id for update", groupIds.toArray());
        if (groups.size() != groupIds.size())
        {
            throw new CustomException("用户组不存在或已删除，请刷新后重新选择");
        }
        for (Map<String, Object> group : groups)
        {
            Long groupId = positiveId(group.get("groupId"));
            String status = String.valueOf(group.get("groupStatus"));
            if ("0".equals(status))
            {
                continue;
            }
            if (!"1".equals(status))
            {
                throw new CustomException("用户组状态异常，请刷新后核对");
            }
            // 停用组仅允许保留既有归属；实际鉴权始终只读取启用组。
            if (!existingGroupIds.contains(groupId))
            {
                throw new CustomException("停用用户组不能新增成员，请刷新后重新选择");
            }
        }
    }

    private void saveSecurityAttributes(SysUser user, String secretLevel, String accessStatus,
            String startTime, String endTime)
    {
        List<Long> attributeIds = jdbcTemplate.queryForList(
                "select id from sys_user_security_attr where user_id = ? for update",
                Long.class, user.getUserId());
        if (attributeIds.size() > 1)
        {
            throw new CustomException("人员安全属性存在重复记录，请先核对数据");
        }
        String operator = SecurityUtils.getUsername();
        if (attributeIds.isEmpty())
        {
            SysUserSecurityAttr attr = new SysUserSecurityAttr();
            attr.setUserId(user.getUserId());
            attr.setUserName(user.getUserName());
            attr.setNickName(user.getNickName());
            attr.setSecretLevel(secretLevel);
            attr.setAccessStatus(accessStatus);
            attr.setAccessStartTime(startTime);
            attr.setAccessEndTime(endTime);
            attr.setRiskLevel("LOW");
            attr.setFailCount(0);
            attr.setCreateBy(operator);
            if (securityAttrService.insertSysUserSecurityAttr(attr) != 1)
            {
                throw new CustomException("人员安全属性保存失败");
            }
            return;
        }

        // 显式写入时间空值以支持清空窗口，运行风险字段不参与配置更新。
        jdbcTemplate.update(
                "update sys_user_security_attr set user_name = ?, nick_name = ?, " +
                "secret_level = ?, access_status = ?, access_start_time = ?, access_end_time = ?, " +
                "update_by = ?, update_time = now() where id = ?",
                user.getUserName(), user.getNickName(), secretLevel, accessStatus,
                startTime, endTime, operator, attributeIds.get(0));
    }

    private List<Long> parseGroupIds(Object value)
    {
        if (!(value instanceof List))
        {
            throw new CustomException("知悉用户组必须为列表；不配置时请提交空列表");
        }
        Set<Long> ids = new LinkedHashSet<Long>();
        for (Object item : (List<?>) value)
        {
            ids.add(positiveId(item));
        }
        return new ArrayList<Long>(ids);
    }

    private Long positiveId(Object value)
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
            // 统一返回业务校验错误，不将数字解析异常作为系统故障抛出。
        }
        throw new CustomException("用户组 ID 必须为正整数");
    }

    private String requireCode(Object value, String label, String... allowed)
    {
        String code = value == null ? "" : String.valueOf(value).trim().toUpperCase(Locale.ROOT);
        for (String candidate : allowed)
        {
            if (candidate.equals(code))
            {
                return code;
            }
        }
        throw new CustomException(label + "无效，请选择有效配置");
    }

    private String parseTime(Object value)
    {
        String text = value == null ? "" : String.valueOf(value).trim();
        if (text.isEmpty())
        {
            return null;
        }
        if (text.matches("\\d{2}:\\d{2}(:\\d{2})?"))
        {
            try
            {
                return LocalTime.parse(text).format(TIME_FORMAT);
            }
            catch (DateTimeParseException ignored)
            {
                // 24:00、非法分秒等值均由 LocalTime 拒绝。
            }
        }
        throw new CustomException("访问时间无效，请使用 HH:mm:ss 格式");
    }
}
