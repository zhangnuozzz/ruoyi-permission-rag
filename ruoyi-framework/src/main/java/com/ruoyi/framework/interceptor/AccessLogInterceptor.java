package com.ruoyi.framework.interceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ruoyi.common.utils.ServletUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.ip.IpUtils;
import com.ruoyi.system.domain.SysAccessLog;
import com.ruoyi.system.service.ISysAccessLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 系统访问监控日志拦截器
 *
 * 记录后台接口访问来源、访问路径、请求方式、访问状态和耗时。
 */
@Component
public class AccessLogInterceptor implements HandlerInterceptor
{
    private static final String START_TIME = "ACCESS_LOG_START_TIME";

    @Autowired
    private ISysAccessLogService sysAccessLogService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
    {
        request.setAttribute(START_TIME, System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex)
    {
        try
        {
            String uri = request.getRequestURI();

            if (shouldIgnore(uri))
            {
                return;
            }

            Long startTime = (Long) request.getAttribute(START_TIME);
            long costTime = startTime == null ? 0L : System.currentTimeMillis() - startTime;

            SysAccessLog accessLog = new SysAccessLog();
            accessLog.setIpaddr(getClientIp(request));
            accessLog.setRequestUri(uri);
            accessLog.setRequestMethod(request.getMethod());
            accessLog.setCostTime(costTime);

            int statusCode = response.getStatus();
            accessLog.setStatus(statusCode >= 200 && statusCode < 400 && ex == null ? "0" : "1");

            if (ex != null)
            {
                accessLog.setErrorMsg(limit(ex.getMessage(), 1000));
            }
            else if (statusCode >= 400)
            {
                accessLog.setErrorMsg("HTTP Status " + statusCode);
            }
            else
            {
                accessLog.setErrorMsg("");
            }

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
            // 访问日志不能影响主业务请求
        }
    }

    private boolean shouldIgnore(String uri)
    {
        if (uri == null)
        {
            return true;
        }

        return uri.contains("/accessLog/list")
                || uri.contains("/accessLog/")
                || uri.contains("/captchaImage")
                || uri.contains("/profile")
                || uri.contains("/common/download")
                || uri.contains("/common/upload")
                || uri.contains("/webjars")
                || uri.contains("/swagger")
                || uri.contains("/v2/api-docs")
                || uri.contains("/csrf");
    }

    private String getClientIp(HttpServletRequest request)
    {
        String ip = IpUtils.getIpAddr(request);
        if (ip == null || ip.length() == 0)
        {
            ip = ServletUtils.getRequest().getRemoteAddr();
        }
        return ip;
    }

    private String limit(String text, int maxLength)
    {
        if (text == null)
        {
            return "";
        }
        return text.length() <= maxLength ? text : text.substring(0, maxLength);
    }
}
