-- 行为告警增强：同一审计日志、同一告警类型只生成一次

ALTER TABLE sys_rag_behavior_alert
ADD UNIQUE KEY IF NOT EXISTS uk_source_log_alert_type (source_log_id, alert_type);
