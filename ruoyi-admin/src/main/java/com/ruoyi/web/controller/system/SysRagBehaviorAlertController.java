package com.ruoyi.system.controller;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.domain.SysRagBehaviorAlert;
import com.ruoyi.system.service.ISysRagBehaviorAlertService;
import com.ruoyi.common.utils.SecurityUtils;

/**
 * RAG行为分析告警Controller
 */
@RestController
@RequestMapping("/system/behaviorAlert")
public class SysRagBehaviorAlertController extends BaseController
{
    @Autowired
    private ISysRagBehaviorAlertService sysRagBehaviorAlertService;

    @PreAuthorize("@ss.hasPermi('system:behaviorAlert:list')")
    @GetMapping("/list")
    public TableDataInfo list(SysRagBehaviorAlert sysRagBehaviorAlert)
    {
        startPage();
        List<SysRagBehaviorAlert> list = sysRagBehaviorAlertService.selectSysRagBehaviorAlertList(sysRagBehaviorAlert);
        return getDataTable(list);
    }

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
     * syslog 风格文本导出。
     */
    @PreAuthorize("@ss.hasPermi('system:behaviorAlert:export')")
    @Log(title = "RAG行为告警syslog导出", businessType = BusinessType.EXPORT)
    @RequestMapping(value = "/syslog", method = { RequestMethod.GET, RequestMethod.POST })
    public AjaxResult exportSyslog(SysRagBehaviorAlert sysRagBehaviorAlert)
    {
        String syslogText = sysRagBehaviorAlertService.exportSyslog(sysRagBehaviorAlert);
        AjaxResult ajax = AjaxResult.success();
        ajax.put("syslog", syslogText);
        return ajax;
    }

    @PreAuthorize("@ss.hasPermi('system:behaviorAlert:analyze')")
    @Log(title = "RAG行为分析", businessType = BusinessType.OTHER)
    @PostMapping("/analyze")
    public AjaxResult analyze()
    {
        int count = sysRagBehaviorAlertService.analyzeRagAuditLogs();
        return AjaxResult.success("行为分析完成，新增告警 " + count + " 条");
    }

    /**
     * 处理行为告警。
     *
     * action:
     * CONFIRM    确认告警
     * IGNORE     忽略告警
     * BLOCK_USER 临时封禁用户
     * LIMIT_USER 标记用户高风险，后续查询更容易被限制
     */
    @PreAuthorize("@ss.hasPermi('system:behaviorAlert:analyze')")
    @Log(title = "RAG行为告警处理", businessType = BusinessType.UPDATE)
    @PutMapping("/handle/{id}")
    public AjaxResult handle(@PathVariable("id") Long id,
                              @RequestParam(value = "action", required = false, defaultValue = "CONFIRM") String action,
                              @RequestBody(required = false) SysRagBehaviorAlert alert)
    {
        String handledBy = "admin";
        try
        {
            handledBy = SecurityUtils.getUsername();
        }
        catch (Exception e)
        {
            handledBy = "admin";
        }

        String handleRemark = alert == null ? "" : alert.getHandleRemark();
        return toAjax(sysRagBehaviorAlertService.handleAlert(id, action, handledBy, handleRemark));
    }

    @PreAuthorize("@ss.hasPermi('system:behaviorAlert:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return AjaxResult.success(sysRagBehaviorAlertService.selectSysRagBehaviorAlertById(id));
    }

    @PreAuthorize("@ss.hasPermi('system:behaviorAlert:remove')")
    @Log(title = "RAG行为分析告警", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(sysRagBehaviorAlertService.deleteSysRagBehaviorAlertByIds(ids));
    }
}
