package com.ruoyi.web.controller.rag;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.security.SecureQueryContext;
import com.ruoyi.system.service.ISecureQueryContextService;

/**
 * VACP 查询安全增强控制器
 */
@RestController
@RequestMapping("/rag/query/security")
public class RagSecureQueryController
{
    @Autowired
    private ISecureQueryContextService secureQueryContextService;

    /**
     * 构建当前用户的检索前安全查询上下文。
     *
     * 测试示例：
     * /rag/query/security/context?query=test&topK=5
     */
    @PreAuthorize("@ss.hasPermi('rag:permissionContext:view')")
    @GetMapping("/context")
    public AjaxResult context(@RequestParam("query") String query,
                              @RequestParam(value = "topK", required = false) Integer topK)
    {
        Long userId = SecurityUtils.getLoginUser().getUser().getUserId();
        String userName = SecurityUtils.getUsername();
        Boolean admin = Long.valueOf(1L).equals(userId);

        SecureQueryContext context = secureQueryContextService.buildContext(userId, userName, admin, query, topK);
        return AjaxResult.success(context);
    }
}
