-- ============================================================
-- VACP 第三轮：BBAC 行为访问控制运行字段
-- 对齐原型文档：
-- 1. 校验来源 IP，黑白名单机制
-- 2. 单位时间访问次数 <= 阈值
-- 3. 连续访问失败次数 >= N -> 临时封禁
-- 4. 重复 query pattern -> 限制访问
-- ============================================================

ALTER TABLE sys_user_security_attr
ADD COLUMN IF NOT EXISTS lock_until DATETIME DEFAULT NULL COMMENT '临时封禁截止时间'
AFTER fail_count;

ALTER TABLE sys_user_security_attr
ADD COLUMN IF NOT EXISTS lock_reason VARCHAR(500) DEFAULT NULL COMMENT '临时封禁原因'
AFTER lock_until;

UPDATE sys_user_security_attr
SET lock_until = NULL,
    lock_reason = NULL
WHERE access_status = 'ACTIVE';
