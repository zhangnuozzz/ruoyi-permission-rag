package com.ruoyi.system.domain.rag;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Supplier;
import com.ruoyi.system.domain.SysRagAuditLog;

/** 检索审计的单行 Syslog 摘要。 */
public final class AuditSyslogMessage
{
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter
            .ofPattern("MMM dd HH:mm:ss", Locale.ENGLISH).withZone(ZoneId.systemDefault());

    private AuditSyslogMessage() {}

    public static String format(RagAuditLog audit, boolean includeQuery)
    {
        return snapshot(audit, QueryExecutionStatus.from(audit.getAllowAccess(), audit.getResponseJson()), includeQuery).get();
    }

    public static String format(SysRagAuditLog audit, boolean includeQuery)
    {
        return build(audit.getId(), audit.getUserId(), audit.getUserName(), audit.getAllowAccess(),
                QueryExecutionStatus.from(audit.getAllowAccess(), audit.getResponseJson()),
                audit.getPassedCount(), audit.getBlockedCount(), audit.getRiskScore(), audit.getCostTime(),
                millis(audit.getCreateTime()), audit.getQueryText(), includeQuery, false);
    }

    /** 只捕获有界字段和不可变执行状态，不将完整响应或可变业务对象放入队列。 */
    public static Supplier<String> snapshot(RagAuditLog audit, QueryExecutionStatus execution, boolean includeQuery)
    {
        final Long id = audit.getId(), userId = audit.getUserId();
        requireId(id);
        final String name = prefix(audit.getUserName(), 64), access = audit.getAllowAccess();
        final QueryExecutionStatus status = Objects.requireNonNull(execution, "Execution status is required");
        final Integer passed = audit.getPassedCount(), blocked = audit.getBlockedCount(), risk = audit.getRiskScore();
        final Long cost = audit.getCostTime(), occurredAt = millis(audit.getCreateTime());
        final String query = includeQuery ? prefix(audit.getQueryText(), 128) : null;
        final boolean truncated = includeQuery && audit.getQueryText() != null
                && audit.getQueryText().codePointCount(0, audit.getQueryText().length()) > 128;
        return () -> build(id, userId, name, access, status, passed, blocked, risk, cost,
                occurredAt, query, includeQuery, truncated);
    }

    private static String build(Long id, Long userId, String userName, String allowAccess,
            QueryExecutionStatus execution, Integer passed, Integer blocked, Integer risk, Long cost,
            Long occurredAt, String query, boolean includeQuery, boolean truncated)
    {
        requireId(id);
        int severity = execution == QueryExecutionStatus.FAILED ? 3 : "0".equals(allowAccess) ? 4 : 6;
        String timestamp = TIMESTAMP.format(occurredAt == null ? Instant.now() : Instant.ofEpochMilli(occurredAt));
        String base = "<" + (16 * 8 + severity) + ">" + timestamp + " vacp-platform VACP-RAG-REQUEST:"
                + " audit_id=" + id + " user_id=" + userId + " user_name=\"" + escape(userName, 64) + "\""
                + " allow_access=" + ("1".equals(allowAccess) ? "1" : "0".equals(allowAccess) ? "0" : "UNKNOWN")
                + " execution_status=" + execution.name() + " passed_count=" + passed + " blocked_count=" + blocked
                + " risk_score=" + risk + " cost_time=" + cost + (occurredAt == null ? " timestamp_missing=true" : "");
        if (!includeQuery) return base;
        String details = base + " query=\"" + escape(query, 128) + "\"";
        if (truncated || (query != null && query.codePointCount(0, query.length()) > 128)) details += " query_truncated=true";
        return details.getBytes(StandardCharsets.UTF_8).length <= 1024 ? details : base + " query_omitted=true";
    }

    private static void requireId(Long id)
    {
        if (id == null || id <= 0) throw new IllegalArgumentException("A persisted audit identifier is required");
    }

    private static Long millis(Date date) { return date == null ? null : date.getTime(); }

    private static String prefix(String value, int maximum)
    {
        if (value == null) return null;
        return value.substring(0, value.offsetByCodePoints(0, Math.min(maximum, value.codePointCount(0, value.length()))));
    }

    private static String escape(String value, int limit)
    {
        String text = prefix(value, limit);
        return text == null ? "" : text.replaceAll("[\\p{Cntrl}\\u2028\\u2029]", " ")
                .replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
