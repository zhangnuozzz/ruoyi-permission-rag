package com.fufu.ragserver.exception;

import com.fufu.ragserver.domain.AjaxResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * RAG服务异常处理器
 *
 * @author fufu
 * @date 2026-05-12
 */
@RestControllerAdvice
public class GlobalExceptionHandler
{
    @ExceptionHandler(ServiceException.class)
    public AjaxResult handleServiceException(ServiceException e)
    {
        return AjaxResult.error(e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public AjaxResult handleException(Exception e)
    {
        return AjaxResult.error("RAG文件处理失败：" + e.getMessage());
    }
}
