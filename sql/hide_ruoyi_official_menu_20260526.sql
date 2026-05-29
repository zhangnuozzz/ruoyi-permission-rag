-- 隐藏若依默认官网菜单，减少模板化展示痕迹
-- RuoYi 菜单 visible 字段约定：0 显示，1 隐藏

UPDATE sys_menu
SET visible = '1'
WHERE menu_name = '若依官网';

-- 可选校验
SELECT menu_id, menu_name, path, visible, status
FROM sys_menu
WHERE menu_name = '若依官网';
