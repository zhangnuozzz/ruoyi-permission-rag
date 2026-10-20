package com.ruoyi.system.domain.permission;

import java.time.LocalTime;
import java.util.regex.Pattern;

/** 固定时间附加限制；适用人员由策略绑定确定，不解析历史主体表达式。 */
public final class WorkingHoursRule
{
    private static final Pattern EXPRESSION = Pattern.compile(
            "time\\s+not\\s+in\\s*\\[\\s*09:00\\s*-\\s*18:00\\s*\\]", Pattern.CASE_INSENSITIVE);
    private static final LocalTime START = LocalTime.of(9, 0);
    private static final LocalTime END = LocalTime.of(18, 0);

    private WorkingHoursRule()
    {
    }

    public static boolean isDeny(String effect)
    {
        return "1".equals(effect) || "DENY".equalsIgnoreCase(effect);
    }

    public static boolean isAllow(String effect)
    {
        return "0".equals(effect) || "ALLOW".equalsIgnoreCase(effect);
    }

    public static boolean supports(String effect, String resourceExpression, String environmentExpression)
    {
        return isDeny(effect) && (resourceExpression == null || resourceExpression.trim().isEmpty())
                && environmentExpression != null && EXPRESSION.matcher(environmentExpression.trim()).matches();
    }

    /** 每日时间窗口左闭右开，18:00 起属于窗口外。 */
    public static boolean isOutside(LocalTime time)
    {
        return time == null || time.isBefore(START) || !time.isBefore(END);
    }
}
