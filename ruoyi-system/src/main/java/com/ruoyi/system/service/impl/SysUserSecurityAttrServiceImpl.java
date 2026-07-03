package com.ruoyi.system.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.SysUserSecurityAttrMapper;
import com.ruoyi.system.domain.SysUserSecurityAttr;
import com.ruoyi.system.service.ISysUserSecurityAttrService;

/**
 * 用户安全属性Service业务层处理
 */
@Service
public class SysUserSecurityAttrServiceImpl implements ISysUserSecurityAttrService
{
    @Autowired
    private SysUserSecurityAttrMapper sysUserSecurityAttrMapper;

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
    public List<SysUserSecurityAttr> selectSysUserSecurityAttrList(SysUserSecurityAttr sysUserSecurityAttr)
    {
        return sysUserSecurityAttrMapper.selectSysUserSecurityAttrList(sysUserSecurityAttr);
    }

    @Override
    public int insertSysUserSecurityAttr(SysUserSecurityAttr sysUserSecurityAttr)
    {
        if (sysUserSecurityAttr.getSecretLevel() == null)
        {
            sysUserSecurityAttr.setSecretLevel("PUBLIC");
        }
        if (sysUserSecurityAttr.getAccessStatus() == null)
        {
            sysUserSecurityAttr.setAccessStatus("ACTIVE");
        }
        if (sysUserSecurityAttr.getRiskLevel() == null)
        {
            sysUserSecurityAttr.setRiskLevel("LOW");
        }
        if (sysUserSecurityAttr.getFailCount() == null)
        {
            sysUserSecurityAttr.setFailCount(0);
        }
        return sysUserSecurityAttrMapper.insertSysUserSecurityAttr(sysUserSecurityAttr);
    }

    @Override
    public int updateSysUserSecurityAttr(SysUserSecurityAttr sysUserSecurityAttr)
    {
        return sysUserSecurityAttrMapper.updateSysUserSecurityAttr(sysUserSecurityAttr);
    }

    @Override
    public int deleteSysUserSecurityAttrByIds(Long[] ids)
    {
        return sysUserSecurityAttrMapper.deleteSysUserSecurityAttrByIds(ids);
    }

    @Override
    public int deleteSysUserSecurityAttrById(Long id)
    {
        return sysUserSecurityAttrMapper.deleteSysUserSecurityAttrById(id);
    }
}
