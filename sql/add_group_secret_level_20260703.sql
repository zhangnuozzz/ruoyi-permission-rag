-- 用户组密级增强
-- 对应 VACP 身份信息管理模块中的“用户组密级”
-- 密级等级：PUBLIC公开 INTERNAL内部 SECRET秘密 CONFIDENTIAL机密

ALTER TABLE sys_group
ADD COLUMN group_secret_level VARCHAR(32) DEFAULT 'PUBLIC' COMMENT '用户组密级：PUBLIC公开 INTERNAL内部 SECRET秘密 CONFIDENTIAL机密'
AFTER scope_code;

UPDATE sys_group
SET group_secret_level = CASE
  WHEN group_code = 'GROUP_PUBLIC' THEN 'PUBLIC'
  WHEN scope_code = 'PUBLIC' THEN 'PUBLIC'
  WHEN scope_code = 'INTERNAL' THEN 'INTERNAL'
  WHEN scope_code = 'PROJECT_A' THEN 'SECRET'
  WHEN scope_code = 'DOC_ADMIN' THEN 'CONFIDENTIAL'
  ELSE 'PUBLIC'
END
WHERE group_secret_level IS NULL OR group_secret_level = 'PUBLIC';
