-- 文档元数据增强
-- 对应 VACP 文档元数据管理：上传人、文档密级、文档用户组、访问状态

ALTER TABLE sys_rag_doc
ADD COLUMN IF NOT EXISTS upload_user_id BIGINT(20) DEFAULT 1 COMMENT '上传人用户ID'
AFTER doc_name;

ALTER TABLE sys_rag_doc
ADD COLUMN IF NOT EXISTS upload_user_name VARCHAR(64) DEFAULT 'admin' COMMENT '上传人用户名'
AFTER upload_user_id;

ALTER TABLE sys_rag_doc
ADD COLUMN IF NOT EXISTS owner_group_name VARCHAR(128) DEFAULT NULL COMMENT '所属用户组名称'
AFTER owner_group_code;

ALTER TABLE sys_rag_doc
ADD COLUMN IF NOT EXISTS owner_group_secret_level VARCHAR(32) DEFAULT 'PUBLIC' COMMENT '所属用户组密级：PUBLIC公开 INTERNAL内部 SECRET秘密 CONFIDENTIAL机密'
AFTER owner_group_name;

ALTER TABLE sys_rag_doc
ADD COLUMN IF NOT EXISTS metadata_status VARCHAR(32) DEFAULT 'ACTIVE' COMMENT '文档元数据访问状态：ACTIVE启用 DISABLED禁用 ARCHIVED归档'
AFTER owner_group_secret_level;

-- 旧数据清洗：把 default 用户组修正为真实用户组，避免后续访问决策无法匹配
UPDATE sys_rag_doc
SET owner_group_code = 'GROUP_RD_01'
WHERE owner_group_code IS NULL OR owner_group_code = '' OR owner_group_code = 'default';

-- 回填用户组名称和用户组密级
UPDATE sys_rag_doc d
LEFT JOIN sys_group g ON d.owner_group_code = g.group_code
SET
  d.owner_group_name = g.group_name,
  d.owner_group_secret_level = g.group_secret_level
WHERE g.group_code IS NOT NULL;

-- 旧 status 同步为标准 metadata_status
UPDATE sys_rag_doc
SET metadata_status = CASE
  WHEN status = '0' THEN 'ACTIVE'
  WHEN status = '1' THEN 'DISABLED'
  ELSE 'ACTIVE'
END
WHERE metadata_status IS NULL OR metadata_status = '';

-- 同步 sys_rag_file 中旧 default 用户组，保持入库文件记录和文档权限标签一致
UPDATE sys_rag_file
SET group_id = 'GROUP_RD_01', group_name = '研发一组'
WHERE group_id IS NULL OR group_id = '' OR group_id = 'default';
