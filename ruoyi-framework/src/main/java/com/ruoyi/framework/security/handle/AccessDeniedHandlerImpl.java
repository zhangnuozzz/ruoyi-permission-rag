package com.ruoyi.framework.security.handle;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.alibaba.fastjson.JSON;
import com.ruoyi.common.core.domain.AjaxResult;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/** 统一已登录用户的接口拒绝响应。 */
@Component
public class AccessDeniedHandlerImpl implements AccessDeniedHandler
{
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException exception) throws IOException
    {
        if (response.isCommitted())
        {
            return;
        }
        response.resetBuffer();
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.getWriter().write(JSON.toJSONString(AjaxResult.error(
                HttpServletResponse.SC_FORBIDDEN, "没有权限，请联系管理员授权")));
    }
}
