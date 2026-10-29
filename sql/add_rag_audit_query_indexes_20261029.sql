-- MariaDB 10.11 / InnoDB.
-- Guard index names and definitions before applying to an existing database.
CREATE INDEX IF NOT EXISTS idx_rag_audit_user_time
    ON sys_rag_audit_log (user_id, create_time);
CREATE INDEX IF NOT EXISTS idx_rag_audit_user_query_time
    ON sys_rag_audit_log (user_id, query_text(128), create_time);
