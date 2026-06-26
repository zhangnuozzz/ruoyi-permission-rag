-- RAG 行为分析告警表
DROP TABLE IF EXISTS sys_rag_behavior_alert;
CREATE TABLE sys_rag_behavior_alert (
  id BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '告警ID',
  source_log_id BIGINT(20) DEFAULT NULL COMMENT '来源审计日志ID',
  user_id BIGINT(20) DEFAULT NULL COMMENT '用户ID',
  user_name VARCHAR(64) DEFAULT '' COMMENT '用户名',
  alert_type VARCHAR(64) DEFAULT '' COMMENT '告警类型',
  alert_level VARCHAR(20) DEFAULT 'low' COMMENT '告警等级',
  alert_reason VARCHAR(500) DEFAULT '' COMMENT '告警原因',
  query_text VARCHAR(1000) DEFAULT '' COMMENT '触发检索内容',
  allow_access INT(1) DEFAULT NULL COMMENT '访问决策',
  cost_time BIGINT(20) DEFAULT NULL COMMENT '耗时毫秒',
  status VARCHAR(20) DEFAULT 'unhandled' COMMENT '处理状态',
  remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_source_type (source_log_id, alert_type)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='RAG行为分析告警表';

-- 菜单：RAG行为告警
INSERT INTO sys_menu
(menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 'RAG行为告警',
       menu_id,
       6,
       'behaviorAlert',
       'rag/behaviorAlert/index',
       1,
       0,
       'C',
       '0',
       '0',
       'system:behaviorAlert:list',
       'warning',
       'admin',
       NOW(),
       'RAG行为分析告警页面'
FROM sys_menu
WHERE menu_name = '系统管理'
  AND NOT EXISTS (
    SELECT 1 FROM (SELECT menu_id FROM sys_menu WHERE path = 'behaviorAlert' AND component = 'rag/behaviorAlert/index') t
  )
LIMIT 1;

-- 按钮权限
SET @parentId := (SELECT menu_id FROM sys_menu WHERE path = 'behaviorAlert' AND component = 'rag/behaviorAlert/index' LIMIT 1);

INSERT INTO sys_menu
(menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '行为告警查询', @parentId, 1, '#', '', 1, 0, 'F', '0', '0', 'system:behaviorAlert:list', '#', 'admin', NOW(), ''
WHERE @parentId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM sys_menu WHERE perms = 'system:behaviorAlert:list') t);

INSERT INTO sys_menu
(menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '行为告警分析', @parentId, 2, '#', '', 1, 0, 'F', '0', '0', 'system:behaviorAlert:analyze', '#', 'admin', NOW(), ''
WHERE @parentId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM sys_menu WHERE perms = 'system:behaviorAlert:analyze') t);

INSERT INTO sys_menu
(menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '行为告警删除', @parentId, 3, '#', '', 1, 0, 'F', '0', '0', 'system:behaviorAlert:remove', '#', 'admin', NOW(), ''
WHERE @parentId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM sys_menu WHERE perms = 'system:behaviorAlert:remove') t);

INSERT INTO sys_menu
(menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '行为告警导出', @parentId, 4, '#', '', 1, 0, 'F', '0', '0', 'system:behaviorAlert:export', '#', 'admin', NOW(), ''
WHERE @parentId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM sys_menu WHERE perms = 'system:behaviorAlert:export') t);
