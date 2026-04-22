package com.ruoyi.system.controller;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
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
 * 策略管理Controller
 * 
 * @author zhangnuo
 * @date 2026-04-20
 */
@RestController
@RequestMapping("/system/policy")
public class SysPolicyController extends BaseController
{
    @Autowired
    private ISysPolicyService sysPolicyService;

    /**
     * 查询策略管理列表
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
     * 导出策略管理列表
     */
    @PreAuthorize("@ss.hasPermi('system:policy:export')")
    @Log(title = "策略管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, SysPolicy sysPolicy)
    {
        List<SysPolicy> list = sysPolicyService.selectSysPolicyList(sysPolicy);
        ExcelUtil<SysPolicy> util = new ExcelUtil<SysPolicy>(SysPolicy.class);
        util.exportExcel(response, list, "策略管理数据");
    }

    /**
     * 获取策略管理详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:policy:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(sysPolicyService.selectSysPolicyById(id));
    }

    /**
     * 新增策略管理
     */
    @PreAuthorize("@ss.hasPermi('system:policy:add')")
    @Log(title = "策略管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody SysPolicy sysPolicy)
    {
        return toAjax(sysPolicyService.insertSysPolicy(sysPolicy));
    }

    /**
     * 修改策略管理
     */
    @PreAuthorize("@ss.hasPermi('system:policy:edit')")
    @Log(title = "策略管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody SysPolicy sysPolicy)
    {
        return toAjax(sysPolicyService.updateSysPolicy(sysPolicy));
    }

    /**
     * 删除策略管理
     */
    @PreAuthorize("@ss.hasPermi('system:policy:remove')")
    @Log(title = "策略管理", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(sysPolicyService.deleteSysPolicyByIds(ids));
    }
}
