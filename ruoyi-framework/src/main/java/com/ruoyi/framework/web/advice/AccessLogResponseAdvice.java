package com.ruoyi.framework.web.advice;

import javax.servlet.http.HttpServletRequest;
import com.ruoyi.common.core.domain.AjaxResult;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/** 提取统一响应的业务状态供访问监控使用，不改变接口响应及 HTTP 状态。 */
@RestControllerAdvice
public class AccessLogResponseAdvice implements ResponseBodyAdvice<Object>
{
    public static final String BUSINESS_CODE_ATTRIBUTE = AccessLogResponseAdvice.class.getName() + ".code";
    public static final String BUSINESS_MESSAGE_ATTRIBUTE = AccessLogResponseAdvice.class.getName() + ".message";

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType)
    {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType, ServerHttpRequest request,
            ServerHttpResponse response)
    {
        if (body instanceof AjaxResult && request instanceof ServletServerHttpRequest)
        {
            AjaxResult result = (AjaxResult) body;
            HttpServletRequest servlet = ((ServletServerHttpRequest) request).getServletRequest();
            int code;
            try
            {
                code = Integer.parseInt(String.valueOf(result.get(AjaxResult.CODE_TAG)));
            }
            catch (NumberFormatException exception)
            {
                code = -1;
            }
            servlet.setAttribute(BUSINESS_CODE_ATTRIBUTE, code);
            Object message = result.get(AjaxResult.MSG_TAG);
            servlet.setAttribute(BUSINESS_MESSAGE_ATTRIBUTE, message instanceof String ? message : "业务响应状态缺失或无效");
        }
        return body;
    }
}
