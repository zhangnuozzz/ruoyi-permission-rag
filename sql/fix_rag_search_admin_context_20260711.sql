-- Fix RAG search demo context for admin.
-- The secure query context requires sys_user_security_attr before it can build
-- metadata filters. A clean demo database may have groups but no admin security
-- attribute row, which causes USER_SECURITY_ATTR_NOT_FOUND.

INSERT INTO sys_user_security_attr
(user_id, user_name, nick_name, secret_level, access_status, access_start_time, access_end_time,
 risk_level, fail_count, remark, create_by, create_time, update_by, update_time)
VALUES
(1, 'admin', '若依', 'CONFIDENTIAL', 'ACTIVE', NULL, NULL,
 'LOW', 0, 'RAG检索演示：admin安全上下文默认属性', 'admin', NOW(), 'admin', NOW())
ON DUPLICATE KEY UPDATE
user_name = VALUES(user_name),
nick_name = VALUES(nick_name),
secret_level = VALUES(secret_level),
access_status = 'ACTIVE',
access_start_time = NULL,
access_end_time = NULL,
risk_level = 'LOW',
fail_count = 0,
remark = VALUES(remark),
update_by = 'admin',
update_time = NOW();

INSERT INTO sys_user_group_rel (user_id, group_id, remark, create_by, create_time)
SELECT 1, id, 'RAG检索演示：admin绑定公开组', 'admin', NOW()
FROM sys_group
WHERE group_code = 'GROUP_PUBLIC'
  AND NOT EXISTS (SELECT 1 FROM sys_user_group_rel WHERE user_id = 1 AND group_id = sys_group.id);

INSERT INTO sys_user_group_rel (user_id, group_id, remark, create_by, create_time)
SELECT 1, id, 'RAG检索演示：admin绑定研发组', 'admin', NOW()
FROM sys_group
WHERE group_code = 'GROUP_RD_01'
  AND NOT EXISTS (SELECT 1 FROM sys_user_group_rel WHERE user_id = 1 AND group_id = sys_group.id);

INSERT INTO sys_user_group_rel (user_id, group_id, remark, create_by, create_time)
SELECT 1, id, 'RAG检索演示：admin绑定项目A组', 'admin', NOW()
FROM sys_group
WHERE group_code = 'GROUP_PROJ_A'
  AND NOT EXISTS (SELECT 1 FROM sys_user_group_rel WHERE user_id = 1 AND group_id = sys_group.id);

INSERT INTO sys_user_group_rel (user_id, group_id, remark, create_by, create_time)
SELECT 1, id, 'RAG检索演示：admin绑定文档管理员组', 'admin', NOW()
FROM sys_group
WHERE group_code = 'GROUP_DOC_ADMIN'
  AND NOT EXISTS (SELECT 1 FROM sys_user_group_rel WHERE user_id = 1 AND group_id = sys_group.id);
