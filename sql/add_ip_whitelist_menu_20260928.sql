-- ============================================================
-- VACP IP 白名单管理菜单
-- 安全治理中心 -> IP白名单
-- ============================================================

INSERT INTO sys_menu
(
    menu_id,
    menu_name,
    parent_id,
    order_num,
    path,
    component,
    is_frame,
    menu_type,
    visible,
    status,
    perms,
    icon,
    create_by,
    create_time,
    remark
)
VALUES
(
    2070,
    'IP白名单',
    2063,
    2,
    'ipWhitelist',
    'system/ipWhitelist/index',
    1,
    'C',
    '0',
    '0',
    'system:ipWhitelist:list',
    'lock',
    'admin',
    NOW(),
    'VACP可信IP白名单管理'
)
ON DUPLICATE KEY UPDATE
menu_name = VALUES(menu_name),
parent_id = VALUES(parent_id),
order_num = VALUES(order_num),
path = VALUES(path),
component = VALUES(component),
visible = VALUES(visible),
status = VALUES(status),
perms = VALUES(perms),
remark = VALUES(remark);

-- 查询
INSERT INTO sys_menu
(menu_id, menu_name, parent_id, order_num, path, component,
 is_frame, menu_type, visible, status, perms, icon,
 create_by, create_time, remark)
VALUES
(2071, 'IP白名单查询', 2070, 1, '#', NULL,
 1, 'F', '0', '0', 'system:ipWhitelist:query', '#',
 'admin', NOW(), '');

-- 新增
INSERT INTO sys_menu
(menu_id, menu_name, parent_id, order_num, path, component,
 is_frame, menu_type, visible, status, perms, icon,
 create_by, create_time, remark)
VALUES
(2072, 'IP白名单新增', 2070, 2, '#', NULL,
 1, 'F', '0', '0', 'system:ipWhitelist:add', '#',
 'admin', NOW(), '');

-- 修改
INSERT INTO sys_menu
(menu_id, menu_name, parent_id, order_num, path, component,
 is_frame, menu_type, visible, status, perms, icon,
 create_by, create_time, remark)
VALUES
(2073, 'IP白名单修改', 2070, 3, '#', NULL,
 1, 'F', '0', '0', 'system:ipWhitelist:edit', '#',
 'admin', NOW(), '');

-- 删除
INSERT INTO sys_menu
(menu_id, menu_name, parent_id, order_num, path, component,
 is_frame, menu_type, visible, status, perms, icon,
 create_by, create_time, remark)
VALUES
(2074, 'IP白名单删除', 2070, 4, '#', NULL,
 1, 'F', '0', '0', 'system:ipWhitelist:remove', '#',
 'admin', NOW(), '');
