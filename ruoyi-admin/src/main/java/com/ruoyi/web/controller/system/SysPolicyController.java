package com.ruoyi.system.controller;

import java.util.List;
import javax.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.SysPolicy;
import com.ruoyi.system.service.ISysPolicyService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 权限策略定义Controller
 * 
 * @author zhangnuo
 * @date 2026-05-05
 */
@RestController
@RequestMapping("/system/policy")
public class SysPolicyController extends BaseController
{
    @Autowired
    private ISysPolicyService sysPolicyService;

    /**
     * 查询权限策略定义列表
     */
    @PreAuthorize("@ss.hasPermi('system:policy:list')")
    @GetMapping("/list")
    public TableDataInfo list(SysPolicy sysPolicy)
    {
        startPage();
        List<SysPolicy> list = sysPolicyService.selectSysPolicyList(sysPolicy);
        return getDataTable(list);
    }

    /**
     * 导出权限策略定义列表
     */
    @PreAuthorize("@ss.hasPermi('system:policy:export')")
    @Log(title = "权限策略定义", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public AjaxResult export(HttpServletResponse response, SysPolicy sysPolicy)
    {
        List<SysPolicy> list = sysPolicyService.selectSysPolicyList(sysPolicy);
        ExcelUtil<SysPolicy> util = new ExcelUtil<SysPolicy>(SysPolicy.class);
        return util.exportExcel(list, "权限策略定义数据");
    }

    /**
     * 获取权限策略定义详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:policy:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return AjaxResult.success(sysPolicyService.selectSysPolicyById(id));
    }

    /**
     * 新增权限策略定义
     */
    @PreAuthorize("@ss.hasPermi('system:policy:add')")
    @Log(title = "权限策略定义", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody SysPolicy sysPolicy)
    {
        return toAjax(sysPolicyService.insertSysPolicy(sysPolicy));
    }

    /**
     * 修改权限策略定义
     */
    @PreAuthorize("@ss.hasPermi('system:policy:edit')")
    @Log(title = "权限策略定义", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody SysPolicy sysPolicy)
    {
        return toAjax(sysPolicyService.updateSysPolicy(sysPolicy));
    }

    /**
     * 删除权限策略定义
     */
    @PreAuthorize("@ss.hasPermi('system:policy:remove')")
    @Log(title = "权限策略定义", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(sysPolicyService.deleteSysPolicyByIds(ids));
    }
}
