package com.ruoyi.system.domain.permission;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.ruoyi.common.exception.CustomException;

/** 附加访问时段规则，使用完整匹配的有限语法，不执行表达式代码。 */
public final class WorkingHoursRule
{
    private static final Pattern WINDOW = Pattern.compile(
            "time\\s+not\\s+in\\s*\\[\\s*([0-9]{2}:[0-9]{2})\\s*-\\s*([0-9]{2}:[0-9]{2})\\s*\\]",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern RESOURCE = Pattern.compile(
            "level\\s+in\\s*\\[\\s*((?:PUBLIC|INTERNAL|SECRET|CONFIDENTIAL)(?:\\s*,\\s*(?:PUBLIC|INTERNAL|SECRET|CONFIDENTIAL))*)\\s*\\]");
    private static final List<String> LEVELS = Arrays.asList("PUBLIC", "INTERNAL", "SECRET", "CONFIDENTIAL");

    private WorkingHoursRule() {}

    public static boolean isDeny(String effect)
    {
        return "1".equals(effect) || "DENY".equalsIgnoreCase(effect);
    }

    public static boolean isAllow(String effect)
    {
        return "0".equals(effect) || "ALLOW".equalsIgnoreCase(effect);
    }

    public static boolean supports(String effect, String resource, String environment)
    {
        return isDeny(effect) && parseWindow(environment) != null && parseLevels(resource) != null;
    }

    public static boolean appliesToAll(String resource)
    {
        Set<String> levels = parseLevels(resource);
        return levels != null && levels.size() == LEVELS.size();
    }

    public static boolean isOutside(LocalTime now)
    {
        return isOutside("time not in [09:00-18:00]", now);
    }

    public static boolean isOutside(String expression, LocalTime now)
    {
        LocalTime[] window = parseWindow(expression);
        if (window == null || now == null) return true;
        if (window[0].isBefore(window[1]))
        {
            return now.isBefore(window[0]) || !now.isBefore(window[1]);
        }
        return now.isBefore(window[0]) && !now.isBefore(window[1]);
    }

    public static String denialReason(PermissionContext context, String level)
    {
        if (context == null || context.getPolicies() == null) return "POLICY_CONTEXT_UNAVAILABLE";
        for (PolicyInfo policy : context.getPolicies())
        {
            if (policy == null) return "INVALID_ACCESS_POLICY";
            if ("1".equals(policy.getStatus())) continue;
            if (isAllow(policy.getEffect())) continue;
            if (!supports(policy.getEffect(), policy.getResourceExpr(), policy.getEnvExpr()))
                return "UNSUPPORTED_ACCESS_POLICY";
            if (parseLevels(policy.getResourceExpr()).contains(level)
                    && isOutside(policy.getEnvExpr(), requestTime(context)))
                return "OUT_OF_POLICY_TIME_WINDOW";
        }
        return null;
    }

    public static String restrictFilter(PermissionContext context, String base)
    {
        if (base == null || base.trim().isEmpty()) throw new CustomException("检索权限条件缺失");
        Set<String> excluded = new LinkedHashSet<String>();
        for (String level : LEVELS)
        {
            String reason = denialReason(context, level);
            if (reason == null) continue;
            if (!"OUT_OF_POLICY_TIME_WINDOW".equals(reason))
                throw new CustomException("访问策略不可用，请核对策略配置");
            excluded.add("\"" + level + "\"");
        }
        if (excluded.isEmpty()) return base;
        return "(" + base + ") and security_level not in [" + String.join(",", excluded) + "]";
    }

    private static LocalTime requestTime(PermissionContext context)
    {
        String value = context.getRequestTime();
        if (value == null || !value.matches("[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2}"))
            return null;
        try
        {
            return LocalTime.parse(value.substring(11), DateTimeFormatter.ISO_LOCAL_TIME);
        }
        catch (DateTimeParseException exception)
        {
            return null;
        }
    }

    private static LocalTime[] parseWindow(String expression)
    {
        if (expression == null || expression.length() > 500) return null;
        Matcher match = WINDOW.matcher(expression.trim());
        if (!match.matches()) return null;
        try
        {
            LocalTime start = LocalTime.parse(match.group(1));
            LocalTime end = LocalTime.parse(match.group(2));
            return start.equals(end) ? null : new LocalTime[]{start, end};
        }
        catch (DateTimeParseException exception)
        {
            return null;
        }
    }

    private static Set<String> parseLevels(String expression)
    {
        if (expression == null || expression.trim().isEmpty()) return new LinkedHashSet<String>(LEVELS);
        if (expression.length() > 500) return null;
        Matcher match = RESOURCE.matcher(expression.trim());
        if (!match.matches()) return null;
        Set<String> levels = new LinkedHashSet<String>();
        for (String value : match.group(1).split(","))
        {
            if (!levels.add(value.trim())) return null;
        }
        return levels;
    }
}
