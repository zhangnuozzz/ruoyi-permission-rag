package com.ruoyi.system.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
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
    public int deleteSysPolicyByIds(Long[] ids)
    {
        return sysPolicyMapper.deleteSysPolicyByIds(ids);
    }

    /**
     * 删除权限策略定义信息
     * 
     * @param id 权限策略定义主键
     * @return 结果
     */
    @Override
    public int deleteSysPolicyById(Long id)
    {
        return sysPolicyMapper.deleteSysPolicyById(id);
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
