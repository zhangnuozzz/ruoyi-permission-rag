package com.ruoyi.framework.security.handle;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import io.jsonwebtoken.JwtException;
import com.alibaba.fastjson.JSON;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.framework.manager.AsyncManager;
import com.ruoyi.framework.manager.factory.AsyncFactory;
import com.ruoyi.framework.web.service.TokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

/** 退出既有会话；过期或畸形令牌视为已无可用会话，允许前端清理登录状态。 */
@Configuration
public class LogoutSuccessHandlerImpl implements LogoutSuccessHandler
{
    private static final Logger log = LoggerFactory.getLogger(LogoutSuccessHandlerImpl.class);

    @Autowired
    private TokenService tokenService;

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException
    {
        LoginUser session;
        try
        {
            session = tokenService.getLoginUser(request);
        }
        catch (JwtException | IllegalArgumentException e)
        {
            session = null;
        }
        catch (RuntimeException e)
        {
            unavailable(response, e);
            return;
        }
        if (session != null)
        {
            try
            {
                tokenService.delLoginUser(session.getToken());
            }
            catch (RuntimeException e)
            {
                unavailable(response, e);
                return;
            }
            try
            {
                AsyncManager.me().execute(AsyncFactory.recordLogininfor(session.getUsername(), Constants.LOGOUT, "退出成功"));
            }
            catch (RuntimeException e)
            {
                log.warn("退出日志写入失败，既有会话已撤销", e);
            }
        }
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        response.getWriter().write(JSON.toJSONString(AjaxResult.success("退出成功")));
    }

    private void unavailable(HttpServletResponse response, RuntimeException exception) throws IOException
    {
        log.error("服务器会话撤销暂不可用", exception);
        response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        response.getWriter().write(JSON.toJSONString(AjaxResult.error(503, "服务器会话暂时无法撤销，请稍后重试")));
    }
}
