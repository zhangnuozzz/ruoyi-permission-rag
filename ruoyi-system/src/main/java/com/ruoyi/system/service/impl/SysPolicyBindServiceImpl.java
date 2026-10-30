package com.ruoyi.system.service.impl;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.exception.CustomException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.SysPolicyBind;
import com.ruoyi.system.domain.permission.WorkingHoursRule;
import com.ruoyi.system.mapper.SysPolicyBindMapper;
import com.ruoyi.system.service.ISysPolicyBindService;
import com.ruoyi.system.service.ISysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 维护策略与适用人员、用户组的关联，不改变人员基础访问资格。
 */
@Service
public class SysPolicyBindServiceImpl implements ISysPolicyBindService
{
    @Autowired
    private SysPolicyBindMapper sysPolicyBindMapper;

    @Autowired
    private ISysUserService userService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public SysPolicyBind selectSysPolicyBindById(Long id)
    {
        return sysPolicyBindMapper.selectSysPolicyBindById(id);
    }

    @Override
    public List<SysPolicyBind> selectSysPolicyBindList(SysPolicyBind filter)
    {
        return sysPolicyBindMapper.selectSysPolicyBindList(filter);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertSysPolicyBind(SysPolicyBind input)
    {
        SysPolicyBind binding = validateInput(input);
        if (input.getId() != null)
        {
            throw new CustomException("新增绑定不能指定绑定 ID");
        }
        // 同一策略的关联写入先锁策略行，避免并发校验后重复插入。
        lockPolicy(binding.getPolicyId(), true);
        validateTarget(binding, true);
        rejectDuplicate(binding);
        binding.setCreateBy(SecurityUtils.getUsername());
        binding.setCreateTime(DateUtils.getNowDate());
        int rows = sysPolicyBindMapper.insertSysPolicyBind(binding);
        requireSingleWrite(rows);
        input.setId(binding.getId());
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateSysPolicyBind(SysPolicyBind input)
    {
        SysPolicyBind binding = validateInput(input);
        binding.setId(requireId(input.getId(), "绑定"));
        lockPolicy(binding.getPolicyId(), false);
        SysPolicyBind current = lockBinding(binding.getId());
        if (!binding.getPolicyId().equals(current.getPolicyId())
                || !binding.getBindType().equals(current.getBindType())
                || !binding.getBindTargetId().equals(current.getBindTargetId()))
        {
            throw new CustomException("已有绑定的策略和适用对象不能修改，请取消后重新添加");
        }
        boolean enabling = "0".equals(binding.getStatus()) && !"0".equals(current.getStatus());
        if (enabling)
        {
            lockPolicy(binding.getPolicyId(), true);
        }
        validateTarget(binding, enabling);
        if (enabling)
        {
            rejectDuplicate(binding);
        }
        // 创建人、创建时间和关联对象不接受客户端覆盖。
        int rows = sysPolicyBindMapper.updateSysPolicyBind(binding);
        requireSingleWrite(rows);
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteSysPolicyBindByIds(Long[] ids)
    {
        if (ids == null || ids.length == 0)
        {
            throw new CustomException("请选择需要取消的策略绑定");
        }
        Set<Long> bindingIds = new TreeSet<Long>();
        Set<Long> policyIds = new TreeSet<Long>();
        for (Long id : ids)
        {
            bindingIds.add(requireId(id, "绑定"));
        }
        // 按固定顺序锁定策略与绑定；整批核对完成后再删除。
        for (Long id : bindingIds)
        {
            SysPolicyBind binding = sysPolicyBindMapper.selectSysPolicyBindById(id);
            if (binding == null)
            {
                throw new CustomException("绑定不存在或已取消，请刷新后重试");
            }
            policyIds.add(requireId(binding.getPolicyId(), "策略"));
        }
        for (Long policyId : policyIds)
        {
            // 历史孤立绑定仍允许清理，不要求策略继续存在或启用。
            jdbcTemplate.queryForList("select id from sys_policy where id = ? for update",
                    Long.class, policyId);
        }
        Set<Long> userIds = new TreeSet<Long>();
        for (Long id : bindingIds)
        {
            SysPolicyBind binding = lockBinding(id);
            if (!policyIds.contains(binding.getPolicyId()))
            {
                throw new CustomException("绑定配置已变化，请刷新后重试");
            }
            if ("USER".equals(binding.getBindType()))
            {
                userIds.add(requireId(binding.getBindTargetId(), "人员"));
            }
        }
        for (Long userId : userIds)
        {
            requireManageableUser(userId, false);
        }
        int rows = sysPolicyBindMapper.deleteSysPolicyBindByIds(bindingIds.toArray(new Long[0]));
        if (rows != bindingIds.size())
        {
            throw new CustomException("绑定未能全部取消，请刷新后重试");
        }
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteSysPolicyBindById(Long id)
    {
        return deleteSysPolicyBindByIds(new Long[] { id });
    }

    private SysPolicyBind validateInput(SysPolicyBind input)
    {
        if (input == null)
        {
            throw new CustomException("策略绑定配置不能为空");
        }
        SysPolicyBind binding = new SysPolicyBind();
        binding.setPolicyId(requireId(input.getPolicyId(), "策略"));
        binding.setBindTargetId(requireId(input.getBindTargetId(), "适用对象"));
        if (!Arrays.asList("USER", "GROUP").contains(input.getBindType()))
        {
            throw new CustomException("适用对象类型仅支持用户或用户组");
        }
        binding.setBindType(input.getBindType());
        String status = input.getStatus();
        if (!"0".equals(status) && !"1".equals(status))
        {
            throw new CustomException("绑定状态必须为启用或停用");
        }
        binding.setStatus(status);
        String remark = input.getRemark() == null ? "" : input.getRemark().trim();
        if (remark.length() > 500)
        {
            throw new CustomException("备注不能超过 500 个字符");
        }
        binding.setRemark(remark);
        return binding;
    }

    private void lockPolicy(Long policyId, boolean requireActive)
    {
        List<Map<String, Object>> policies = jdbcTemplate.queryForList(
                "select status, effect, resource_expr, env_expr from sys_policy "
                + "where id = ? and del_flag = '0' for update", policyId);
        if (policies.isEmpty())
        {
            throw new CustomException("策略不存在或已删除");
        }
        Map<String, Object> policy = policies.get(0);
        if (!requireActive) return;
        if (!"0".equals(policy.get("status")))
        {
            throw new CustomException("策略已停用，不能新增或启用绑定");
        }
        if (!WorkingHoursRule.supports((String) policy.get("effect"),
                (String) policy.get("resource_expr"), (String) policy.get("env_expr")))
        {
            throw new CustomException("策略不是有效的访问时段规则，请修改后再绑定");
        }
    }

    private SysPolicyBind lockBinding(Long id)
    {
        // 通过当前读锁核对记录，避免一致性快照返回已被删除的绑定。
        List<Map<String, Object>> values = jdbcTemplate.queryForList(
                "select policy_id, bind_type, bind_target_id, status from sys_policy_bind "
                + "where id = ? for update", id);
        if (values.isEmpty())
        {
            throw new CustomException("绑定不存在或已取消，请刷新后重试");
        }
        Map<String, Object> value = values.get(0);
        SysPolicyBind binding = new SysPolicyBind();
        binding.setId(id);
        binding.setPolicyId(((Number) value.get("policy_id")).longValue());
        binding.setBindType((String) value.get("bind_type"));
        binding.setBindTargetId(((Number) value.get("bind_target_id")).longValue());
        binding.setStatus((String) value.get("status"));
        return binding;
    }

    private void validateTarget(SysPolicyBind binding, boolean requireActive)
    {
        Long targetId = binding.getBindTargetId();
        if ("USER".equals(binding.getBindType()))
        {
            requireManageableUser(targetId, requireActive);
            return;
        }
        List<String> states = jdbcTemplate.queryForList(
                "select status from sys_group where id = ? and del_flag = '0' for update",
                String.class, targetId);
        if (requireActive && (states.isEmpty() || !"0".equals(states.get(0))))
        {
            throw new CustomException("用户组不存在、已删除或已停用，不能新增或启用绑定");
        }
    }

    private void requireManageableUser(Long userId, boolean requireActive)
    {
        requireId(userId, "人员");
        SysUser target = userService.selectUserById(userId);
        SysUser operator = SecurityUtils.getLoginUser().getUser();
        boolean exists = target != null && "0".equals(target.getDelFlag());
        if (!operator.isAdmin())
        {
            if (!exists || target.isAdmin())
            {
                throw new CustomException("该人员不在当前账号的数据管理范围内");
            }
            SysUser filter = new SysUser();
            filter.setUserName(target.getUserName());
            List<SysUser> visible = userService.selectUserList(filter);
            if (visible == null || visible.stream().noneMatch(user -> userId.equals(user.getUserId())))
            {
                throw new CustomException("该人员不在当前账号的数据管理范围内");
            }
        }
        List<String> states = jdbcTemplate.queryForList(
                "select status from sys_user where user_id = ? and del_flag = '0' for update",
                String.class, userId);
        if (requireActive && (states.isEmpty() || !"0".equals(states.get(0))))
        {
            throw new CustomException("人员不存在、已删除或账号已停用，不能新增或启用绑定");
        }
    }

    private void rejectDuplicate(SysPolicyBind binding)
    {
        List<Long> existingIds = jdbcTemplate.queryForList(
                "select id from sys_policy_bind where policy_id = ? and bind_type = ? "
                + "and bind_target_id = ? order by id for update", Long.class,
                binding.getPolicyId(), binding.getBindType(), binding.getBindTargetId());
        if (existingIds.stream().anyMatch(id -> !id.equals(binding.getId())))
        {
            throw new CustomException("该对象已有策略绑定，请先核对现有绑定状态");
        }
    }

    private Long requireId(Long id, String label)
    {
        if (id == null || id <= 0)
        {
            throw new CustomException(label + " ID 必须为正整数");
        }
        return id;
    }

    private void requireSingleWrite(int rows)
    {
        if (rows != 1)
        {
            throw new CustomException("策略绑定保存失败，请刷新后重试");
        }
    }
}
