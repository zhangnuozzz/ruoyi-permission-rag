-- 第十六模块：VACP 菜单结构重构
-- 目标：把散落在“系统管理”和“安全中心”下的 RAG / 权限 / 审计功能统一整理到 VACP 安全向量平台下。

-- 1. 将原“安全中心”升级为项目一级菜单
UPDATE sys_menu
SET menu_name = 'VACP安全向量平台',
    parent_id = 0,
    order_num = 2,
    path = 'vacp',
    component = NULL,
    is_frame = 1,
    menu_type = 'M',
    visible = '0',
    status = '0',
    perms = '',
    icon = 'lock',
    remark = 'VACP安全向量访问控制平台'
WHERE menu_id = 2031;

-- 2. 隐藏若依原生非核心一级菜单，让侧边栏更聚焦
UPDATE sys_menu SET visible = '1' WHERE menu_id IN (2, 3, 4);

-- 3. 新增 VACP 四个二级目录
INSERT INTO sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, is_frame, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES
(2060, '安全检索中心', 2031, 1, 'secureSearch', NULL, 1, 'M', '0', '0', '', 'search', 'admin', NOW(), 'VACP安全检索中心'),
(2061, '身份与权限管理', 2031, 2, 'identityAccess', NULL, 1, 'M', '0', '0', '', 'peoples', 'admin', NOW(), 'VACP身份与权限管理'),
(2062, '审计与告警中心', 2031, 3, 'auditAlert', NULL, 1, 'M', '0', '0', '', 'monitor', 'admin', NOW(), 'VACP审计与告警中心'),
(2063, '安全治理中心', 2031, 4, 'securityGovernance', NULL, 1, 'M', '0', '0', '', 'lock', 'admin', NOW(), 'VACP安全治理中心')
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

-- 4. 安全检索中心
UPDATE sys_menu
SET parent_id = 2060,
    order_num = 1,
    menu_name = 'RAG安全检索',
    path = 'ragSearch',
    component = 'rag/search/index',
    icon = 'search'
WHERE menu_id = 2030;

UPDATE sys_menu
SET parent_id = 2060,
    order_num = 2,
    menu_name = 'RAG文件入库',
    path = 'ragFile',
    component = 'rag/file/index',
    icon = 'upload'
WHERE menu_id = 2041;

UPDATE sys_menu
SET parent_id = 2060,
    order_num = 3,
    menu_name = '文档权限标签',
    path = 'ragDoc',
    component = 'system/ragDoc/index',
    icon = 'documentation'
WHERE menu_id = 2024;

-- 5. 身份与权限管理
UPDATE sys_menu
SET parent_id = 2061,
    order_num = 1,
    menu_name = '用户安全属性',
    path = 'userSecurityAttr',
    component = 'system/userSecurityAttr/index',
    icon = 'peoples'
WHERE menu_id = 2050;

UPDATE sys_menu
SET parent_id = 2061,
    order_num = 2,
    menu_name = '用户组管理',
    path = 'group',
    component = 'system/group/index',
    icon = 'people'
WHERE menu_id = 2000;

UPDATE sys_menu
SET parent_id = 2061,
    order_num = 3,
    menu_name = '权限策略定义',
    path = 'policy',
    component = 'system/policy/index',
    icon = 'tree-table'
WHERE menu_id = 2006;

UPDATE sys_menu
SET parent_id = 2061,
    order_num = 4,
    menu_name = '策略绑定管理',
    path = 'policyBind',
    component = 'system/policyBind/index',
    icon = 'tree'
WHERE menu_id = 2012;

UPDATE sys_menu
SET parent_id = 2061,
    order_num = 5,
    menu_name = 'RAG权限上下文',
    path = 'permissionContext',
    component = 'rag/permissionContext/index',
    icon = 'tree'
WHERE menu_id = 2044;

-- 6. 审计与告警中心
UPDATE sys_menu
SET parent_id = 2062,
    order_num = 1,
    menu_name = 'RAG审计日志',
    path = 'ragAuditLog',
    component = 'rag/auditLog/index',
    icon = 'log'
WHERE menu_id = 2018;

UPDATE sys_menu
SET parent_id = 2062,
    order_num = 2,
    menu_name = 'RAG行为告警',
    path = 'behaviorAlert',
    component = 'rag/behaviorAlert/index',
    icon = 'warning'
WHERE menu_id = 2046;

UPDATE sys_menu
SET parent_id = 2062,
    order_num = 3,
    menu_name = '访问监控',
    path = 'accessLog',
    component = 'system/accessLog/index',
    icon = 'monitor'
WHERE menu_id = 2032;

-- 7. 安全治理中心
UPDATE sys_menu
SET parent_id = 2063,
    order_num = 1,
    menu_name = 'IP黑名单',
    path = 'ipBlacklist',
    component = 'system/ipBlacklist/index',
    icon = 'lock'
WHERE menu_id = 2036;

-- 8. 整理系统管理，只保留若依基础后台管理
UPDATE sys_menu SET order_num = 1 WHERE menu_id = 100;
UPDATE sys_menu SET order_num = 2 WHERE menu_id = 101;
UPDATE sys_menu SET order_num = 3 WHERE menu_id = 102;
UPDATE sys_menu SET order_num = 4 WHERE menu_id = 103;
UPDATE sys_menu SET order_num = 5 WHERE menu_id = 104;
UPDATE sys_menu SET order_num = 6 WHERE menu_id = 105;
UPDATE sys_menu SET order_num = 7 WHERE menu_id = 106;
UPDATE sys_menu SET order_num = 8 WHERE menu_id = 107;
UPDATE sys_menu SET order_num = 9 WHERE menu_id = 108;
