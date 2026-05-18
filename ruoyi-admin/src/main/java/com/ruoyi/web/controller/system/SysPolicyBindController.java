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
import com.ruoyi.system.domain.SysPolicyBind;
import com.ruoyi.system.service.ISysPolicyBindService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 策略绑定管理Controller
 * 
 * @author zhangnuo
 * @date 2026-05-05
 */
@RestController
@RequestMapping("/system/policyBind")
public class SysPolicyBindController extends BaseController
{
    @Autowired
    private ISysPolicyBindService sysPolicyBindService;

    /**
     * 查询策略绑定管理列表
     */
    @PreAuthorize("@ss.hasPermi('system:policyBind:list')")
    @GetMapping("/list")
    public TableDataInfo list(SysPolicyBind sysPolicyBind)
    {
        startPage();
        List<SysPolicyBind> list = sysPolicyBindService.selectSysPolicyBindList(sysPolicyBind);
        return getDataTable(list);
    }

    /**
     * 导出策略绑定管理列表
     */
    @PreAuthorize("@ss.hasPermi('system:policyBind:export')")
    @Log(title = "策略绑定管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public AjaxResult export(HttpServletResponse response, SysPolicyBind sysPolicyBind)
    {
        List<SysPolicyBind> list = sysPolicyBindService.selectSysPolicyBindList(sysPolicyBind);
        ExcelUtil<SysPolicyBind> util = new ExcelUtil<SysPolicyBind>(SysPolicyBind.class);
        return util.exportExcel(list, "策略绑定管理数据");
    }

    /**
     * 获取策略绑定管理详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:policyBind:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return AjaxResult.success(sysPolicyBindService.selectSysPolicyBindById(id));
    }

    /**
     * 新增策略绑定管理
     */
    @PreAuthorize("@ss.hasPermi('system:policyBind:add')")
    @Log(title = "策略绑定管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody SysPolicyBind sysPolicyBind)
    {
        return toAjax(sysPolicyBindService.insertSysPolicyBind(sysPolicyBind));
    }

    /**
     * 修改策略绑定管理
     */
    @PreAuthorize("@ss.hasPermi('system:policyBind:edit')")
    @Log(title = "策略绑定管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody SysPolicyBind sysPolicyBind)
    {
        return toAjax(sysPolicyBindService.updateSysPolicyBind(sysPolicyBind));
    }

    /**
     * 删除策略绑定管理
     */
    @PreAuthorize("@ss.hasPermi('system:policyBind:remove')")
    @Log(title = "策略绑定管理", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(sysPolicyBindService.deleteSysPolicyBindByIds(ids));
    }
}
