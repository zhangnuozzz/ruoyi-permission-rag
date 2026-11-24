-- Only for the fresh rag-local test environment. Test accounts use the existing admin demo password.
INSERT INTO sys_user(user_name,nick_name,password,status,del_flag,create_by,create_time)
SELECT 'rag_internal','内部资料测试员',password,'0','0','local_acceptance',NOW()
FROM sys_user WHERE user_id=1 AND NOT EXISTS(SELECT 1 FROM sys_user WHERE user_name='rag_internal');
INSERT INTO sys_user(user_name,nick_name,password,status,del_flag,create_by,create_time)
SELECT 'rag_project','项目A测试员',password,'0','0','local_acceptance',NOW()
FROM sys_user WHERE user_id=1 AND NOT EXISTS(SELECT 1 FROM sys_user WHERE user_name='rag_project');

INSERT INTO sys_user_security_attr(user_id,user_name,nick_name,secret_level,access_status,risk_level,create_by,create_time)
SELECT user_id,user_name,nick_name,CASE user_name WHEN 'test_user' THEN 'PUBLIC' WHEN 'rag_internal' THEN 'INTERNAL' ELSE 'SECRET' END,
 'ACTIVE','LOW','local_acceptance',NOW() FROM sys_user WHERE user_name IN ('test_user','rag_internal','rag_project')
ON DUPLICATE KEY UPDATE secret_level=VALUES(secret_level),access_status='ACTIVE',risk_level='LOW',access_start_time=NULL,access_end_time=NULL;
DELETE r FROM sys_user_group_rel r JOIN sys_user u ON u.user_id=r.user_id
WHERE u.user_name IN ('test_user','rag_internal','rag_project');
INSERT INTO sys_user_group_rel(user_id,group_id,create_by,create_time)
SELECT u.user_id,g.id,'local_acceptance',NOW() FROM sys_user u JOIN sys_group g
 ON g.group_code=CASE WHEN u.user_name='rag_project' THEN 'GROUP_PROJ_A' ELSE 'GROUP_PUBLIC' END
WHERE u.user_name IN ('test_user','rag_internal','rag_project');
INSERT INTO sys_password_state(user_id,changed_at,change_required)
SELECT u.user_id,NOW(3),0 FROM sys_user u WHERE u.user_name IN ('test_user','rag_internal','rag_project')
AND NOT EXISTS(SELECT 1 FROM sys_password_state s WHERE s.user_id=u.user_id);
