-- 用户安全属性表
-- 用于 VACP 零信任安全向量检索中的用户密级、访问状态、访问时间窗口和风险属性管理

DROP TABLE IF EXISTS sys_user_security_attr;

CREATE TABLE sys_user_security_attr (
  id BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  user_id BIGINT(20) NOT NULL COMMENT '用户ID',
  user_name VARCHAR(64) DEFAULT '' COMMENT '用户名',
  nick_name VARCHAR(64) DEFAULT '' COMMENT '用户昵称',
  secret_level VARCHAR(32) DEFAULT 'PUBLIC' COMMENT '用户密级：PUBLIC公开 INTERNAL内部 SECRET秘密 CONFIDENTIAL机密',
  access_status VARCHAR(32) DEFAULT 'ACTIVE' COMMENT '访问状态：ACTIVE启用 DISABLED禁用 LOCKED锁定',
  access_start_time TIME DEFAULT NULL COMMENT '允许访问开始时间',
  access_end_time TIME DEFAULT NULL COMMENT '允许访问结束时间',
  risk_level VARCHAR(32) DEFAULT 'LOW' COMMENT '风险等级：LOW低 MEDIUM中 HIGH高',
  fail_count INT(11) DEFAULT 0 COMMENT '连续访问失败次数',
  last_access_ip VARCHAR(64) DEFAULT NULL COMMENT '最近访问IP',
  last_access_time DATETIME DEFAULT NULL COMMENT '最近访问时间',
  remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
  create_by VARCHAR(64) DEFAULT '' COMMENT '创建者',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by VARCHAR(64) DEFAULT '' COMMENT '更新者',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_id (user_id),
  KEY idx_secret_level (secret_level),
  KEY idx_access_status (access_status),
  KEY idx_risk_level (risk_level)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='用户安全属性表';

-- 菜单：用户安全属性
-- 注意：parent_id 这里先挂到系统管理下面，如果你后面想放到“权限治理”下面，可以再调整 parent_id
INSERT INTO sys_menu
(menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '用户安全属性', 1, 9, 'userSecurityAttr', 'system/userSecurityAttr/index', 1, 0, 'C', '0', '0', 'system:userSecurityAttr:list', 'peoples', 'admin', NOW(), '用户安全属性菜单'
WHERE NOT EXISTS (
  SELECT 1 FROM sys_menu WHERE perms = 'system:userSecurityAttr:list'
);

SET @parentId := (SELECT menu_id FROM sys_menu WHERE perms = 'system:userSecurityAttr:list' LIMIT 1);

INSERT INTO sys_menu
(menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '用户安全属性查询', @parentId, 1, '#', '', 1, 0, 'F', '0', '0', 'system:userSecurityAttr:query', '#', 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:userSecurityAttr:query');

INSERT INTO sys_menu
(menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '用户安全属性新增', @parentId, 2, '#', '', 1, 0, 'F', '0', '0', 'system:userSecurityAttr:add', '#', 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:userSecurityAttr:add');

INSERT INTO sys_menu
(menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '用户安全属性修改', @parentId, 3, '#', '', 1, 0, 'F', '0', '0', 'system:userSecurityAttr:edit', '#', 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:userSecurityAttr:edit');

INSERT INTO sys_menu
(menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '用户安全属性删除', @parentId, 4, '#', '', 1, 0, 'F', '0', '0', 'system:userSecurityAttr:remove', '#', 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:userSecurityAttr:remove');

INSERT INTO sys_menu
(menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '用户安全属性导出', @parentId, 5, '#', '', 1, 0, 'F', '0', '0', 'system:userSecurityAttr:export', '#', 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:userSecurityAttr:export');
