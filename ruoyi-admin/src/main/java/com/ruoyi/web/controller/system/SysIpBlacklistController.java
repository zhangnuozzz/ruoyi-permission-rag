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
import com.ruoyi.system.domain.SysIpBlacklist;
import com.ruoyi.system.service.ISysIpBlacklistService;

/**
 * IP黑名单 Controller
 */
@RestController
@RequestMapping("/system/ipBlacklist")
public class SysIpBlacklistController extends BaseController
{
    @Autowired
    private ISysIpBlacklistService sysIpBlacklistService;

    /**
     * 查询IP黑名单列表
     */
    @PreAuthorize("@ss.hasPermi('system:ipBlacklist:list')")
    @GetMapping("/list")
    public TableDataInfo list(SysIpBlacklist sysIpBlacklist)
    {
        startPage();
        List<SysIpBlacklist> list = sysIpBlacklistService.selectSysIpBlacklistList(sysIpBlacklist);
        return getDataTable(list);
    }

    /**
     * 获取IP黑名单详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:ipBlacklist:query')")
    @GetMapping(value = "/{blacklistId}")
    public AjaxResult getInfo(@PathVariable("blacklistId") Long blacklistId)
    {
        return AjaxResult.success(sysIpBlacklistService.selectSysIpBlacklistById(blacklistId));
    }

    /**
     * 新增IP黑名单
     */
    @PreAuthorize("@ss.hasPermi('system:ipBlacklist:add')")
    @Log(title = "IP黑名单", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody SysIpBlacklist sysIpBlacklist)
    {
        try
        {
            sysIpBlacklist.setCreateBy(SecurityUtils.getUsername());
            return toAjax(sysIpBlacklistService.insertSysIpBlacklist(sysIpBlacklist));
        }
        catch (DuplicateKeyException e)
        {
            return AjaxResult.error("该 IP 已在黑名单中，请修改原记录或更换 IP 地址");
        }
    }

    /**
     * 修改IP黑名单
     */
    @PreAuthorize("@ss.hasPermi('system:ipBlacklist:edit')")
    @Log(title = "IP黑名单", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody SysIpBlacklist sysIpBlacklist)
    {
        try
        {
            sysIpBlacklist.setUpdateBy(SecurityUtils.getUsername());
            return toAjax(sysIpBlacklistService.updateSysIpBlacklist(sysIpBlacklist));
        }
        catch (DuplicateKeyException e)
        {
            return AjaxResult.error("该 IP 已在黑名单中，请修改原记录或更换 IP 地址");
        }
    }

    /**
     * 删除IP黑名单
     */
    @PreAuthorize("@ss.hasPermi('system:ipBlacklist:remove')")
    @Log(title = "IP黑名单", businessType = BusinessType.DELETE)
    @DeleteMapping("/{blacklistIds}")
    public AjaxResult remove(@PathVariable Long[] blacklistIds)
    {
        return toAjax(sysIpBlacklistService.deleteSysIpBlacklistByIds(blacklistIds));
    }
}
