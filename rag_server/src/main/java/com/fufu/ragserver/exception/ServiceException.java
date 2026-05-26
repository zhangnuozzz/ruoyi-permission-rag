package com.fufu.ragserver.exception;

/**
 * 业务异常
 *
 * @author fufu
 * @date 2026-05-12
 */
public class ServiceException extends RuntimeException
{
    public ServiceException(String message)
    {
        super(message);
    }

    public ServiceException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
