-- 只登记独立导出按钮权限，不自动分配角色，不改变人员或文档访问范围。
INSERT INTO sys_menu
    (menu_name, parent_id, order_num, path, is_frame, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '访问监控导出', parent.menu_id, 99, '#', 1, 'F', '0', '0', 'system:accessLog:export', '#', 'system', NOW(), ''
FROM sys_menu parent
WHERE parent.perms = 'system:accessLog:list' AND parent.menu_type = 'C'
  AND NOT EXISTS (SELECT 1 FROM sys_menu existing WHERE existing.perms = 'system:accessLog:export');

INSERT INTO sys_menu
    (menu_name, parent_id, order_num, path, is_frame, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 'IP黑名单导出', parent.menu_id, 99, '#', 1, 'F', '0', '0', 'system:ipBlacklist:export', '#', 'system', NOW(), ''
FROM sys_menu parent
WHERE parent.perms = 'system:ipBlacklist:list' AND parent.menu_type = 'C'
  AND NOT EXISTS (SELECT 1 FROM sys_menu existing WHERE existing.perms = 'system:ipBlacklist:export');

INSERT INTO sys_menu
    (menu_name, parent_id, order_num, path, is_frame, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 'IP白名单导出', parent.menu_id, 99, '#', 1, 'F', '0', '0', 'system:ipWhitelist:export', '#', 'system', NOW(), ''
FROM sys_menu parent
WHERE parent.perms = 'system:ipWhitelist:list' AND parent.menu_type = 'C'
  AND NOT EXISTS (SELECT 1 FROM sys_menu existing WHERE existing.perms = 'system:ipWhitelist:export');
