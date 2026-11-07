package com.ruoyi.common.utils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** 请求内累计访问控制阶段耗时，不包含远端检索及答案生成。 */
public final class AccessTiming
{
    public enum Stage { AUTHENTICATION, PERMISSION_CONTEXT, METADATA_FILTER, DOCUMENT_AUTHORIZATION }
    private static final String KEY = AccessTiming.class.getName();
    private AccessTiming() { }

    public static void record(Stage stage, long startNanos)
    {
        record(currentRequest(), stage, startNanos);
    }

    public static void record(HttpServletRequest request, Stage stage, long startNanos)
    {
        if (request == null) return;
        long[] values = (long[]) request.getAttribute(KEY);
        if (values == null)
        {
            values = new long[Stage.values().length];
            request.setAttribute(KEY, values);
        }
        values[stage.ordinal()] += Math.max(0L, System.nanoTime() - startNanos);
    }

    public static Map<String, Double> snapshot()
    {
        HttpServletRequest request = currentRequest();
        long[] values = request == null ? null : (long[]) request.getAttribute(KEY);
        if (values == null) return Collections.emptyMap();
        Map<String, Double> result = new LinkedHashMap<>();
        long total = 0;
        for (Stage stage : Stage.values())
        {
            long nanos = values[stage.ordinal()];
            result.put(stage.name(), nanos / 1000000.0);
            total += nanos;
        }
        result.put("totalMs", total / 1000000.0);
        return result;
    }

    private static HttpServletRequest currentRequest()
    {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        return attributes instanceof ServletRequestAttributes ? ((ServletRequestAttributes) attributes).getRequest() : null;
    }
}
