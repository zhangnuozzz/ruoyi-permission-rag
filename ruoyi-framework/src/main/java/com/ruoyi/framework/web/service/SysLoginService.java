package com.ruoyi.framework.web.service;

import java.util.Collections;
import javax.annotation.Resource;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.exception.CustomException;
import com.ruoyi.framework.manager.AsyncManager;
import com.ruoyi.framework.manager.factory.AsyncFactory;
import com.ruoyi.system.service.AccountPasswordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

@Component
public class SysLoginService
{
    private static final Logger log = LoggerFactory.getLogger(SysLoginService.class);
    private static final String INVALID = "用户名或密码错误";
    private static final DefaultRedisScript<String> TAKE_CAPTCHA = new DefaultRedisScript<>(
            "local v = redis.call('GET', KEYS[1]); redis.call('DEL', KEYS[1]); return v", String.class);
    @Autowired private TokenService tokenService;
    @Resource private AuthenticationManager authenticationManager;
    @Autowired private RedisCache redisCache;
    @Autowired private LoginAttemptService attempts;
    @Autowired private AccountPasswordService passwords;
    @Autowired private com.ruoyi.system.service.ISysUserService users;

    @SuppressWarnings("unchecked")
    public String login(String username, String password, String code, String uuid)
    {
        String account = username == null ? "" : username.trim();
        if (account.isEmpty() || account.length() > 30 || password == null || password.length() > 128
                || uuid == null || uuid.length() > 64 || code == null || code.length() > 16)
            throw new CustomException(INVALID);
        try
        {
            com.ruoyi.common.core.domain.entity.SysUser current = users.selectUserByUserName(account);
            if (current != null) account = current.getUserName();
            attempts.check(account);
            String captcha = (String) redisCache.redisTemplate.execute(TAKE_CAPTCHA,
                    Collections.singletonList(Constants.CAPTCHA_CODE_KEY + uuid));
            if (captcha == null || !code.equalsIgnoreCase(captcha))
            {
                record(account, Constants.LOGIN_FAIL, "验证码错误或已失效");
                throw new CustomException(INVALID);
            }
            Authentication authentication;
            try
            {
                authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(account, password));
            }
            catch (org.springframework.security.authentication.InternalAuthenticationServiceException exception)
            {
                throw exception;
            }
            catch (AuthenticationException exception)
            {
                record(account, Constants.LOGIN_FAIL, "账号或密码校验失败");
                attempts.failure(account);
                throw new CustomException(INVALID);
            }
            LoginUser loginUser = (LoginUser) authentication.getPrincipal();
            attempts.success(account);
            loginUser.setPasswordChangeRequired(passwords.requiresChange(loginUser.getUser().getUserId()));
            String token = tokenService.createToken(loginUser);
            record(account, Constants.LOGIN_SUCCESS, "登录成功");
            return token;
        }
        catch (CustomException exception)
        {
            throw exception;
        }
        catch (RuntimeException exception)
        {
            log.error("登录校验服务暂不可用", exception);
            throw new CustomException("登录服务暂不可用，请稍后重试", 503);
        }
    }

    private void record(String username, String status, String message)
    {
        AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, status, message));
    }
}
