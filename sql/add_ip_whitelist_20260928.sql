-- ============================================================
-- VACP BBAC：IP 白名单
-- 对齐原型文档中的 IP 黑白名单机制
-- ============================================================

CREATE TABLE IF NOT EXISTS sys_ip_whitelist (
    whitelist_id BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '白名单ID',
    ipaddr VARCHAR(128) NOT NULL COMMENT 'IP地址',
    reason VARCHAR(500) DEFAULT NULL COMMENT '放行原因',
    status CHAR(1) DEFAULT '0' COMMENT '状态：0启用 1停用',
    create_by VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME DEFAULT NULL COMMENT '更新时间',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (whitelist_id),
    UNIQUE KEY uk_ip_whitelist_ipaddr (ipaddr),
    KEY idx_ip_whitelist_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='IP白名单表';
