package com.ruoyi.framework.security.rule;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import com.alibaba.fastjson.JSON;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.ip.IpUtils;
import com.ruoyi.system.domain.SysAccessLog;
import com.ruoyi.system.service.IpAccessService;
import com.ruoyi.system.service.ISysAccessLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** 请求来源与静态规则统一校验，同一次请求只判定和记录一次。 */
@Service
public class RequestSecurityService
{
    private static final Logger log = LoggerFactory.getLogger(RequestSecurityService.class);
    private static final String CHECKED = RequestSecurityService.class.getName() + ".checked";

    @Autowired
    private IpAccessService ipAccessService;

    @Autowired
    private ISysAccessLogService accessLogService;

    public boolean check(HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        if (Boolean.TRUE.equals(request.getAttribute(CHECKED)) || shouldIgnore(request.getRequestURI()))
        {
            return true;
        }
        String ip = IpUtils.getIpAddr(request);
        String reason = StaticSecurityRule.check(request);
        int status = HttpServletResponse.SC_FORBIDDEN;
        if (reason == null)
        {
            try
            {
                IpAccessService.Decision decision = ipAccessService.evaluate(ip);
                if (decision == IpAccessService.Decision.BLACKLIST)
                {
                    reason = "IP命中黑名单";
                }
                else if (decision == IpAccessService.Decision.INVALID)
                {
                    reason = "无法确认请求来源IP";
                }
            }
            catch (RuntimeException e)
            {
                log.error("IP规则查询失败，已拒绝当前请求", e);
                reason = "IP安全检查暂不可用，请稍后重试";
                status = HttpServletResponse.SC_SERVICE_UNAVAILABLE;
            }
        }
        if (reason == null)
        {
            request.setAttribute(CHECKED, Boolean.TRUE);
            return true;
        }
        recordBlockedLog(request, ip, reason);
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        response.getWriter().write(JSON.toJSONString(AjaxResult.error(status, "请求已被安全规则拦截：" + reason)));
        return false;
    }

    private boolean shouldIgnore(String uri)
    {
        return "/captchaImage".equals(uri) || "/logout".equals(uri) || "/error".equals(uri)
                || "/favicon.ico".equals(uri) || uri != null && (uri.startsWith("/profile/avatar/")
                || uri.startsWith("/webjars/"));
    }

    private void recordBlockedLog(HttpServletRequest request, String ip, String reason)
    {
        try
        {
            SysAccessLog entry = new SysAccessLog();
            entry.setIpaddr(ip);
            entry.setRequestUri(request.getRequestURI());
            entry.setRequestMethod(request.getMethod());
            entry.setStatus("1");
            entry.setErrorMsg(reason);
            entry.setCostTime(0L);
            entry.setUserName("anonymous");
            try
            {
                entry.setUserName(SecurityUtils.getUsername());
                entry.setUserId(SecurityUtils.getLoginUser().getUser().getUserId());
            }
            catch (RuntimeException ignored)
            {
                // 认证前的拦截记录保留来源地址，不伪造用户身份。
            }
            accessLogService.insertSysAccessLog(entry);
        }
        catch (RuntimeException e)
        {
            log.warn("安全拦截日志写入失败，拦截结果保持不变", e);
        }
    }
}
