package com.ruoyi.web.controller.system;

import java.util.List;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.domain.SysUserSecurityAttr;
import com.ruoyi.system.service.ISysUserSecurityAttrService;

/**
 * 用户安全属性Controller
 */
@RestController
@RequestMapping("/system/userSecurityAttr")
public class SysUserSecurityAttrController extends BaseController
{
    @Autowired
    private ISysUserSecurityAttrService sysUserSecurityAttrService;

    @PreAuthorize("@ss.hasPermi('system:userSecurityAttr:list')")
    @GetMapping("/list")
    public TableDataInfo list(SysUserSecurityAttr sysUserSecurityAttr)
    {
        startPage();
        List<SysUserSecurityAttr> list = sysUserSecurityAttrService.selectSysUserSecurityAttrList(sysUserSecurityAttr);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('system:userSecurityAttr:export')")
    @Log(title = "用户安全属性", businessType = BusinessType.EXPORT)
    @RequestMapping(value = "/export", method = { RequestMethod.GET, RequestMethod.POST })
    public AjaxResult export(SysUserSecurityAttr sysUserSecurityAttr)
    {
        List<SysUserSecurityAttr> list = sysUserSecurityAttrService.selectSysUserSecurityAttrList(sysUserSecurityAttr);
        ExcelUtil<SysUserSecurityAttr> util = new ExcelUtil<SysUserSecurityAttr>(SysUserSecurityAttr.class);
        return util.exportExcel(list, "用户安全属性数据");
    }

    @PreAuthorize("@ss.hasPermi('system:userSecurityAttr:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return AjaxResult.success(sysUserSecurityAttrService.selectSysUserSecurityAttrById(id));
    }

    @PreAuthorize("@ss.hasPermi('system:userSecurityAttr:add')")
    @Log(title = "用户安全属性", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody SysUserSecurityAttr sysUserSecurityAttr)
    {
        sysUserSecurityAttr.setCreateBy("admin");
        return toAjax(sysUserSecurityAttrService.insertSysUserSecurityAttr(sysUserSecurityAttr));
    }

    @PreAuthorize("@ss.hasPermi('system:userSecurityAttr:edit')")
    @Log(title = "用户安全属性", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody SysUserSecurityAttr sysUserSecurityAttr)
    {
        sysUserSecurityAttr.setUpdateBy("admin");
        return toAjax(sysUserSecurityAttrService.updateSysUserSecurityAttr(sysUserSecurityAttr));
    }

    @PreAuthorize("@ss.hasPermi('system:userSecurityAttr:remove')")
    @Log(title = "用户安全属性", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(sysUserSecurityAttrService.deleteSysUserSecurityAttrByIds(ids));
    }
}
