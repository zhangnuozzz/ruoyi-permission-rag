package com.ruoyi.system.service;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 首页态势感知统计。所有数值直接来自平台业务表，不使用演示数据。
 */
@Service
public class SituationOverviewService
{
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Map<String, Object> getOverview()
    {
        LocalDateTime generatedAt = LocalDateTime.now();
        LocalDate today = generatedAt.toLocalDate();
        Timestamp todayStart = Timestamp.valueOf(today.atStartOfDay());
        Timestamp tomorrowStart = Timestamp.valueOf(today.plusDays(1).atStartOfDay());
        Timestamp weekStart = Timestamp.valueOf(today.minusDays(6).atStartOfDay());

        // 一次聚合生成请求及决策计数，避免刷新期间新增记录造成分项总数不一致。
        Map<String, Object> audit = jdbcTemplate.queryForMap(
                "select count(*) as requests, " +
                "coalesce(sum(case when allow_access = '1' then 1 else 0 end), 0) as allowed, " +
                "coalesce(sum(case when allow_access = '0' then 1 else 0 end), 0) as denied, " +
                "coalesce(sum(case when create_time >= ? and create_time < ? then 1 else 0 end), 0) as todayRequests, " +
                "coalesce(sum(case when create_time >= ? and create_time < ? and allow_access = '0' " +
                "then 1 else 0 end), 0) as todayDenied from sys_rag_audit_log",
                todayStart, tomorrowStart, todayStart, tomorrowStart);
        long requests = number(audit.get("requests"));
        long allowed = number(audit.get("allowed"));
        long denied = number(audit.get("denied"));

        Map<String, Object> totals = new LinkedHashMap<>();
        totals.put("users", count("select count(*) from sys_user where del_flag = '0'"));
        totals.put("documents", count("select count(*) from sys_rag_doc where del_flag = '0'"));
        totals.put("requests", requests);
        totals.put("pendingAlerts", count("select count(*) from sys_rag_behavior_alert where status = 'unhandled'"));
        totals.put("todayRequests", number(audit.get("todayRequests")));
        totals.put("todayDenied", number(audit.get("todayDenied")));

        Map<String, Object> decisions = new LinkedHashMap<>();
        decisions.put("allowed", allowed);
        decisions.put("denied", denied);
        decisions.put("undetermined", requests - allowed - denied);

        List<Map<String, Object>> dailyRows = jdbcTemplate.queryForList(
                "select date_format(create_time, '%Y-%m-%d') as day, count(*) as total, " +
                "sum(case when allow_access = '0' then 1 else 0 end) as denied " +
                "from sys_rag_audit_log where create_time >= ? and create_time < ? " +
                "group by date_format(create_time, '%Y-%m-%d') order by day", weekStart, tomorrowStart);
        Map<String, Map<String, Object>> dailyLookup = new HashMap<>();
        for (Map<String, Object> row : dailyRows)
        {
            dailyLookup.put(String.valueOf(row.get("day")), row);
        }
        List<Map<String, Object>> trend = new ArrayList<>();
        for (int offset = 6; offset >= 0; offset--)
        {
            String day = today.minusDays(offset).toString();
            Map<String, Object> source = dailyLookup.get(day);
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("date", day);
            point.put("requests", source == null ? 0L : number(source.get("total")));
            point.put("denied", source == null ? 0L : number(source.get("denied")));
            trend.add(point);
        }

        List<Map<String, Object>> levels = jdbcTemplate.queryForList(
                "select coalesce(nullif(security_level, ''), 'UNKNOWN') as level, count(*) as total " +
                "from sys_rag_doc where del_flag = '0' " +
                "group by coalesce(nullif(security_level, ''), 'UNKNOWN') order by total desc, level asc");

        List<Map<String, Object>> alerts = jdbcTemplate.queryForList(
                "select id, user_name as userName, alert_type as alertType, alert_level as alertLevel, " +
                "date_format(create_time, '%Y-%m-%d %H:%i') as createTime " +
                "from sys_rag_behavior_alert where status = 'unhandled' " +
                "order by create_time desc, id desc limit 5");

        List<Map<String, Object>> recentRequests = jdbcTemplate.queryForList(
                "select id, user_name as userName, allow_access as allowAccess, " +
                "coalesce(passed_count, 0) as passedCount, coalesce(blocked_count, 0) as blockedCount, " +
                "cost_time as costTime, date_format(create_time, '%Y-%m-%d %H:%i') as createTime " +
                "from sys_rag_audit_log order by create_time desc, id desc limit 6");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("generatedAt", generatedAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        result.put("totals", totals);
        result.put("decisions", decisions);
        result.put("trend", trend);
        result.put("documentLevels", levels);
        result.put("pendingAlerts", alerts);
        result.put("recentRequests", recentRequests);
        return result;
    }

    private long count(String sql, Object... args)
    {
        Long value = jdbcTemplate.queryForObject(sql, args, Long.class);
        return value == null ? 0L : value;
    }

    private long number(Object value)
    {
        return value instanceof Number ? ((Number) value).longValue() : 0L;
    }
}
