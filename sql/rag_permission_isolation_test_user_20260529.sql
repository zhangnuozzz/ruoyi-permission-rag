-- RAG 安全检索权限隔离测试数据
-- 作用：
-- 1. 确保存在 PUBLIC 用户组 GROUP_PUBLIC
-- 2. 确保存在 PUBLIC 读取策略 RAG_PUBLIC_READ
-- 3. 将 test_user 绑定到 GROUP_PUBLIC
-- 4. 将 RAG_PUBLIC_READ 策略绑定到 GROUP_PUBLIC
-- 5. 用于演示 admin 与 test_user 查询同一 RAG 问题时的权限隔离效果

USE ry-vue-320;

-- 1. 创建 PUBLIC 用户组
INSERT INTO sys_group
(group_code, group_name, scope_code, status, remark, create_by, create_time)
SELECT
    'GROUP_PUBLIC',
    '公开文档组',
    'PUBLIC',
    '0',
    '普通用户权限隔离测试使用',
    'admin',
    NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM sys_group WHERE group_code = 'GROUP_PUBLIC'
);

-- 2. 创建 PUBLIC 读取策略
INSERT INTO sys_policy
(policy_code, policy_name, effect, subject_type, subject_expr, resource_expr, env_expr, priority, status, remark, create_by, create_time)
SELECT
    'RAG_PUBLIC_READ',
    '公开文档读取策略',
    '0',
    'GROUP',
    'GROUP_PUBLIC',
    'scope_code=PUBLIC',
    '',
    100,
    '0',
    '允许公开用户组检索 PUBLIC 文档',
    'admin',
    NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM sys_policy WHERE policy_code = 'RAG_PUBLIC_READ'
);

-- 3. 将 test_user 从原用户组移除
DELETE FROM sys_user_group_rel
WHERE user_id = (
    SELECT user_id FROM sys_user WHERE user_name = 'test_user'
);

-- 4. 将 test_user 绑定到 GROUP_PUBLIC
INSERT INTO sys_user_group_rel
(user_id, group_id, remark, create_by, create_time)
SELECT
    u.user_id,
    g.id,
    '普通用户权限隔离测试：仅绑定 PUBLIC 组',
    'admin',
    NOW()
FROM sys_user u
JOIN sys_group g ON g.group_code = 'GROUP_PUBLIC'
WHERE u.user_name = 'test_user'
  AND NOT EXISTS (
      SELECT 1
      FROM sys_user_group_rel r
      WHERE r.user_id = u.user_id
        AND r.group_id = g.id
  );

-- 5. 将 RAG_PUBLIC_READ 策略绑定到 GROUP_PUBLIC
INSERT INTO sys_policy_bind
(policy_id, bind_type, bind_target_id, status, remark, create_by, create_time)
SELECT
    p.id,
    'GROUP',
    g.id,
    '0',
    '绑定公开文档读取策略到 GROUP_PUBLIC',
    'admin',
    NOW()
FROM sys_policy p
JOIN sys_group g ON g.group_code = 'GROUP_PUBLIC'
WHERE p.policy_code = 'RAG_PUBLIC_READ'
  AND NOT EXISTS (
      SELECT 1
      FROM sys_policy_bind b
      WHERE b.policy_id = p.id
        AND b.bind_type = 'GROUP'
        AND b.bind_target_id = g.id
  );

-- 6. 验证 test_user 权限链路
SELECT 
    u.user_id,
    u.user_name,
    g.id AS group_id,
    g.group_code,
    g.group_name,
    g.scope_code,
    p.id AS policy_id,
    p.policy_code,
    p.policy_name,
    p.effect,
    p.status AS policy_status,
    b.status AS bind_status
FROM sys_user u
LEFT JOIN sys_user_group_rel r ON u.user_id = r.user_id
LEFT JOIN sys_group g ON r.group_id = g.id
LEFT JOIN sys_policy_bind b ON g.id = b.bind_target_id
LEFT JOIN sys_policy p ON b.policy_id = p.id
WHERE u.user_name = 'test_user';
