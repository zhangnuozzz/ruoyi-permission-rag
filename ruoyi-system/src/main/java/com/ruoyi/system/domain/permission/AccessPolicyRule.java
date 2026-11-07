package com.ruoyi.system.domain.permission;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.ruoyi.common.exception.CustomException;
import com.ruoyi.common.utils.ip.IpUtils;

/** 有限语法的附加访问限制；在基础权限内裁决，不执行用户表达式。 */
public final class AccessPolicyRule
{
    private static final List<String> LEVELS = Arrays.asList("PUBLIC", "INTERNAL", "SECRET", "CONFIDENTIAL");
    private static final List<String> RISKS = Arrays.asList("LOW", "MEDIUM", "HIGH");
    private static final Pattern WINDOW = Pattern.compile("time\\s+not\\s+in\\s*\\[\\s*([0-9]{2}:[0-9]{2})\\s*-\\s*([0-9]{2}:[0-9]{2})\\s*\\]", Pattern.CASE_INSENSITIVE);
    private static final Pattern NETWORKS = Pattern.compile("ip\\s+not\\s+in\\s*\\[\\s*([^\\[\\]]+)\\s*\\]", Pattern.CASE_INSENSITIVE);
    private static final Pattern RISK = Pattern.compile("risk\\s+above\\s*\\[\\s*(LOW|MEDIUM|HIGH)\\s*\\]", Pattern.CASE_INSENSITIVE);
    private static final Pattern RESOURCE = Pattern.compile("level\\s+in\\s*\\[\\s*((?:PUBLIC|INTERNAL|SECRET|CONFIDENTIAL)(?:\\s*,\\s*(?:PUBLIC|INTERNAL|SECRET|CONFIDENTIAL))*)\\s*\\]");
    private static final Set<String> CONDITION_DENIALS = new HashSet<>(Arrays.asList(
            "OUT_OF_POLICY_TIME_WINDOW", "OUT_OF_POLICY_IP_RANGE", "ABOVE_POLICY_RISK_LEVEL"));

    private AccessPolicyRule() { }

    public static boolean isDeny(String value) { return "1".equals(value) || "DENY".equalsIgnoreCase(value); }
    public static boolean isAllow(String value) { return "0".equals(value) || "ALLOW".equalsIgnoreCase(value); }
    public static boolean supports(String effect, String resource, String environment)
    {
        return (isDeny(effect) || isAllow(effect)) && parseLevels(resource) != null && parseConditions(effect, environment) != null;
    }
    public static boolean requiresRisk(String expression)
    {
        Conditions parsed = parseConditions("1", expression);
        if (parsed == null) parsed = parseConditions("0", expression);
        return parsed != null && parsed.maxRisk != null;
    }

    public static String admissionDenial(PermissionContext context)
    {
        String invalid = validate(context);
        if (invalid != null) return invalid;
        String firstReason = null;
        boolean allDenied = true;
        for (String level : LEVELS)
        {
            String reason = decideLevel(context, level);
            if (reason != null && !CONDITION_DENIALS.contains(reason)) return reason;
            if (reason == null) allDenied = false;
            else if (firstReason == null) firstReason = reason;
        }
        return allDenied ? firstReason : null;
    }

    public static String denialReason(PermissionContext context, String level)
    {
        String invalid = validate(context);
        return invalid == null ? decideLevel(context, level) : invalid;
    }

    private static String decideLevel(PermissionContext context, String level)
    {
        int winner = Integer.MAX_VALUE;
        String denial = null;
        for (PolicyInfo policy : context.getPolicies())
        {
            if (!activePolicy(policy) || !parseLevels(policy.getResourceExpr()).contains(level)) continue;
            String violation = parseConditions(policy.getEffect(), policy.getEnvExpr()).denial(context);
            if (violation != null && !CONDITION_DENIALS.contains(violation)) return violation;
            boolean deny = isDeny(policy.getEffect());
            if (!(deny ? violation != null : violation == null)) continue;
            int priority = policy.getPriority();
            if (priority < winner)
            {
                winner = priority;
                denial = deny ? violation : null;
            }
            else if (priority == winner && deny) denial = violation;
        }
        return denial;
    }

    public static String restrictFilter(PermissionContext context, String base)
    {
        if (base == null || base.trim().isEmpty()) throw new CustomException("检索权限条件缺失");
        Set<String> excluded = new LinkedHashSet<>();
        for (String level : LEVELS)
        {
            String reason = denialReason(context, level);
            if (reason == null) continue;
            if (!CONDITION_DENIALS.contains(reason)) throw new CustomException("访问策略不可用，请核对策略配置");
            excluded.add("\"" + level + "\"");
        }
        return excluded.isEmpty() ? base : "(" + base + ") and security_level not in [" + String.join(",", excluded) + "]";
    }

    private static boolean activePolicy(PolicyInfo policy)
    {
        return "0".equals(policy.getStatus())
                && !(isAllow(policy.getEffect()) && Integer.valueOf(0).equals(policy.getConditionVersion()));
    }

    private static String validate(PermissionContext context)
    {
        if (context == null || context.getPolicies() == null) return "POLICY_CONTEXT_UNAVAILABLE";
        for (PolicyInfo policy : context.getPolicies())
        {
            if (policy == null) return "INVALID_ACCESS_POLICY";
            if ("1".equals(policy.getStatus())) continue;
            if (!"0".equals(policy.getStatus())) return "INVALID_ACCESS_POLICY";
            if (isAllow(policy.getEffect()) && Integer.valueOf(0).equals(policy.getConditionVersion())) continue;
            if ((!Integer.valueOf(0).equals(policy.getConditionVersion()) && !Integer.valueOf(1).equals(policy.getConditionVersion()))
                    || policy.getPriority() == null || policy.getPriority() < 0 || policy.getPriority() > 9999)
                return "INVALID_ACCESS_POLICY";
            if (!supports(policy.getEffect(), policy.getResourceExpr(), policy.getEnvExpr())) return "UNSUPPORTED_ACCESS_POLICY";
        }
        return null;
    }

    private static Set<String> parseLevels(String expression)
    {
        if (expression == null || expression.trim().isEmpty()) return new LinkedHashSet<>(LEVELS);
        if (expression.length() > 500) return null;
        Matcher match = RESOURCE.matcher(expression.trim());
        if (!match.matches()) return null;
        Set<String> levels = new LinkedHashSet<>();
        for (String value : match.group(1).split(",")) if (!levels.add(value.trim())) return null;
        return levels;
    }

    private static Conditions parseConditions(String effect, String expression)
    {
        if (expression == null || expression.length() > 500) return null;
        boolean allow = isAllow(effect);
        String normalized = expression.trim();
        if (allow)
        {
            if (normalized.contains("||")) return null;
            if (normalized.matches("(?is).*\\b(?:time|ip)\\s+not\\s+in\\b.*")
                    || normalized.matches("(?is).*\\brisk\\s+above\\b.*")) return null;
            normalized = normalized.replace("&&", "||")
                    .replaceAll("(?i)\\btime\\s+in\\b", "time not in")
                    .replaceAll("(?i)\\bip\\s+in\\b", "ip not in")
                    .replaceAll("(?i)\\brisk\\s+at\\s+most\\b", "risk above");
        }
        else if (normalized.contains("&&")) return null;
        String[] clauses = normalized.split("\\s*\\|\\|\\s*", -1);
        if (clauses.length < 1 || clauses.length > 3) return null;
        Conditions result = new Conditions();
        for (String clause : clauses)
        {
            Matcher time = WINDOW.matcher(clause.trim());
            Matcher network = NETWORKS.matcher(clause.trim());
            Matcher risk = RISK.matcher(clause.trim());
            if (time.matches() && result.start == null)
            {
                try
                {
                    result.start = LocalTime.parse(time.group(1)); result.end = LocalTime.parse(time.group(2));
                    if (result.start.equals(result.end)) return null;
                }
                catch (DateTimeParseException exception) { return null; }
            }
            else if (network.matches() && result.networks == null)
            {
                String[] values = network.group(1).split(",", -1);
                if (values.length > 32) return null;
                result.networks = new ArrayList<>();
                for (String value : values)
                {
                    Network subnet = Network.parse(value.trim());
                    if (subnet == null) return null;
                    result.networks.add(subnet);
                }
            }
            else if (risk.matches() && result.maxRisk == null) result.maxRisk = risk.group(1).toUpperCase(Locale.ROOT);
            else return null;
        }
        return result;
    }

    private static final class Conditions
    {
        private LocalTime start;
        private LocalTime end;
        private List<Network> networks;
        private String maxRisk;

        private String denial(PermissionContext context)
        {
            if (start != null)
            {
                String value = context.getRequestTime();
                if (value == null || !value.matches("[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2}")) return "POLICY_TIME_CONTEXT_UNAVAILABLE";
                LocalTime now;
                try { now = LocalTime.parse(value.substring(11), DateTimeFormatter.ISO_LOCAL_TIME); }
                catch (DateTimeParseException exception) { return "POLICY_TIME_CONTEXT_UNAVAILABLE"; }
                boolean outside = start.isBefore(end) ? now.isBefore(start) || !now.isBefore(end) : now.isBefore(start) && !now.isBefore(end);
                if (outside) return "OUT_OF_POLICY_TIME_WINDOW";
            }
            if (networks != null)
            {
                byte[] address = Network.address(context.getRequestIp());
                boolean matched = false;
                if (address != null) for (Network subnet : networks) if (subnet.contains(address)) { matched = true; break; }
                if (!matched) return "OUT_OF_POLICY_IP_RANGE";
            }
            if (maxRisk != null)
            {
                int current = RISKS.indexOf(context.getUserRiskLevel());
                if (current < 0) return "POLICY_RISK_CONTEXT_UNAVAILABLE";
                if (current > RISKS.indexOf(maxRisk)) return "ABOVE_POLICY_RISK_LEVEL";
            }
            return null;
        }
    }

    private static final class Network
    {
        private final byte[] network;
        private final int prefix;
        private Network(byte[] network, int prefix) { this.network = network; this.prefix = prefix; }
        private static Network parse(String value)
        {
            String[] parts = value.split("/", -1);
            if (parts.length > 2) return null;
            byte[] address = address(parts[0]);
            if (address == null) return null;
            int prefix = address.length * 8;
            if (parts.length == 2)
            {
                if (!parts[1].matches("0|[1-9][0-9]{0,2}")) return null;
                prefix = Integer.parseInt(parts[1]);
                if (prefix > address.length * 8) return null;
            }
            return new Network(address, prefix);
        }
        private static byte[] address(String value)
        {
            if (value == null || value.isEmpty() || value.length() > 45) return null;
            if (value.indexOf(':') < 0)
            {
                String normalized = IpUtils.normalizeAddress(value);
                return normalized == null ? null : IpUtils.textToNumericFormatV4(normalized);
            }
            if (!value.matches("[0-9a-fA-F:.]+")) return null;
            try { return InetAddress.getByName(value).getAddress(); }
            catch (UnknownHostException exception) { return null; }
        }
        private boolean contains(byte[] address)
        {
            if (address.length != network.length) return false;
            for (int bit = 0; bit < prefix; bit++)
                if ((address[bit / 8] & (1 << (7 - bit % 8))) != (network[bit / 8] & (1 << (7 - bit % 8)))) return false;
            return true;
        }
    }
}
