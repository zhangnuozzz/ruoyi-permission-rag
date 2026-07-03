-- RAG 安全审计日志增强
-- 增加风险分数、受限访问、通过数量、拦截数量、拦截原因、安全查询上下文

ALTER TABLE sys_rag_audit_log
ADD COLUMN IF NOT EXISTS risk_score INT DEFAULT 0 COMMENT '风险分数'
AFTER deny_reasons;

ALTER TABLE sys_rag_audit_log
ADD COLUMN IF NOT EXISTS limited_query CHAR(1) DEFAULT '0' COMMENT '是否受限查询：0否 1是'
AFTER risk_score;

ALTER TABLE sys_rag_audit_log
ADD COLUMN IF NOT EXISTS passed_count INT DEFAULT 0 COMMENT '通过结果数量'
AFTER limited_query;

ALTER TABLE sys_rag_audit_log
ADD COLUMN IF NOT EXISTS blocked_count INT DEFAULT 0 COMMENT '拦截结果数量'
AFTER passed_count;

ALTER TABLE sys_rag_audit_log
ADD COLUMN IF NOT EXISTS blocked_reasons VARCHAR(1000) DEFAULT NULL COMMENT '拦截原因汇总'
AFTER blocked_count;

ALTER TABLE sys_rag_audit_log
ADD COLUMN IF NOT EXISTS secure_context_json LONGTEXT COMMENT '查询安全上下文JSON'
AFTER blocked_reasons;
