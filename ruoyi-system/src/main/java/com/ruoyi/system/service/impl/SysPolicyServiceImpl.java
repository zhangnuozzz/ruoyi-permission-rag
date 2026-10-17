package com.ruoyi.system.service.impl;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import com.ruoyi.common.exception.CustomException;
import com.ruoyi.common.utils.DateUtils;
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
    @Transactional
    public int insertSysPolicy(SysPolicy sysPolicy)
    {
        sysPolicy.setCreateTime(DateUtils.getNowDate());
        int rows = sysPolicyMapper.insertSysPolicy(sysPolicy);

        if (rows > 0)
        {
            savePolicyVersion(sysPolicy.getId(), "CREATE", sysPolicy.getCreateBy());
        }

        return rows;
    }

    /**
     * 修改权限策略定义
     *
     * @param sysPolicy 权限策略定义
     * @return 结果
     */
    @Override
    @Transactional
    public int updateSysPolicy(SysPolicy sysPolicy)
    {
        sysPolicy.setUpdateTime(DateUtils.getNowDate());
        int rows = sysPolicyMapper.updateSysPolicy(sysPolicy);

        if (rows > 0)
        {
            savePolicyVersion(sysPolicy.getId(), "UPDATE", sysPolicy.getUpdateBy());
        }

        return rows;
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
            return;
        }

        Integer maxVersion = sysPolicyMapper.selectMaxPolicyVersionNo(policyId);

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

        sysPolicyMapper.insertPolicyVersion(version);
    }

}
