package com.ruoyi.framework.filter;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.ip.IpUtils;
import com.ruoyi.framework.security.rule.StaticSecurityRule;
import com.ruoyi.system.domain.SysAccessLog;
import com.ruoyi.system.service.ISysAccessLogService;
import com.ruoyi.system.service.ISysIpBlacklistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * IP黑名单与静态安全规则过滤器
 *
 * Filter 比 MVC Interceptor 更靠前，可在未登录请求阶段拦截危险访问。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class IpBlockFilter extends OncePerRequestFilter
{
    @Autowired
    private ISysIpBlacklistService sysIpBlacklistService;

    @Autowired
    private ISysAccessLogService sysAccessLogService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException
    {
        String uri = request.getRequestURI();

        if (shouldIgnore(uri))
        {
            filterChain.doFilter(request, response);
            return;
        }

        String ip = IpUtils.getIpAddr(request);
        String denyReason = null;

        if (!StaticSecurityRule.isLocalIp(ip) && sysIpBlacklistService.isIpBlocked(ip))
        {
            denyReason = "IP命中黑名单";
        }

        String staticRuleReason = StaticSecurityRule.check(request);
        if (staticRuleReason != null)
        {
            denyReason = staticRuleReason;
        }

        if (denyReason != null)
        {
            recordBlockedLog(request, ip, denyReason);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":403,\"msg\":\"请求已被安全规则拦截：" + denyReason + "\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void recordBlockedLog(HttpServletRequest request, String ip, String denyReason)
    {
        try
        {
            SysAccessLog accessLog = new SysAccessLog();
            accessLog.setIpaddr(ip);
            accessLog.setRequestUri(request.getRequestURI());
            accessLog.setRequestMethod(request.getMethod());
            accessLog.setStatus("1");
            accessLog.setErrorMsg(denyReason);
            accessLog.setCostTime(0L);

            try
            {
                accessLog.setUserName(SecurityUtils.getUsername());
                if (SecurityUtils.getLoginUser() != null && SecurityUtils.getLoginUser().getUser() != null)
                {
                    accessLog.setUserId(SecurityUtils.getLoginUser().getUser().getUserId());
                }
            }
            catch (Exception ignored)
            {
                accessLog.setUserName("anonymous");
            }

            sysAccessLogService.insertSysAccessLog(accessLog);
        }
        catch (Exception ignored)
        {
            // 安全日志异常不能影响拦截逻辑
        }
    }

    private boolean shouldIgnore(String uri)
    {
        if (uri == null)
        {
            return true;
        }

        return uri.contains("/captchaImage")
                || uri.contains("/login")
                || uri.contains("/logout")
                || uri.contains("/profile")
                || uri.contains("/common/download")
                || uri.contains("/common/upload")
                || uri.contains("/webjars")
                || uri.contains("/favicon.ico");
    }
}
