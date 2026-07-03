-- 修复 VACP 身份与 ABAC 强规则脚本的运行时数据冲突
-- 原因：sys_user_group_rel 触发器会更新 sys_group，插入关系时如果同时读取 sys_group，会触发 MariaDB 1442。

-- 1. 临时删除用户组关系触发器，避免插入演示关系时更新冲突
DROP TRIGGER IF EXISTS trg_user_group_rel_ai_refresh_secret;
DROP TRIGGER IF EXISTS trg_user_group_rel_au_refresh_secret;
DROP TRIGGER IF EXISTS trg_user_group_rel_ad_refresh_secret;
DROP TRIGGER IF EXISTS trg_user_security_attr_au_refresh_group_secret;

-- 2. 补齐 admin 到现有演示用户组的绑定
INSERT INTO sys_user_group_rel (user_id, group_id, remark, create_by, create_time)
SELECT 1, id, 'VACP演示数据：admin绑定到用户组，用于计算用户组继承密级', 'admin', NOW()
FROM sys_group
WHERE group_code = 'GROUP_RD_01'
  AND NOT EXISTS (SELECT 1 FROM sys_user_group_rel WHERE user_id = 1 AND group_id = sys_group.id);

INSERT INTO sys_user_group_rel (user_id, group_id, remark, create_by, create_time)
SELECT 1, id, 'VACP演示数据：admin绑定到用户组，用于计算用户组继承密级', 'admin', NOW()
FROM sys_group
WHERE group_code = 'GROUP_PROJ_A'
  AND NOT EXISTS (SELECT 1 FROM sys_user_group_rel WHERE user_id = 1 AND group_id = sys_group.id);

INSERT INTO sys_user_group_rel (user_id, group_id, remark, create_by, create_time)
SELECT 1, id, 'VACP演示数据：admin绑定到用户组，用于计算用户组继承密级', 'admin', NOW()
FROM sys_group
WHERE group_code = 'GROUP_DOC_ADMIN'
  AND NOT EXISTS (SELECT 1 FROM sys_user_group_rel WHERE user_id = 1 AND group_id = sys_group.id);

INSERT INTO sys_user_group_rel (user_id, group_id, remark, create_by, create_time)
SELECT 1, id, 'VACP演示数据：admin绑定到公开组', 'admin', NOW()
FROM sys_group
WHERE group_code = 'GROUP_PUBLIC'
  AND NOT EXISTS (SELECT 1 FROM sys_user_group_rel WHERE user_id = 1 AND group_id = sys_group.id);

-- 3. 先手动设置演示用户组密级，保证现有文档满足“文档密级 <= 用户组密级”
UPDATE sys_group
SET group_secret_level = 'CONFIDENTIAL',
    manager_user_id = 1,
    manager_name = 'admin',
    update_by = 'system',
    update_time = NOW()
WHERE group_code IN ('GROUP_RD_01', 'GROUP_PROJ_A', 'GROUP_DOC_ADMIN');

UPDATE sys_group
SET group_secret_level = 'PUBLIC',
    manager_user_id = 1,
    manager_name = 'admin',
    scope_code = 'PUBLIC',
    update_by = 'system',
    update_time = NOW()
WHERE group_code = 'GROUP_PUBLIC';

-- 4. 同步文档元数据，避免现有数据和 ABAC 规则冲突
UPDATE sys_rag_doc d
JOIN sys_group g ON g.group_code = d.owner_group_code
SET d.doc_group_id = d.owner_group_code,
    d.owner_group_name = g.group_name,
    d.owner_group_secret_level = g.group_secret_level
WHERE d.del_flag = '0'
  AND d.owner_group_code IS NOT NULL
  AND d.owner_group_code <> ''
  AND d.security_level <> 'PUBLIC';

UPDATE sys_rag_doc
SET owner_group_code = 'GROUP_PUBLIC',
    doc_group_id = 'GROUP_PUBLIC',
    owner_group_name = '公开文档组',
    owner_group_secret_level = 'PUBLIC',
    scope_code = 'PUBLIC'
WHERE security_level = 'PUBLIC';

UPDATE sys_rag_file
SET group_id = 'GROUP_PUBLIC',
    group_name = '公开文档组',
    scope_code = 'PUBLIC'
WHERE security_level = 'PUBLIC';
