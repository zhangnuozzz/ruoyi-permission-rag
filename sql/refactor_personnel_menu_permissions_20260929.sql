USE `ry-vue-320`;

SET @old_user_menu_id := (
    SELECT menu_id
    FROM sys_menu
    WHERE component = 'system/user/index'
      AND menu_id <> 2050
    ORDER BY menu_id
    LIMIT 1
);

-- 原用户管理功能按钮改挂到人员权限管理下面
UPDATE sys_menu
SET parent_id = 2050
WHERE parent_id = @old_user_menu_id
  AND menu_type = 'F';

-- 原若依用户管理继续隐藏
UPDATE sys_menu
SET visible = '1'
WHERE menu_id = @old_user_menu_id;

-- 新页面
UPDATE sys_menu
SET
    menu_name = '人员权限管理',
    component = 'system/user/index',
    perms = 'system:user:list',
    visible = '0',
    status = '0',
    order_num = 1
WHERE menu_id = 2050;

SELECT
    menu_id,
    menu_name,
    parent_id,
    menu_type,
    perms,
    visible
FROM sys_menu
WHERE menu_id = 2050
   OR parent_id = 2050
ORDER BY menu_type, order_num;
