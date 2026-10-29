package com.ruoyi.system.service.impl;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import com.ruoyi.common.exception.CustomException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import org.springframework.dao.DuplicateKeyException;
import com.ruoyi.system.domain.permission.WorkingHoursRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.system.mapper.SysPolicyMapper;
import com.ruoyi.system.domain.SysPolicy;
import com.ruoyi.system.domain.SysPolicyVersion;
import com.ruoyi.system.service.ISysPolicyService;

/**
 * 权限策略定义Service业务层处理
 *
 * @author zhangnuo
 * @date 2026-05-05
 */
@Service
public class SysPolicyServiceImpl implements ISysPolicyService
{
    @Autowired
    private SysPolicyMapper sysPolicyMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 查询权限策略定义
     *
     * @param id 权限策略定义主键
     * @return 权限策略定义
     */
    @Override
    public SysPolicy selectSysPolicyById(Long id)
    {
        return sysPolicyMapper.selectSysPolicyById(id);
    }

    /**
     * 查询策略版本历史。
     */
    @Override
    public List<SysPolicyVersion> selectPolicyVersionList(Long policyId)
    {
        return sysPolicyMapper.selectPolicyVersionList(policyId);
    }

    /**
     * 查询权限策略定义列表
     *
     * @param sysPolicy 权限策略定义
     * @return 权限策略定义
     */
    @Override
    public List<SysPolicy> selectSysPolicyList(SysPolicy sysPolicy)
    {
        return sysPolicyMapper.selectSysPolicyList(sysPolicy);
    }

    /**
     * 新增权限策略定义
     *
     * @param sysPolicy 权限策略定义
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertSysPolicy(SysPolicy input)
    {
        if (input == null || input.getId() != null)
        {
            throw new CustomException("新增策略不能为空且不能指定 ID");
        }
        SysPolicy policy = writableFields(input, null);
        policy.setCreateBy(SecurityUtils.getUsername());
        policy.setCreateTime(DateUtils.getNowDate());
        policy.setDelFlag("0");
        try
        {
            requireSingleWrite(sysPolicyMapper.insertSysPolicy(policy));
        }
        catch (DuplicateKeyException exception)
        {
            throw new CustomException("策略编码已存在，请使用其他编码");
        }
        if (policy.getId() == null || policy.getId() <= 0)
        {
            throw new CustomException("策略编号未生成，本次保存已回滚");
        }
        savePolicyVersion(policy.getId(), "CREATE", policy.getCreateBy());
        input.setId(policy.getId());
        return 1;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateSysPolicy(SysPolicy input)
    {
        if (input == null || input.getId() == null || input.getId() <= 0)
        {
            throw new CustomException("策略 ID 无效，请刷新后重试");
        }
        // 与绑定维护和删除共用策略行锁，串行分配同一策略的版本号。
        SysPolicy current = sysPolicyMapper.selectSysPolicyByIdForUpdate(input.getId());
        if (current == null)
        {
            throw new CustomException("策略不存在或已删除");
        }
        SysPolicy policy = writableFields(input, current);
        policy.setId(current.getId());
        policy.setUpdateBy(SecurityUtils.getUsername());
        policy.setUpdateTime(DateUtils.getNowDate());
        try
        {
            requireSingleWrite(sysPolicyMapper.updateSysPolicy(policy));
        }
        catch (DuplicateKeyException exception)
        {
            throw new CustomException("策略编码已存在，请使用其他编码");
        }
        savePolicyVersion(policy.getId(), "UPDATE", policy.getUpdateBy());
        return 1;
    }

    private SysPolicy writableFields(SysPolicy input, SysPolicy current)
    {
        SysPolicy policy = new SysPolicy();
        String code = requiredText(input.getPolicyCode(), 64, "策略编码");
        if (!code.matches("[A-Z][A-Z0-9_]{0,63}")
                && (current == null || !code.equals(current.getPolicyCode())))
        {
            throw new CustomException("策略编码须以大写字母开头，仅使用大写字母、数字和下划线");
        }
        policy.setPolicyCode(code);
        policy.setPolicyName(requiredText(input.getPolicyName(), 100, "策略名称"));
        String effect = text(input.getEffect());
        if (!WorkingHoursRule.isAllow(effect) && !WorkingHoursRule.isDeny(effect))
        {
            throw new CustomException("请选择有效的策略效果");
        }
        policy.setEffect(WorkingHoursRule.isDeny(effect) ? "1" : "0");
        String subject = text(input.getSubjectType());
        if (!"USER".equals(subject) && !"GROUP".equals(subject))
        {
            throw new CustomException("请选择用户或用户组主体类型");
        }
        policy.setSubjectType(subject);
        policy.setSubjectExpr(optionalText(input.getSubjectExpr(), 500, "主体表达式"));
        policy.setResourceExpr(optionalText(input.getResourceExpr(), 500, "资源表达式"));
        policy.setEnvExpr(optionalText(input.getEnvExpr(), 500, "环境表达式"));
        policy.setRemark(optionalText(input.getRemark(), 500, "备注"));
        if (input.getPriority() == null || input.getPriority() < 0 || input.getPriority() > 9999)
        {
            throw new CustomException("预留优先级须为 0 到 9999 的整数");
        }
        policy.setPriority(input.getPriority());
        if (!"0".equals(input.getStatus()) && !"1".equals(input.getStatus()))
        {
            throw new CustomException("请选择有效的策略状态");
        }
        policy.setStatus(input.getStatus());
        if ("0".equals(policy.getStatus()))
        {
            if (!WorkingHoursRule.supports(policy.getEffect(), policy.getResourceExpr(), policy.getEnvExpr()))
            {
                throw new CustomException("请设置有效的访问时段和文档密级范围");
            }
            if (!policy.getSubjectExpr().isEmpty()
                    && (current == null || !policy.getSubjectExpr().equals(text(current.getSubjectExpr()))))
            {
                throw new CustomException("适用对象须通过人员或用户组绑定指定，不能新增未执行的主体表达式");
            }
        }
        return policy;
    }

    private String requiredText(String value, int max, String label)
    {
        String result = optionalText(value, max, label);
        if (result.isEmpty())
        {
            throw new CustomException(label + "不能为空");
        }
        return result;
    }

    private String optionalText(String value, int max, String label)
    {
        String result = text(value);
        if (result.length() > max)
        {
            throw new CustomException(label + "不能超过 " + max + " 个字符");
        }
        return result;
    }

    private String text(String value)
    {
        return value == null ? "" : value.trim();
    }

    private void requireSingleWrite(int rows)
    {
        if (rows != 1)
        {
            throw new CustomException("策略或版本未能完整保存，本次修改已回滚");
        }
    }

    /**
     * 批量删除权限策略定义
     *
     * @param ids 需要删除的权限策略定义主键
     * @return 结果
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public int deleteSysPolicyByIds(Long[] ids)
    {
        if (ids == null || ids.length == 0)
        {
            throw new CustomException("请选择需要删除的策略");
        }
        Set<Long> policyIds = new TreeSet<Long>();
        for (Long id : ids)
        {
            if (id == null || id <= 0)
            {
                throw new CustomException("策略 ID 无效，请刷新后重试");
            }
            policyIds.add(id);
        }
        // 与绑定维护共用策略行锁，按固定顺序锁定后核对整批引用。
        for (Long id : policyIds)
        {
            List<Long> policies = jdbcTemplate.queryForList(
                    "select id from sys_policy where id = ? and del_flag = '0' for update",
                    Long.class, id);
            if (policies.size() != 1)
            {
                throw new CustomException("策略不存在或已删除，请刷新后重试");
            }
        }
        for (Long id : policyIds)
        {
            // 停用绑定也保留关联；删除策略前必须明确取消，避免留下悬空引用。
            Long exists = jdbcTemplate.queryForObject(
                    "select exists(select 1 from sys_policy_bind where policy_id = ?)",
                    Long.class, id);
            if (exists == null || exists != 0)
            {
                throw new CustomException("策略仍有适用对象（含停用绑定），请先取消绑定再删除");
            }
        }
        int rows = sysPolicyMapper.deleteSysPolicyByIds(policyIds.toArray(new Long[0]));
        if (rows != policyIds.size())
        {
            throw new CustomException("策略未能全部删除，请刷新后核对");
        }
        return rows;
    }

    /**
     * 删除权限策略定义信息
     *
     * @param id 权限策略定义主键
     * @return 结果
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public int deleteSysPolicyById(Long id)
    {
        return deleteSysPolicyByIds(new Long[] { id });
    }

    /**
     * 保存当前策略完整快照。
     */
    private void savePolicyVersion(Long policyId, String changeType, String changeBy)
    {
        SysPolicy current = sysPolicyMapper.selectSysPolicyById(policyId);
        if (current == null)
        {
            throw new CustomException("策略快照读取失败，本次保存已回滚");
        }

        Integer maxVersion = sysPolicyMapper.selectMaxPolicyVersionNo(policyId);

        if (maxVersion != null && maxVersion == Integer.MAX_VALUE)
        {
            throw new CustomException("策略版本号已达上限，请联系管理员处理");
        }
        SysPolicyVersion version = new SysPolicyVersion();
        version.setPolicyId(policyId);
        version.setVersionNo(maxVersion == null ? 1 : maxVersion + 1);
        version.setPolicyCode(current.getPolicyCode());
        version.setPolicyName(current.getPolicyName());
        version.setEffect(current.getEffect());
        version.setSubjectType(current.getSubjectType());
        version.setSubjectExpr(current.getSubjectExpr());
        version.setResourceExpr(current.getResourceExpr());
        version.setEnvExpr(current.getEnvExpr());
        version.setPriority(current.getPriority());
        version.setStatus(current.getStatus());
        version.setRemark(current.getRemark());
        version.setChangeType(changeType);
        version.setChangeBy(changeBy == null ? "" : changeBy);
        version.setChangeTime(DateUtils.getNowDate());

        requireSingleWrite(sysPolicyMapper.insertPolicyVersion(version));
    }

}
