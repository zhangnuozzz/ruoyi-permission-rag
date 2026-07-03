-- 第十六模块最终版：VACP 菜单拆分为四个一级中心
-- 原因：RuoYi-Vue 3.2.0 三级菜单体验差；单个 VACP 一级菜单又太空。
-- 方案：安全检索中心、身份权限中心、审计告警中心、安全治理中心作为一级菜单。

-- 1. 原 2031 改为“安全检索中心”
UPDATE sys_menu
SET menu_name = '安全检索中心',
    parent_id = 0,
    order_num = 2,
    path = 'secureSearch',
    component = NULL,
    is_frame = 1,
    menu_type = 'M',
    visible = '0',
    status = '0',
    perms = '',
    icon = 'search',
    remark = 'VACP安全检索中心'
WHERE menu_id = 2031;

-- 2. 新增三个一级中心
INSERT INTO sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, is_frame, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES
(2061, '身份权限中心', 0, 3, 'identityAccess', NULL, 1, 'M', '0', '0', '', 'peoples', 'admin', NOW(), 'VACP身份与权限管理中心'),
(2062, '审计告警中心', 0, 4, 'auditAlert', NULL, 1, 'M', '0', '0', '', 'monitor', 'admin', NOW(), 'VACP审计与告警中心'),
(2063, '安全治理中心', 0, 5, 'securityGovernance', NULL, 1, 'M', '0', '0', '', 'lock', 'admin', NOW(), 'VACP安全治理中心')
ON DUPLICATE KEY UPDATE
menu_name = VALUES(menu_name),
parent_id = VALUES(parent_id),
order_num = VALUES(order_num),
path = VALUES(path),
component = VALUES(component),
is_frame = VALUES(is_frame),
menu_type = VALUES(menu_type),
visible = VALUES(visible),
status = VALUES(status),
perms = VALUES(perms),
icon = VALUES(icon),
remark = VALUES(remark);

-- 3. 如果之前建过 2060，把它隐藏掉
UPDATE sys_menu
SET visible = '1',
    status = '1'
WHERE menu_id = 2060;

-- 4. 安全检索中心子菜单
UPDATE sys_menu
SET parent_id = 2031,
    order_num = 1,
    menu_name = 'RAG安全检索',
    path = 'ragSearch',
    component = 'rag/search/index',
    icon = 'search',
    visible = '0',
    status = '0'
WHERE menu_id = 2030;

UPDATE sys_menu
SET parent_id = 2031,
    order_num = 2,
    menu_name = 'RAG文件入库',
    path = 'ragFile',
    component = 'rag/file/index',
    icon = 'upload',
    visible = '0',
    status = '0'
WHERE menu_id = 2041;

UPDATE sys_menu
SET parent_id = 2031,
    order_num = 3,
    menu_name = '文档权限标签',
    path = 'ragDoc',
    component = 'system/ragDoc/index',
    icon = 'documentation',
    visible = '0',
    status = '0'
WHERE menu_id = 2024;

-- 5. 身份权限中心子菜单
UPDATE sys_menu
SET parent_id = 2061,
    order_num = 1,
    menu_name = '用户安全属性',
    path = 'userSecurityAttr',
    component = 'system/userSecurityAttr/index',
    icon = 'peoples',
    visible = '0',
    status = '0'
WHERE menu_id = 2050;

UPDATE sys_menu
SET parent_id = 2061,
    order_num = 2,
    menu_name = '用户组管理',
    path = 'group',
    component = 'system/group/index',
    icon = 'people',
    visible = '0',
    status = '0'
WHERE menu_id = 2000;

UPDATE sys_menu
SET parent_id = 2061,
    order_num = 3,
    menu_name = '权限策略定义',
    path = 'policy',
    component = 'system/policy/index',
    icon = 'tree-table',
    visible = '0',
    status = '0'
WHERE menu_id = 2006;

UPDATE sys_menu
SET parent_id = 2061,
    order_num = 4,
    menu_name = '策略绑定管理',
    path = 'policyBind',
    component = 'system/policyBind/index',
    icon = 'tree',
    visible = '0',
    status = '0'
WHERE menu_id = 2012;

UPDATE sys_menu
SET parent_id = 2061,
    order_num = 5,
    menu_name = '权限上下文',
    path = 'permissionContext',
    component = 'rag/permissionContext/index',
    icon = 'tree',
    visible = '0',
    status = '0'
WHERE menu_id = 2044;

-- 6. 审计告警中心子菜单
UPDATE sys_menu
SET parent_id = 2062,
    order_num = 1,
    menu_name = 'RAG审计日志',
    path = 'ragAuditLog',
    component = 'rag/auditLog/index',
    icon = 'log',
    visible = '0',
    status = '0'
WHERE menu_id = 2018;

UPDATE sys_menu
SET parent_id = 2062,
    order_num = 2,
    menu_name = 'RAG行为告警',
    path = 'behaviorAlert',
    component = 'rag/behaviorAlert/index',
    icon = 'warning',
    visible = '0',
    status = '0'
WHERE menu_id = 2046;

UPDATE sys_menu
SET parent_id = 2062,
    order_num = 3,
    menu_name = '访问监控',
    path = 'accessLog',
    component = 'system/accessLog/index',
    icon = 'monitor',
    visible = '0',
    status = '0'
WHERE menu_id = 2032;

-- 7. 安全治理中心子菜单
UPDATE sys_menu
SET parent_id = 2063,
    order_num = 1,
    menu_name = 'IP黑名单',
    path = 'ipBlacklist',
    component = 'system/ipBlacklist/index',
    icon = 'lock',
    visible = '0',
    status = '0'
WHERE menu_id = 2036;

-- 8. 系统管理作为基础后台管理，排到后面
UPDATE sys_menu
SET order_num = 6,
    visible = '0',
    status = '0'
WHERE menu_id = 1;

UPDATE sys_menu SET order_num = 1 WHERE menu_id = 100;
UPDATE sys_menu SET order_num = 2 WHERE menu_id = 101;
UPDATE sys_menu SET order_num = 3 WHERE menu_id = 102;
UPDATE sys_menu SET order_num = 4 WHERE menu_id = 103;
UPDATE sys_menu SET order_num = 5 WHERE menu_id = 104;
UPDATE sys_menu SET order_num = 6 WHERE menu_id = 105;
UPDATE sys_menu SET order_num = 7 WHERE menu_id = 106;
UPDATE sys_menu SET order_num = 8 WHERE menu_id = 107;
UPDATE sys_menu SET order_num = 9 WHERE menu_id = 108;

-- 9. 隐藏若依非核心一级菜单
UPDATE sys_menu SET visible = '1' WHERE menu_id IN (2, 3, 4);
