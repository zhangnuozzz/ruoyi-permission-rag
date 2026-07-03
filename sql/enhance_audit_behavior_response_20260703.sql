-- VACP 审计与行为分析增强：
-- 1. 防止同一条审计日志、同一种告警类型重复生成；
-- 2. 为处理状态预留更多动作：handled / ignored / blocked / limited。

ALTER TABLE sys_rag_behavior_alert
ADD UNIQUE INDEX IF NOT EXISTS uk_rag_alert_log_type(source_log_id, alert_type);

UPDATE sys_rag_behavior_alert
SET status = 'unhandled'
WHERE status IS NULL OR status = '';
