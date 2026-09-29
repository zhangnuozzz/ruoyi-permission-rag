USE `ry-vue-320`;

-- ==========================================================
-- 身份权限中心
--
-- 用户权限配置
-- 用户组管理
-- 访问策略管理
-- 权限上下文
-- ==========================================================

-- 用户安全属性 -> 用户权限配置
UPDATE sys_menu
SET
    menu_name = '用户权限配置',
    parent_id = 2061,
    order_num = 1,
    visible = '0',
    status = '0'
WHERE menu_id = 2050;

-- 用户组管理
UPDATE sys_menu
SET
    menu_name = '用户组管理',
    parent_id = 2061,
    order_num = 2,
    visible = '0',
    status = '0'
WHERE menu_id = 2000;

-- 权限策略定义 -> 访问策略管理
UPDATE sys_menu
SET
    menu_name = '访问策略管理',
    parent_id = 2061,
    order_num = 3,
    visible = '0',
    status = '0'
WHERE menu_id = 2006;

-- 权限上下文
UPDATE sys_menu
SET
    menu_name = '权限上下文',
    parent_id = 2061,
    order_num = 4,
    visible = '0',
    status = '0'
WHERE menu_id = 2044;

-- 原策略绑定页面隐藏
UPDATE sys_menu
SET
    visible = '1',
    order_num = 99
WHERE menu_id = 2012;

-- 隐藏可能存在的独立用户-组关系页面
UPDATE sys_menu
SET visible = '1'
WHERE
    menu_name IN (
        '用户组关系管理',
        '用户与用户组关系管理',
        '用户-组关系管理',
        '用户组关系'
    )
    OR component IN (
        'system/userGroup/index',
        'system/userGroupRel/index',
        'system/userGroupRelation/index'
    );

-- 检查结果
SELECT
    menu_id,
    menu_name,
    parent_id,
    order_num,
    visible,
    status,
    component
FROM sys_menu
WHERE parent_id = 2061
ORDER BY order_num;
