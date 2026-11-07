package com.ruoyi.web.controller.system;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.model.PasswordChangeRequest;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.framework.web.service.AccountService;
import com.ruoyi.system.service.AccountPasswordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/account/password")
public class AccountController
{
    @Autowired private AccountService accounts;
    @Autowired private AccountPasswordService passwords;

    @GetMapping("/status")
    public AjaxResult status()
    {
        AjaxResult result = AjaxResult.success();
        result.put("changeRequired", passwords.requiresChange(SecurityUtils.getLoginUser().getUser().getUserId()));
        result.put("maxAgeDays", passwords.validMaxAgeDays());
        return result;
    }

    @PostMapping("/change")
    @Log(title = "修改密码", businessType = BusinessType.UPDATE, isSaveRequestData = false)
    public AjaxResult change(@RequestBody PasswordChangeRequest body)
    {
        return accounts.change(body);
    }
}
