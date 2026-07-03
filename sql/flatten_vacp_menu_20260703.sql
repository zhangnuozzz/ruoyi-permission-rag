-- 第十六模块修正版：VACP 菜单改为两级结构
-- 原因：RuoYi-Vue 3.2.0 原版侧边栏对三级菜单支持不够友好，容易出现菜单截断和页面嵌套。

-- 1. 保留一级菜单：VACP安全向量平台
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

-- 2. 隐藏刚才新增的四个三级目录，避免侧边栏出现三级嵌套
UPDATE sys_menu
SET visible = '1',
    status = '1'
WHERE menu_id IN (2060, 2061, 2062, 2063);

-- 3. 核心功能全部直接挂到 VACP 一级菜单下面

-- 安全检索类
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

-- 身份与权限类
UPDATE sys_menu
SET parent_id = 2031,
    order_num = 4,
    menu_name = '用户安全属性',
    path = 'userSecurityAttr',
    component = 'system/userSecurityAttr/index',
    icon = 'peoples',
    visible = '0',
    status = '0'
WHERE menu_id = 2050;

UPDATE sys_menu
SET parent_id = 2031,
    order_num = 5,
    menu_name = '用户组管理',
    path = 'group',
    component = 'system/group/index',
    icon = 'people',
    visible = '0',
    status = '0'
WHERE menu_id = 2000;

UPDATE sys_menu
SET parent_id = 2031,
    order_num = 6,
    menu_name = '权限策略定义',
    path = 'policy',
    component = 'system/policy/index',
    icon = 'tree-table',
    visible = '0',
    status = '0'
WHERE menu_id = 2006;

UPDATE sys_menu
SET parent_id = 2031,
    order_num = 7,
    menu_name = '策略绑定管理',
    path = 'policyBind',
    component = 'system/policyBind/index',
    icon = 'tree',
    visible = '0',
    status = '0'
WHERE menu_id = 2012;

UPDATE sys_menu
SET parent_id = 2031,
    order_num = 8,
    menu_name = '权限上下文',
    path = 'permissionContext',
    component = 'rag/permissionContext/index',
    icon = 'tree',
    visible = '0',
    status = '0'
WHERE menu_id = 2044;

-- 审计与告警类
UPDATE sys_menu
SET parent_id = 2031,
    order_num = 9,
    menu_name = 'RAG审计日志',
    path = 'ragAuditLog',
    component = 'rag/auditLog/index',
    icon = 'log',
    visible = '0',
    status = '0'
WHERE menu_id = 2018;

UPDATE sys_menu
SET parent_id = 2031,
    order_num = 10,
    menu_name = 'RAG行为告警',
    path = 'behaviorAlert',
    component = 'rag/behaviorAlert/index',
    icon = 'warning',
    visible = '0',
    status = '0'
WHERE menu_id = 2046;

UPDATE sys_menu
SET parent_id = 2031,
    order_num = 11,
    menu_name = '访问监控',
    path = 'accessLog',
    component = 'system/accessLog/index',
    icon = 'monitor',
    visible = '0',
    status = '0'
WHERE menu_id = 2032;

UPDATE sys_menu
SET parent_id = 2031,
    order_num = 12,
    menu_name = 'IP黑名单',
    path = 'ipBlacklist',
    component = 'system/ipBlacklist/index',
    icon = 'lock',
    visible = '0',
    status = '0'
WHERE menu_id = 2036;

-- 4. 系统管理恢复为若依基础管理
UPDATE sys_menu SET order_num = 1 WHERE menu_id = 100;
UPDATE sys_menu SET order_num = 2 WHERE menu_id = 101;
UPDATE sys_menu SET order_num = 3 WHERE menu_id = 102;
UPDATE sys_menu SET order_num = 4 WHERE menu_id = 103;
UPDATE sys_menu SET order_num = 5 WHERE menu_id = 104;
UPDATE sys_menu SET order_num = 6 WHERE menu_id = 105;
UPDATE sys_menu SET order_num = 7 WHERE menu_id = 106;
UPDATE sys_menu SET order_num = 8 WHERE menu_id = 107;
UPDATE sys_menu SET order_num = 9 WHERE menu_id = 108;

-- 5. 继续隐藏若依非核心一级菜单
UPDATE sys_menu SET visible = '1' WHERE menu_id IN (2, 3, 4);
