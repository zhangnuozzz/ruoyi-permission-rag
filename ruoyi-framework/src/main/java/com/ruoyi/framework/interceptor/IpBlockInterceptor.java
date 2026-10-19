package com.ruoyi.framework.interceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.ruoyi.framework.security.rule.RequestSecurityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/** MVC 安全检查入口，复用前置过滤结果，避免重复查询和重复拦截日志。 */
@Component
public class IpBlockInterceptor implements HandlerInterceptor
{
    @Autowired
    private RequestSecurityService requestSecurityService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception
    {
        return requestSecurityService.check(request, response);
    }
}
