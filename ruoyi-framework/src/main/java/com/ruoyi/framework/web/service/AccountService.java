package com.ruoyi.framework.web.service;

import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.core.domain.model.PasswordChangeRequest;
import com.ruoyi.common.utils.ServletUtils;
import com.ruoyi.system.service.AccountPasswordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AccountService
{
    @Autowired private TokenService tokenService;
    @Autowired private AccountPasswordService passwords;

    public AjaxResult change(PasswordChangeRequest body)
    {
        LoginUser session = tokenService.getLoginUser(ServletUtils.getRequest());
        String encoded = passwords.change(session.getUser().getUserId(), body.getOldPassword(), body.getNewPassword(), false);
        session.getUser().setPassword(encoded);
        session.setPasswordChangeRequired(false);
        tokenService.setLoginUser(session);
        return AjaxResult.success("密码已修改");
    }
}
