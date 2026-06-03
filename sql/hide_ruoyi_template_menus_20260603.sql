-- 隐藏若依模板自带、当前项目演示不需要的菜单
-- 说明：
-- 1. visible = '1' 表示隐藏菜单，但不删除数据，后续需要时可恢复
-- 2. 主要隐藏若依官网、系统监控、系统工具等模板痕迹
-- 3. 保留系统管理及本项目核心菜单：用户组、权限策略、RAG 文件入库、RAG 检索、审计日志、安全中心等

UPDATE sys_menu
SET visible = '1'
WHERE menu_name IN (
  '若依官网',
  '系统监控',
  '在线用户',
  '定时任务',
  '数据监控',
  '服务监控',
  '系统工具',
  '表单构建',
  '代码生成',
  '系统接口'
);

-- 再按 path 补一层，防止菜单名版本差异导致漏掉
UPDATE sys_menu
SET visible = '1'
WHERE path IN (
  'http://ruoyi.vip',
  'monitor',
  'online',
  'job',
  'druid',
  'server',
  'tool',
  'build',
  'gen',
  'swagger'
);

-- 查看隐藏结果
SELECT menu_id, menu_name, parent_id, path, component, visible, status
FROM sys_menu
WHERE menu_name IN (
  '若依官网',
  '系统监控',
  '在线用户',
  '定时任务',
  '数据监控',
  '服务监控',
  '系统工具',
  '表单构建',
  '代码生成',
  '系统接口'
)
OR path IN (
  'http://ruoyi.vip',
  'monitor',
  'online',
  'job',
  'druid',
  'server',
  'tool',
  'build',
  'gen',
  'swagger'
)
ORDER BY parent_id, order_num, menu_id;
