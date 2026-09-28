package com.ruoyi.web.controller.system;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.SysIpWhitelist;
import com.ruoyi.system.service.ISysIpWhitelistService;

/**
 * IP白名单 Controller
 */
@RestController
@RequestMapping("/system/ipWhitelist")
public class SysIpWhitelistController extends BaseController
{
    @Autowired
    private ISysIpWhitelistService sysIpWhitelistService;

    /**
     * 查询IP白名单列表
     */
    @PreAuthorize("@ss.hasPermi('system:ipWhitelist:list')")
    @GetMapping("/list")
    public TableDataInfo list(SysIpWhitelist sysIpWhitelist)
    {
        startPage();
        List<SysIpWhitelist> list = sysIpWhitelistService.selectSysIpWhitelistList(sysIpWhitelist);
        return getDataTable(list);
    }

    /**
     * 获取IP白名单详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:ipWhitelist:query')")
    @GetMapping(value = "/{whitelistId}")
    public AjaxResult getInfo(@PathVariable("whitelistId") Long whitelistId)
    {
        return AjaxResult.success(sysIpWhitelistService.selectSysIpWhitelistById(whitelistId));
    }

    /**
     * 新增IP白名单
     */
    @PreAuthorize("@ss.hasPermi('system:ipWhitelist:add')")
    @Log(title = "IP白名单", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody SysIpWhitelist sysIpWhitelist)
    {
        try
        {
            sysIpWhitelist.setCreateBy(SecurityUtils.getUsername());
            return toAjax(sysIpWhitelistService.insertSysIpWhitelist(sysIpWhitelist));
        }
        catch (DuplicateKeyException e)
        {
            return AjaxResult.error("该 IP 已在白名单中，请修改原记录或更换 IP 地址");
        }
    }

    /**
     * 修改IP白名单
     */
    @PreAuthorize("@ss.hasPermi('system:ipWhitelist:edit')")
    @Log(title = "IP白名单", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody SysIpWhitelist sysIpWhitelist)
    {
        try
        {
            sysIpWhitelist.setUpdateBy(SecurityUtils.getUsername());
            return toAjax(sysIpWhitelistService.updateSysIpWhitelist(sysIpWhitelist));
        }
        catch (DuplicateKeyException e)
        {
            return AjaxResult.error("该 IP 已在白名单中，请修改原记录或更换 IP 地址");
        }
    }

    /**
     * 删除IP白名单
     */
    @PreAuthorize("@ss.hasPermi('system:ipWhitelist:remove')")
    @Log(title = "IP白名单", businessType = BusinessType.DELETE)
    @DeleteMapping("/{whitelistIds}")
    public AjaxResult remove(@PathVariable Long[] whitelistIds)
    {
        return toAjax(sysIpWhitelistService.deleteSysIpWhitelistByIds(whitelistIds));
    }
}
