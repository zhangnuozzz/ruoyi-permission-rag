-- 为 RAG 检索审计日志增加 JSON 可追溯字段
-- 用于满足甲方“审计日志可以查看 JSON 内容”的要求。

ALTER TABLE sys_rag_audit_log
ADD COLUMN IF NOT EXISTS user_context_json LONGTEXT NULL COMMENT '用户权限上下文JSON' AFTER scope_codes,
ADD COLUMN IF NOT EXISTS request_json LONGTEXT NULL COMMENT 'RAG检索请求JSON' AFTER metadata_filter,
ADD COLUMN IF NOT EXISTS raw_results_json LONGTEXT NULL COMMENT 'RAG Server原始候选结果JSON' AFTER request_json,
ADD COLUMN IF NOT EXISTS passed_results_json LONGTEXT NULL COMMENT '二次过滤通过结果JSON' AFTER raw_results_json,
ADD COLUMN IF NOT EXISTS blocked_results_json LONGTEXT NULL COMMENT '二次过滤拦截结果JSON' AFTER passed_results_json,
ADD COLUMN IF NOT EXISTS response_json LONGTEXT NULL COMMENT '最终返回前端响应JSON' AFTER blocked_results_json;
