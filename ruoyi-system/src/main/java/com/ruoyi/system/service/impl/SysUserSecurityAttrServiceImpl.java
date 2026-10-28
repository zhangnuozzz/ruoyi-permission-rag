package com.ruoyi.system.service.impl;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.exception.CustomException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.mapper.SysUserSecurityAttrMapper;
import com.ruoyi.system.domain.SysUserSecurityAttr;
import com.ruoyi.system.service.ISysUserSecurityAttrService;

@Service
public class SysUserSecurityAttrServiceImpl implements ISysUserSecurityAttrService
{
    @Autowired
    private SysUserSecurityAttrMapper sysUserSecurityAttrMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public SysUserSecurityAttr selectSysUserSecurityAttrById(Long id)
    {
        return sysUserSecurityAttrMapper.selectSysUserSecurityAttrById(id);
    }

    @Override
    public SysUserSecurityAttr selectSysUserSecurityAttrByUserId(Long userId)
    {
        return sysUserSecurityAttrMapper.selectSysUserSecurityAttrByUserId(userId);
    }

    @Override
    public List<SysUserSecurityAttr> selectSysUserSecurityAttrList(SysUserSecurityAttr query)
    {
        return sysUserSecurityAttrMapper.selectSysUserSecurityAttrList(query);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertSysUserSecurityAttr(SysUserSecurityAttr input)
    {
        if (input == null || input.getId() != null)
        {
            throw new CustomException("新增安全属性不能为空且不能指定记录ID");
        }
        Map<String, Object> user = lockUser(input.getUserId());
        if (!jdbcTemplate.queryForList("select id from sys_user_security_attr where user_id = ? for update",
                Long.class, input.getUserId()).isEmpty())
        {
            throw new CustomException("该人员已配置安全属性，请修改已有记录");
        }
        SysUserSecurityAttr value = writable(input, null);
        value.setUserId(input.getUserId());
        copyIdentity(value, user);
        value.setFailCount(0);
        value.setCreateBy(SecurityUtils.getUsername());
        try
        {
            requireWrite(sysUserSecurityAttrMapper.insertSysUserSecurityAttr(value));
        }
        catch (DuplicateKeyException exception)
        {
            throw new CustomException("该人员已配置安全属性，请刷新后核对");
        }
        number(value.getId());
        input.setId(value.getId());
        return 1;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateSysUserSecurityAttr(SysUserSecurityAttr input)
    {
        if (input == null) throw new CustomException("安全属性不能为空");
        Map<String, Object> initial = readAttribute(input.getId(), false);
        Long userId = number(initial.get("user_id"));
        Map<String, Object> user = lockUser(userId);
        Map<String, Object> current = readAttribute(input.getId(), true);
        if (!userId.equals(number(current.get("user_id")))
                || (input.getUserId() != null && !userId.equals(input.getUserId())))
        {
            throw new CustomException("安全属性不能改绑其他人员");
        }
        SysUserSecurityAttr value = writable(input, current);
        value.setId(input.getId());
        copyIdentity(value, user);
        value.setUpdateBy(SecurityUtils.getUsername());
        requireWrite(sysUserSecurityAttrMapper.updateSysUserSecurityAttr(value));
        if ("LOCKED".equals(current.get("access_status")) && "ACTIVE".equals(value.getAccessStatus()))
        {
            requireWrite(jdbcTemplate.update("update sys_user_security_attr set fail_count = 0, "
                    + "lock_until = null, lock_reason = null where id = ?", input.getId()));
        }
        return 1;
    }

    private SysUserSecurityAttr writable(SysUserSecurityAttr input, Map<String, Object> current)
    {
        SysUserSecurityAttr value = new SysUserSecurityAttr();
        value.setSecretLevel(code(input.getSecretLevel(), current, "secret_level", "PUBLIC",
                "PUBLIC", "INTERNAL", "SECRET", "CONFIDENTIAL"));
        value.setAccessStatus(code(input.getAccessStatus(), current, "access_status", "ACTIVE",
                "ACTIVE", "DISABLED", "LOCKED"));
        value.setRiskLevel(code(input.getRiskLevel(), current, "risk_level", "LOW", "LOW", "MEDIUM", "HIGH"));
        String start = current == null ? null : text(current.get("access_start_time"));
        String end = current == null ? null : text(current.get("access_end_time"));
        if (input.hasAccessStartTime() || input.hasAccessEndTime())
        {
            if (!input.hasAccessStartTime() || !input.hasAccessEndTime())
                throw new CustomException("访问时间须同时提交开始和结束，或同时留空");
            start = time(input.getAccessStartTime());
            end = time(input.getAccessEndTime());
        }
        if ((start == null) != (end == null)) throw new CustomException("访问时间须成对设置或同时清空");
        value.setAccessStartTime(start);
        value.setAccessEndTime(end);
        String remark = input.getRemark() == null && current != null
                ? text(current.get("remark")) : input.getRemark();
        if (remark != null && remark.length() > 500) throw new CustomException("备注不能超过500个字符");
        value.setRemark(remark);
        return value;
    }

    private Map<String, Object> lockUser(Long userId)
    {
        number(userId);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("select user_id, user_name, nick_name "
                + "from sys_user where user_id = ? and del_flag = '0' for update", userId);
        if (rows.size() != 1) throw new CustomException("人员不存在或已删除");
        return rows.get(0);
    }

    private Map<String, Object> readAttribute(Long id, boolean lock)
    {
        number(id);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("select id, user_id, secret_level, "
                + "access_status, access_start_time, access_end_time, risk_level, remark "
                + "from sys_user_security_attr where id = ?" + (lock ? " for update" : ""), id);
        if (rows.size() != 1) throw new CustomException("安全属性不存在，请刷新后重试");
        return rows.get(0);
    }

    private void copyIdentity(SysUserSecurityAttr value, Map<String, Object> user)
    {
        value.setUserName((String) user.get("user_name"));
        value.setNickName((String) user.get("nick_name"));
    }

    private String code(String input, Map<String, Object> current, String field, String fallback, String... allowed)
    {
        String value = input == null ? (current == null ? fallback : text(current.get(field))) : input.trim();
        if (value != null) value = value.toUpperCase(Locale.ROOT);
        for (String item : allowed) if (item.equals(value)) return item;
        throw new CustomException("密级、访问状态或风险等级无效，请重新选择");
    }

    private String time(String input)
    {
        String value = text(input);
        if (value == null) return null;
        if (value.matches("[0-9]{2}:[0-9]{2}")) value += ":00";
        if (!value.matches("[0-9]{2}:[0-9]{2}:[0-9]{2}")) throw new CustomException("访问时间格式须为HH:mm:ss");
        try
        {
            return LocalTime.parse(value).format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
        }
        catch (DateTimeParseException exception)
        {
            throw new CustomException("访问时间无效");
        }
    }

    private String text(Object value)
    {
        if (value == null) return null;
        String result = String.valueOf(value).trim();
        return result.isEmpty() ? null : result;
    }

    private Long number(Object value)
    {
        if (!(value instanceof Number) || ((Number) value).longValue() <= 0)
            throw new CustomException("记录或人员ID无效");
        return ((Number) value).longValue();
    }

    private void requireWrite(int rows)
    {
        if (rows != 1) throw new CustomException("安全属性未能完整保存，本次修改已回滚");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteSysUserSecurityAttrByIds(Long[] ids)
    {
        if (ids == null || ids.length == 0) throw new CustomException("请选择安全属性记录");
        Set<Long> ordered = new TreeSet<Long>();
        Set<Long> owners = new TreeSet<Long>();
        for (Long id : ids)
        {
            ordered.add(number(id));
            Object owner = readAttribute(id, false).get("user_id");
            if (owner != null) owners.add(number(owner));
        }
        for (Long owner : owners)
            jdbcTemplate.queryForList("select user_id from sys_user where user_id = ? for update", owner);
        for (Long id : ordered)
        {
            Map<String, Object> row = readAttribute(id, true);
            Long live = jdbcTemplate.queryForObject("select count(*) from sys_user where user_id = ? "
                    + "and (del_flag is null or del_flag <> '2')", Long.class, row.get("user_id"));
            if (live == null || live > 0) throw new CustomException("人员仍存在，不能删除其安全属性；请按需禁用访问");
        }
        int rows = sysUserSecurityAttrMapper.deleteSysUserSecurityAttrByIds(ordered.toArray(new Long[0]));
        if (rows != ordered.size()) throw new CustomException("安全属性未能全部删除，本次操作已回滚");
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteSysUserSecurityAttrById(Long id)
    {
        return deleteSysUserSecurityAttrByIds(new Long[]{id});
    }
}
