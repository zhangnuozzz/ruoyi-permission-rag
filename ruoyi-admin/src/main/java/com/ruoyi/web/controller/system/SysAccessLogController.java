package com.ruoyi.web.controller.system;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.utils.poi.TableExportSupport;
import com.ruoyi.system.domain.export.AccessLogExportRow;
import com.ruoyi.system.domain.SysAccessLog;
import com.ruoyi.system.service.ISysAccessLogService;

/**
 * 系统访问监控日志 Controller
 */
@RestController
@RequestMapping("/system/accessLog")
public class SysAccessLogController extends BaseController
{
    @Autowired
    private ISysAccessLogService sysAccessLogService;

    /**
     * 查询系统访问监控日志列表
     */
    @PreAuthorize("@ss.hasPermi('system:accessLog:list')")
    @GetMapping("/list")
    public TableDataInfo list(SysAccessLog sysAccessLog)
    {
        startPage();
        List<SysAccessLog> list = sysAccessLogService.selectSysAccessLogList(sysAccessLog);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('system:accessLog:export')")
    @Log(title = "访问监控导出", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public AjaxResult export(SysAccessLog query)
    {
        List<SysAccessLog> records = TableExportSupport.select(() -> sysAccessLogService.selectSysAccessLogList(query));
        List<AccessLogExportRow> rows = records.stream().map(AccessLogExportRow::new).collect(Collectors.toList());
        return new ExcelUtil<AccessLogExportRow>(AccessLogExportRow.class).exportExcel(rows, "访问监控数据");
    }

    /**
     * 获取系统访问监控日志详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:accessLog:query')")
    @GetMapping(value = "/{accessId}")
    public AjaxResult getInfo(@PathVariable("accessId") Long accessId)
    {
        return AjaxResult.success(sysAccessLogService.selectSysAccessLogById(accessId));
    }

    /**
     * 删除系统访问监控日志
     */
    @PreAuthorize("@ss.hasPermi('system:accessLog:remove')")
    @Log(title = "系统访问监控日志", businessType = BusinessType.DELETE)
    @DeleteMapping("/{accessIds}")
    public AjaxResult remove(@PathVariable Long[] accessIds)
    {
        return toAjax(sysAccessLogService.deleteSysAccessLogByIds(accessIds));
    }

    /**
     * 清空系统访问监控日志
     */
    @PreAuthorize("@ss.hasPermi('system:accessLog:remove')")
    @Log(title = "系统访问监控日志", businessType = BusinessType.CLEAN)
    @DeleteMapping("/clean")
    public AjaxResult clean()
    {
        sysAccessLogService.cleanSysAccessLog();
        return AjaxResult.success();
    }
}
