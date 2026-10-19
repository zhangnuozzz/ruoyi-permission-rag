package com.ruoyi.framework.filter;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.ruoyi.framework.security.rule.RequestSecurityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** 认证前校验请求来源，阻止黑名单和危险请求进入业务接口。 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class IpBlockFilter extends OncePerRequestFilter
{
    @Autowired
    private RequestSecurityService requestSecurityService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException
    {
        if (requestSecurityService.check(request, response))
        {
            filterChain.doFilter(request, response);
        }
    }
}
