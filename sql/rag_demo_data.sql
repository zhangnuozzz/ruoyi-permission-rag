-- ============================================
-- RAG 权限过滤演示数据
-- 说明：
-- 1. 本脚本用于恢复 RAG 文件检索权限演示环境；
-- 2. 包含测试文档、受限演示用户、角色关系、用户组关系；
-- 3. 可重复执行，适合本地联调和演示环境初始化。
-- ============================================

-- --------------------------------------------
-- 一、插入 RAG 权限过滤测试文档
-- --------------------------------------------
INSERT INTO sys_rag_doc
(doc_id, doc_name, scope_code, security_level, owner_group_code, status, remark, create_by, create_time)
VALUES
('TEST-PUBLIC-001',    '公开级测试文档',         'PUBLIC',    'PUBLIC',   'GROUP_PUBLIC',    '0', '权限过滤自测：公开文档，默认允许登录用户访问', 'admin', NOW()),
('TEST-INTERNAL-001',  '内部级研发制度测试文档', 'INTERNAL',  'INTERNAL', 'GROUP_RD_01',     '0', '权限过滤自测：研发组内部文档，研发组用户可访问', 'admin', NOW()),
('TEST-DOCADMIN-001',  '文档管理员测试手册',     'DOC_ADMIN', 'INTERNAL', 'GROUP_DOC_ADMIN', '0', '权限过滤自测：文档管理员范围文档，文档管理员可访问', 'admin', NOW()),
('TEST-PROJECT-A-001', '项目A专项测试资料',      'PROJECT_A', 'INTERNAL', 'GROUP_PROJ_A',    '0', '权限过滤自测：项目A范围文档，非项目A用户应被过滤', 'admin', NOW()),
('TEST-SECRET-001',    '秘密级测试资料',         'SECRET',    'SECRET',   'GROUP_DOC_ADMIN', '0', '权限过滤自测：秘密级资料，当前无 SECRET 权限时应被过滤', 'admin', NOW())
ON DUPLICATE KEY UPDATE
doc_name = VALUES(doc_name),
scope_code = VALUES(scope_code),
security_level = VALUES(security_level),
owner_group_code = VALUES(owner_group_code),
status = VALUES(status),
remark = VALUES(remark),
update_by = 'admin',
update_time = NOW();

-- --------------------------------------------
-- 二、插入受限演示用户
-- 账号：test_user
-- 密码：admin123
-- --------------------------------------------
INSERT INTO sys_user
(user_id, dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar, password, status, del_flag, login_ip, login_date, create_by, create_time, remark)
VALUES
(1001, 103, 'test_user', '受限演示用户', '00', 'test_user@example.com', '13800000001', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', '127.0.0.1', NOW(), 'admin', NOW(), 'RAG权限过滤演示用户：仅具备 INTERNAL 权限')
ON DUPLICATE KEY UPDATE
nick_name = VALUES(nick_name),
status = '0',
del_flag = '0',
remark = VALUES(remark),
update_by = 'admin',
update_time = NOW();

-- --------------------------------------------
-- 三、分配普通角色
-- 普通角色 role_id = 2
-- --------------------------------------------
INSERT IGNORE INTO sys_user_role(user_id, role_id)
VALUES (1001, 2);

-- --------------------------------------------
-- 四、分配用户组：仅加入研发一组
-- GROUP_RD_01 -> INTERNAL
-- --------------------------------------------
DELETE FROM sys_user_group_rel WHERE user_id = 1001;

INSERT INTO sys_user_group_rel(user_id, group_id, create_by, create_time)
SELECT 1001, id, 'admin', NOW()
FROM sys_group
WHERE group_code = 'GROUP_RD_01';

-- --------------------------------------------
-- 五、授权普通角色访问演示页面菜单
-- --------------------------------------------
INSERT IGNORE INTO sys_role_menu(role_id, menu_id)
SELECT 2, menu_id
FROM sys_menu
WHERE menu_name IN ('RAG检索测试', 'RAG权限上下文', 'RAG检索审计日志', '文档权限标签')
   OR perms IN ('rag:permissionContext:view');

-- --------------------------------------------
-- 六、执行后可人工验证
-- 1. admin 用户：
--    可访问 PUBLIC / INTERNAL / DOC_ADMIN
-- 2. test_user 用户：
--    仅具备 INTERNAL，允许 PUBLIC + INTERNAL
--    拦截 DOC_ADMIN / PROJECT_A / SECRET
-- --------------------------------------------
