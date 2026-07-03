package com.ruoyi.web.controller.rag;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.permission.AccessDecisionResult;
import com.ruoyi.system.service.IAccessDecisionService;

/**
 * VACP 访问控制决策控制器
 */
@RestController
@RequestMapping("/rag/access/decision")
public class RagAccessDecisionController
{
    @Autowired
    private IAccessDecisionService accessDecisionService;

    /**
     * 根据当前登录用户和目标文档ID进行访问控制决策。
     *
     * 测试示例：
     * /rag/access/decision/check?docId=DOC-001
     */
    @PreAuthorize("@ss.hasPermi('rag:permissionContext:view')")
    @GetMapping("/check")
    public AjaxResult check(@RequestParam("docId") String docId)
    {
        Long userId = SecurityUtils.getLoginUser().getUser().getUserId();
        String userName = SecurityUtils.getUsername();
        Boolean admin = Long.valueOf(1L).equals(userId);

        AccessDecisionResult result = accessDecisionService.decide(userId, userName, admin, docId);
        return AjaxResult.success(result);
    }
}
