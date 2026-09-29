package com.ruoyi.web.controller.rag;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.system.service.SituationOverviewService;

/** 管理员态势感知首页。 */
@RestController
@RequestMapping("/rag/dashboard")
public class SituationOverviewController
{
    @Autowired
    private SituationOverviewService situationOverviewService;

    @PreAuthorize("@ss.hasRole('admin')")
    @GetMapping("/overview")
    public AjaxResult overview()
    {
        return AjaxResult.success(situationOverviewService.getOverview());
    }
}
