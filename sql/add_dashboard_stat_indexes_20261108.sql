ALTER TABLE sys_rag_audit_log
    ADD INDEX idx_rag_audit_time_decision (create_time, allow_access),
    ALGORITHM=INPLACE, LOCK=NONE;

ALTER TABLE sys_rag_behavior_alert
    ADD INDEX idx_rag_alert_status_time (status, create_time, id),
    ALGORITHM=INPLACE, LOCK=NONE;
