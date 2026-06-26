-- 修复日志菜单路由冲突：
-- 1. 日志管理作为父菜单，不再指向 RAG 审计页面；
-- 2. 操作日志、登录日志保持若依原生页面；
-- 3. RAG 检索审计日志独立到 rag/auditLog/index。

UPDATE sys_menu
SET
  component = NULL,
  perms = '',
  menu_type = 'M',
  visible = '0',
  status = '0'
WHERE menu_id = 108;

UPDATE sys_menu
SET
  parent_id = 108,
  path = 'operlog',
  component = 'monitor/operlog/index',
  perms = 'monitor:operlog:list',
  menu_type = 'C',
  visible = '0',
  status = '0'
WHERE menu_id = 500;

UPDATE sys_menu
SET
  parent_id = 108,
  path = 'logininfor',
  component = 'monitor/logininfor/index',
  perms = 'monitor:logininfor:list',
  menu_type = 'C',
  visible = '0',
  status = '0'
WHERE menu_id = 501;

UPDATE sys_menu
SET
  menu_name = 'RAG审计日志',
  parent_id = 1,
  path = 'ragAuditLog',
  component = 'rag/auditLog/index',
  perms = 'rag:audit:list',
  menu_type = 'C',
  visible = '0',
  status = '0'
WHERE menu_id = 2018;
