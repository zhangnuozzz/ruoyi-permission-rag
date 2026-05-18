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
import com.ruoyi.system.domain.SysRagDoc;
import com.ruoyi.system.service.ISysRagDocService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 文档权限标签Controller
 * 
 * @author zhangnuo
 * @date 2026-05-06
 */
@RestController
@RequestMapping("/system/ragDoc")
public class SysRagDocController extends BaseController
{
    @Autowired
    private ISysRagDocService sysRagDocService;

    /**
     * 查询文档权限标签列表
     */
    @PreAuthorize("@ss.hasPermi('system:ragDoc:list')")
    @GetMapping("/list")
    public TableDataInfo list(SysRagDoc sysRagDoc)
    {
        startPage();
        List<SysRagDoc> list = sysRagDocService.selectSysRagDocList(sysRagDoc);
        return getDataTable(list);
    }

    /**
     * 导出文档权限标签列表
     */
    @PreAuthorize("@ss.hasPermi('system:ragDoc:export')")
    @Log(title = "文档权限标签", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public AjaxResult export(HttpServletResponse response, SysRagDoc sysRagDoc)
    {
        List<SysRagDoc> list = sysRagDocService.selectSysRagDocList(sysRagDoc);
        ExcelUtil<SysRagDoc> util = new ExcelUtil<SysRagDoc>(SysRagDoc.class);
        return util.exportExcel(list, "文档权限标签数据");
    }

    /**
     * 获取文档权限标签详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:ragDoc:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return AjaxResult.success(sysRagDocService.selectSysRagDocById(id));
    }

    /**
     * 新增文档权限标签
     */
    @PreAuthorize("@ss.hasPermi('system:ragDoc:add')")
    @Log(title = "文档权限标签", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody SysRagDoc sysRagDoc)
    {
        return toAjax(sysRagDocService.insertSysRagDoc(sysRagDoc));
    }

    /**
     * 修改文档权限标签
     */
    @PreAuthorize("@ss.hasPermi('system:ragDoc:edit')")
    @Log(title = "文档权限标签", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody SysRagDoc sysRagDoc)
    {
        return toAjax(sysRagDocService.updateSysRagDoc(sysRagDoc));
    }

    /**
     * 删除文档权限标签
     */
    @PreAuthorize("@ss.hasPermi('system:ragDoc:remove')")
    @Log(title = "文档权限标签", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(sysRagDocService.deleteSysRagDocByIds(ids));
    }
}
