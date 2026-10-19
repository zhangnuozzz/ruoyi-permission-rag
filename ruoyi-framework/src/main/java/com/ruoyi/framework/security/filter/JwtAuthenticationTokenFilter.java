package com.ruoyi.framework.security.filter;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import io.jsonwebtoken.JwtException;
import com.alibaba.fastjson.JSON;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.framework.web.service.SessionValidationService;
import com.ruoyi.framework.web.service.TokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** 验证既有令牌、账号当前状态和有效权限后建立本次请求身份。 */
@Component
public class JwtAuthenticationTokenFilter extends OncePerRequestFilter
{
    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationTokenFilter.class);

    @Autowired
    private TokenService tokenService;

    @Autowired
    private SessionValidationService sessionValidationService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request)
    {
        String path = request.getServletPath();
        return "/login".equals(path) || "/captchaImage".equals(path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException
    {
        if (SecurityUtils.getAuthentication() != null)
        {
            chain.doFilter(request, response);
            return;
        }
        LoginUser loginUser;
        try
        {
            loginUser = tokenService.getLoginUser(request);
        }
        catch (JwtException | IllegalArgumentException e)
        {
            unauthenticated(response);
            return;
        }
        catch (RuntimeException e)
        {
            unavailable(response, e);
            return;
        }
        try
        {
            if (loginUser != null)
            {
                if (!sessionValidationService.validateAndRefresh(loginUser))
                {
                    revoke(loginUser);
                    unauthenticated(response);
                    return;
                }
                tokenService.verifyToken(loginUser);
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        loginUser, null, loginUser.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
                request.setAttribute(TokenService.REQUEST_LOGIN_USER, loginUser);
            }
        }
        catch (RuntimeException e)
        {
            unavailable(response, e);
            return;
        }
        chain.doFilter(request, response);
    }

    private void unavailable(HttpServletResponse response, RuntimeException exception) throws IOException
    {
        SecurityContextHolder.clearContext();
        log.error("会话安全复核暂不可用，已拒绝当前请求", exception);
        response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        respond(response, 503, "登录状态暂时无法核验，请稍后重试");
    }

    private void revoke(LoginUser session)
    {
        if (session.getToken() != null && !session.getToken().isEmpty())
        {
            tokenService.delLoginUser(session.getToken());
        }
    }

    private void unauthenticated(HttpServletResponse response) throws IOException
    {
        SecurityContextHolder.clearContext();
        // 沿用框架的业务 401 返回方式，使现有页面正常进入重新登录流程。
        respond(response, 401, "登录状态已失效，请重新登录");
    }

    private void respond(HttpServletResponse response, int code, String message) throws IOException
    {
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        response.getWriter().write(JSON.toJSONString(AjaxResult.error(code, message)));
    }
}
