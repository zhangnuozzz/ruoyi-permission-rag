package com.ruoyi.web.controller.rag;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.permission.PermissionContext;
import com.ruoyi.system.service.IPermissionContextService;

/**
 * RAG 权限上下文控制器
 *
 * 用于展示当前登录用户在 RAG 检索前的权限画像。
 * 后续真实检索接口接入后，该上下文可直接作为检索请求的权限输入。
 */
@RestController
@RequestMapping("/rag/permission/context")
public class RagPermissionContextController
{
    @Autowired
    private IPermissionContextService permissionContextService;

    /**
     * 查询当前登录用户的权限上下文。
     */
    @PreAuthorize("@ss.hasPermi('rag:permissionContext:view')")
    @GetMapping("/current")
    public AjaxResult current()
    {
        Long userId = SecurityUtils.getLoginUser().getUser().getUserId();
        String userName = SecurityUtils.getUsername();

        /*
         * RuoYi 默认 admin 用户 userId 为 1。
         * 这里先采用最小稳定判断，避免不同版本 SysUser#isAdmin() 兼容问题。
         */
        Boolean admin = Long.valueOf(1L).equals(userId);

        PermissionContext context = permissionContextService.buildContext(userId, userName, admin);
        return AjaxResult.success(context);
    }
}
