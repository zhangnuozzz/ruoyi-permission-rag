ALTER TABLE sys_rag_behavior_alert
ADD COLUMN IF NOT EXISTS handled_by VARCHAR(64) DEFAULT NULL COMMENT '处理人'
AFTER status;

ALTER TABLE sys_rag_behavior_alert
ADD COLUMN IF NOT EXISTS handled_time DATETIME DEFAULT NULL COMMENT '处理时间'
AFTER handled_by;

ALTER TABLE sys_rag_behavior_alert
ADD COLUMN IF NOT EXISTS handle_remark VARCHAR(1000) DEFAULT NULL COMMENT '处理说明'
AFTER handled_time;
