-- 保留既有规则格式标记为0；新增与重新保存的规则使用版本1。
-- 应用脚本逐列核验是否已存在，已有列不会重复执行。
ALTER TABLE sys_policy ADD COLUMN condition_version TINYINT NOT NULL DEFAULT 0 AFTER env_expr;
ALTER TABLE sys_policy_version ADD COLUMN condition_version TINYINT NOT NULL DEFAULT 0 AFTER env_expr;
