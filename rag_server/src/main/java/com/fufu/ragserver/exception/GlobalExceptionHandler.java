package com.fufu.ragserver.exception;

import com.fufu.ragserver.domain.AjaxResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ServiceException.class)
    public AjaxResult handleServiceException(ServiceException e)
    {
        log.warn("RAG文件业务处理失败：{}", e.getMessage(), e);
        return AjaxResult.error(e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public AjaxResult handleException(Exception e)
    {
        log.error("RAG文件处理异常", e);
        return AjaxResult.error("RAG文件处理失败：" + e.getMessage());
    }
}
