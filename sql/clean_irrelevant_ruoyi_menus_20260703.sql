-- 清理与 VACP 项目无关的若依原生菜单
-- 只隐藏菜单，不删除数据，后续需要可以恢复。

-- 隐藏若依非核心一级菜单
UPDATE sys_menu
SET visible = '1'
WHERE menu_id IN (
  2,  -- 系统监控
  3,  -- 系统工具
  4   -- 若依官网
);

-- 系统管理下只保留：用户管理、角色管理、菜单管理
-- 隐藏部门、岗位、字典、参数、通知公告、日志管理
UPDATE sys_menu
SET visible = '1'
WHERE menu_id IN (
  103, -- 部门管理
  104, -- 岗位管理
  105, -- 字典管理
  106, -- 参数设置
  107, -- 通知公告
  108  -- 日志管理
);

-- 系统管理排序放到 VACP 四个中心之后
UPDATE sys_menu
SET order_num = 6,
    visible = '0',
    status = '0'
WHERE menu_id = 1;

-- 系统管理保留项排序
UPDATE sys_menu SET order_num = 1, visible = '0', status = '0' WHERE menu_id = 100; -- 用户管理
UPDATE sys_menu SET order_num = 2, visible = '0', status = '0' WHERE menu_id = 101; -- 角色管理
UPDATE sys_menu SET order_num = 3, visible = '0', status = '0' WHERE menu_id = 102; -- 菜单管理
