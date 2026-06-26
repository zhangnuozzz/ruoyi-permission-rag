package com.ruoyi.system.controller;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.domain.SysRagBehaviorAlert;
import com.ruoyi.system.service.ISysRagBehaviorAlertService;

/**
 * RAG行为分析告警Controller
 */
@RestController
@RequestMapping("/system/behaviorAlert")
public class SysRagBehaviorAlertController extends BaseController
{
    @Autowired
    private ISysRagBehaviorAlertService sysRagBehaviorAlertService;

    /**
     * 查询RAG行为分析告警列表
     */
    @PreAuthorize("@ss.hasPermi('system:behaviorAlert:list')")
    @GetMapping("/list")
    public TableDataInfo list(SysRagBehaviorAlert sysRagBehaviorAlert)
    {
        startPage();
        List<SysRagBehaviorAlert> list = sysRagBehaviorAlertService.selectSysRagBehaviorAlertList(sysRagBehaviorAlert);
        return getDataTable(list);
    }

    /**
     * 导出RAG行为分析告警列表
     */
    @PreAuthorize("@ss.hasPermi('system:behaviorAlert:export')")
    @Log(title = "RAG行为分析告警", businessType = BusinessType.EXPORT)
    @RequestMapping(value = "/export", method = { RequestMethod.GET, RequestMethod.POST })
    public AjaxResult export(SysRagBehaviorAlert sysRagBehaviorAlert)
    {
        List<SysRagBehaviorAlert> list = sysRagBehaviorAlertService.selectSysRagBehaviorAlertList(sysRagBehaviorAlert);
        ExcelUtil<SysRagBehaviorAlert> util = new ExcelUtil<SysRagBehaviorAlert>(SysRagBehaviorAlert.class);
        return util.exportExcel(list, "RAG行为分析告警数据");
    }

    /**
     * 触发行为分析
     */
    @PreAuthorize("@ss.hasPermi('system:behaviorAlert:analyze')")
    @Log(title = "RAG行为分析", businessType = BusinessType.OTHER)
    @PostMapping("/analyze")
    public AjaxResult analyze()
    {
        int count = sysRagBehaviorAlertService.analyzeRagAuditLogs();
        return AjaxResult.success("行为分析完成，新增告警 " + count + " 条");
    }

    /**
     * 获取RAG行为分析告警详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:behaviorAlert:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return AjaxResult.success(sysRagBehaviorAlertService.selectSysRagBehaviorAlertById(id));
    }

    /**
     * 删除RAG行为分析告警
     */
    @PreAuthorize("@ss.hasPermi('system:behaviorAlert:remove')")
    @Log(title = "RAG行为分析告警", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(sysRagBehaviorAlertService.deleteSysRagBehaviorAlertByIds(ids));
    }
}
