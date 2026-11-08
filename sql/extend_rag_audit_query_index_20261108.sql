-- 覆盖接口允许的完整查询长度，保留原有文本比较规则。
-- 升级前核对同名索引为 (user_id, query_text(128), create_time)。
ALTER TABLE sys_rag_audit_log
    DROP INDEX idx_rag_audit_user_query_time,
    ADD INDEX idx_rag_audit_user_query_time (user_id, query_text(500), create_time),
    ALGORITHM=INPLACE, LOCK=NONE;
