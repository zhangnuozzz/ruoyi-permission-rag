package com.ruoyi.framework.security.rule;

import java.net.URLDecoder;

import javax.servlet.http.HttpServletRequest;

/**
 * 静态安全规则
 *
 * 第一版采用固定规则，后续可扩展为数据库可配置规则。
 */
public class StaticSecurityRule
{
    private static final String[] DANGEROUS_PATHS = {
            "/actuator",
            "/druid",
            "/swagger",
            "/v2/api-docs",
            "/.env",
            "/wp-admin",
            "/phpmyadmin"
    };

    private static final String[] DANGEROUS_KEYWORDS = {
            "../",
            "<script",
            "union select",
            "drop table",
            "insert into",
            "delete from",
            " or 1=1",
            "' or '1'='1"
    };

    private static final String[] LOCAL_IPS = {
            "127.0.0.1",
            "localhost",
            "0:0:0:0:0:0:0:1"
    };

    public static boolean isLocalIp(String ip)
    {
        if (ip == null)
        {
            return false;
        }
        for (String localIp : LOCAL_IPS)
        {
            if (localIp.equalsIgnoreCase(ip))
            {
                return true;
            }
        }
        return false;
    }

    public static String check(HttpServletRequest request)
    {
        if (request == null)
        {
            return null;
        }

        String uri = safeLower(request.getRequestURI());
        String query = safeLower(request.getQueryString());
        String decodedQuery = decode(query);

        for (String path : DANGEROUS_PATHS)
        {
            if (uri.contains(path))
            {
                return "命中危险路径规则：" + path;
            }
        }

        for (String keyword : DANGEROUS_KEYWORDS)
        {
            if (query.contains(keyword) || decodedQuery.contains(keyword))
            {
                return "命中危险参数规则：" + keyword;
            }
        }

        return null;
    }

    public static boolean isSensitiveUri(String uri)
    {
        if (uri == null)
        {
            return false;
        }

        String lowerUri = uri.toLowerCase();

        return lowerUri.contains("/rag/search")
                || lowerUri.contains("/system/policy")
                || lowerUri.contains("/system/policybind")
                || lowerUri.contains("/system/group")
                || lowerUri.contains("/system/ragdoc")
                || lowerUri.contains("/system/user")
                || lowerUri.contains("/system/role");
    }

    private static String decode(String value)
    {
        try
        {
            return value == null ? "" : URLDecoder.decode(value, "UTF-8").toLowerCase();
        }
        catch (Exception e)
        {
            return value == null ? "" : value.toLowerCase();
        }
    }

    private static String safeLower(String value)
    {
        return value == null ? "" : value.toLowerCase();
    }
}
