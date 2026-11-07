package com.ruoyi.framework.web.service;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import com.ruoyi.common.exception.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

@Service
public class LoginAttemptService
{
    @Autowired private StringRedisTemplate redis;
    @Value("${account.login.maxAttempts:5}") private int maxAttempts;
    @Value("${account.login.windowSeconds:900}") private int windowSeconds;
    @Value("${account.login.lockSeconds:900}") private int lockSeconds;

    private final DefaultRedisScript<Long> script;

    public LoginAttemptService()
    {
        script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("security/login-attempt.lua"));
        script.setResultType(Long.class);
    }

    public void check(String username) { evaluate(username, "check"); }
    public void failure(String username) { evaluate(username, "failure"); }
    public void success(String username) { evaluate(username, "success"); }

    private void evaluate(String username, String action)
    {
        if (maxAttempts < 1 || maxAttempts > 20 || windowSeconds < 1 || lockSeconds < 1)
            throw new IllegalStateException("登录锁定配置无效");
        String identity = DigestUtils.md5DigestAsHex(username.trim().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8));
        Long remaining = redis.execute(script, Arrays.asList("account:failure:" + identity, "account:lock:" + identity),
                action, String.valueOf(maxAttempts), String.valueOf(windowSeconds), String.valueOf(lockSeconds));
        if (remaining == null) throw new IllegalStateException("登录锁定状态不可用");
        if (remaining > 0) throw new CustomException("登录已暂时锁定，请在" + remaining + "秒后重试", 423);
    }
}
