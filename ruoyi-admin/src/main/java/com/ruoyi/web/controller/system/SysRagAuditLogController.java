package com.ruoyi.system.controller;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.SysRagAuditLog;
import com.ruoyi.system.service.ISysRagAuditLogService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * RAG检索审计日志Controller
 * 
 * @author zhangnuo
 * @date 2026-05-05
 */
@RestController
@RequestMapping("/system/log")
public class SysRagAuditLogController extends BaseController
{
    @Autowired
    private ISysRagAuditLogService sysRagAuditLogService;

    /**
     * 查询RAG检索审计日志列表
     */
    @PreAuthorize("@ss.hasPermi('system:log:list')")
    @GetMapping("/list")
    public TableDataInfo list(SysRagAuditLog sysRagAuditLog)
    {
        startPage();
        List<SysRagAuditLog> list = sysRagAuditLogService.selectSysRagAuditLogList(sysRagAuditLog);
        return getDataTable(list);
    }

    /**
     * 导出RAG检索审计日志列表
     */
    @PreAuthorize("@ss.hasPermi('system:log:export')")
    @Log(title = "RAG检索审计日志", businessType = BusinessType.EXPORT)
    @RequestMapping(value = "/export", method = { RequestMethod.GET, RequestMethod.POST })
    public AjaxResult export(SysRagAuditLog sysRagAuditLog)
    {
        List<SysRagAuditLog> list = sysRagAuditLogService.selectSysRagAuditLogList(sysRagAuditLog);
        ExcelUtil<SysRagAuditLog> util = new ExcelUtil<SysRagAuditLog>(SysRagAuditLog.class);
        return util.exportExcel(list, "RAG检索审计日志数据");
    }

    /**
     * 获取RAG检索审计日志详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:log:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return AjaxResult.success(sysRagAuditLogService.selectSysRagAuditLogById(id));
    }

    /**
     * 新增RAG检索审计日志
     */
    @PreAuthorize("@ss.hasPermi('system:log:add')")
    @Log(title = "RAG检索审计日志", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody SysRagAuditLog sysRagAuditLog)
    {
        return toAjax(sysRagAuditLogService.insertSysRagAuditLog(sysRagAuditLog));
    }

    /**
     * 修改RAG检索审计日志
     */
    @PreAuthorize("@ss.hasPermi('system:log:edit')")
    @Log(title = "RAG检索审计日志", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody SysRagAuditLog sysRagAuditLog)
    {
        return toAjax(sysRagAuditLogService.updateSysRagAuditLog(sysRagAuditLog));
    }

    /**
     * 删除RAG检索审计日志
     */
    @PreAuthorize("@ss.hasPermi('system:log:remove')")
    @Log(title = "RAG检索审计日志", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(sysRagAuditLogService.deleteSysRagAuditLogByIds(ids));
    }
}
