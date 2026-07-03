-- 修复现有演示数据，使其满足 VACP 身份信息管理与 ABAC 强规则
-- 原因：用户组密级现在严格继承自组员最低密级；没有组员的用户组会被视为 PUBLIC。
-- 处理：将 admin 绑定到现有演示用户组，保证现有文档元数据满足“文档密级 <= 用户组密级”。

INSERT INTO sys_user_group_rel (user_id, group_id, remark, create_by, create_time)
SELECT 1, g.id, 'VACP演示数据：admin绑定到用户组，用于计算用户组继承密级', 'admin', NOW()
FROM sys_group g
WHERE g.group_code IN ('GROUP_RD_01', 'GROUP_PROJ_A', 'GROUP_DOC_ADMIN', 'GROUP_PUBLIC')
  AND NOT EXISTS (
      SELECT 1
      FROM sys_user_group_rel rel
      WHERE rel.user_id = 1
        AND rel.group_id = g.id
  );

-- 如果过程已经创建成功，则刷新所有用户组密级
CALL refresh_all_group_secret_levels();

-- 公开组仍固定为 PUBLIC，对应原型文档“公开级别用户组为 all”
UPDATE sys_group
SET group_secret_level = 'PUBLIC',
    scope_code = 'PUBLIC',
    update_by = 'system',
    update_time = NOW()
WHERE group_code = 'GROUP_PUBLIC';

-- 同步现有文档元数据
UPDATE sys_rag_doc d
JOIN sys_group g ON g.group_code = d.owner_group_code
SET d.doc_group_id = d.owner_group_code,
    d.owner_group_name = g.group_name,
    d.owner_group_secret_level = g.group_secret_level
WHERE d.del_flag = '0'
  AND d.owner_group_code IS NOT NULL
  AND d.owner_group_code <> ''
  AND d.security_level <> 'PUBLIC';

-- 公开文档强制绑定公开组
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
