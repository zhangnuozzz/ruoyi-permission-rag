-- ============================================================
-- VACP 权限策略版本管理
-- 每次策略新增/修改时保存完整快照
-- ============================================================

CREATE TABLE IF NOT EXISTS sys_policy_version (
    id BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '版本记录ID',
    policy_id BIGINT(20) NOT NULL COMMENT '策略ID',
    version_no INT NOT NULL COMMENT '版本号',
    policy_code VARCHAR(64) NOT NULL COMMENT '策略编码',
    policy_name VARCHAR(128) NOT NULL COMMENT '策略名称',
    effect VARCHAR(16) DEFAULT 'ALLOW' COMMENT '策略效果',
    subject_type VARCHAR(32) DEFAULT NULL COMMENT '主体类型',
    subject_expr VARCHAR(1000) DEFAULT NULL COMMENT '主体表达式',
    resource_expr VARCHAR(1000) DEFAULT NULL COMMENT '资源表达式',
    env_expr VARCHAR(1000) DEFAULT NULL COMMENT '环境表达式',
    priority INT DEFAULT 100 COMMENT '优先级',
    status CHAR(1) DEFAULT '0' COMMENT '状态',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    change_type VARCHAR(32) NOT NULL COMMENT '变更类型：BASELINE/CREATE/UPDATE',
    change_by VARCHAR(64) DEFAULT '' COMMENT '变更人',
    change_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '变更时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_policy_version (policy_id, version_no),
    KEY idx_policy_version_policy (policy_id),
    KEY idx_policy_version_time (change_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限策略版本历史表';

-- ============================================================
-- 为已经存在的策略建立 V1 基线版本
-- 重复执行不会重复生成
-- ============================================================

INSERT INTO sys_policy_version
(
    policy_id,
    version_no,
    policy_code,
    policy_name,
    effect,
    subject_type,
    subject_expr,
    resource_expr,
    env_expr,
    priority,
    status,
    remark,
    change_type,
    change_by,
    change_time
)
SELECT
    p.id,
    1,
    p.policy_code,
    p.policy_name,
    p.effect,
    p.subject_type,
    p.subject_expr,
    p.resource_expr,
    p.env_expr,
    p.priority,
    p.status,
    p.remark,
    'BASELINE',
    IFNULL(NULLIF(p.update_by, ''), IFNULL(NULLIF(p.create_by, ''), 'system')),
    IFNULL(p.update_time, IFNULL(p.create_time, NOW()))
FROM sys_policy p
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_policy_version v
    WHERE v.policy_id = p.id
);
