-- 恢复既有字典维护页面，只调整菜单显示，不扩展任何角色授权。
UPDATE sys_menu
SET menu_name = '名称管理', visible = '0', status = '0', update_time = NOW()
WHERE menu_id = 105 AND parent_id = 1 AND path = 'dict' AND component = 'system/dict/index'
  AND perms = 'system:dict:list';
