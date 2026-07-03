-- ============================================================
-- VACP 第四轮：文件向量化存储元数据增强
-- 对齐原型文档：
-- 1. 多格式识别
-- 2. 解析方式记录
-- 3. embedding 状态
-- 4. chunk 数
-- 5. 向量索引类型
-- 6. 元数据索引状态
-- 7. doc_level / doc_group / status 写入
-- ============================================================

ALTER TABLE sys_rag_file
ADD COLUMN IF NOT EXISTS file_type VARCHAR(32) DEFAULT 'TXT' COMMENT '文件类型'
AFTER file_name;

ALTER TABLE sys_rag_file
ADD COLUMN IF NOT EXISTS parse_method VARCHAR(64) DEFAULT 'UTF8_TEXT' COMMENT '解析方式'
AFTER file_type;

ALTER TABLE sys_rag_file
ADD COLUMN IF NOT EXISTS chunk_count INT DEFAULT 0 COMMENT '文本切块数量'
AFTER minio_object_name;

ALTER TABLE sys_rag_file
ADD COLUMN IF NOT EXISTS embedding_status VARCHAR(32) DEFAULT 'SUCCESS' COMMENT '向量化状态'
AFTER chunk_count;

ALTER TABLE sys_rag_file
ADD COLUMN IF NOT EXISTS vector_index_type VARCHAR(32) DEFAULT 'HNSW' COMMENT '向量索引类型'
AFTER embedding_status;

ALTER TABLE sys_rag_file
ADD COLUMN IF NOT EXISTS metadata_index_status VARCHAR(32) DEFAULT 'READY' COMMENT '元数据索引状态'
AFTER vector_index_type;

ALTER TABLE sys_rag_file
ADD COLUMN IF NOT EXISTS doc_level VARCHAR(64) DEFAULT NULL COMMENT '写入向量库的文档密级'
AFTER metadata_index_status;

ALTER TABLE sys_rag_file
ADD COLUMN IF NOT EXISTS doc_group VARCHAR(64) DEFAULT NULL COMMENT '写入向量库的文档用户组'
AFTER doc_level;

ALTER TABLE sys_rag_file
ADD COLUMN IF NOT EXISTS doc_status VARCHAR(32) DEFAULT 'ACTIVE' COMMENT '写入向量库的文档状态'
AFTER doc_group;

ALTER TABLE sys_rag_file
ADD COLUMN IF NOT EXISTS metadata_json LONGTEXT COMMENT '向量库元数据JSON'
AFTER doc_status;

UPDATE sys_rag_file
SET file_type = CASE
        WHEN lower(file_name) LIKE '%.txt' THEN 'TXT'
        WHEN lower(file_name) LIKE '%.md' THEN 'MD'
        WHEN lower(file_name) LIKE '%.csv' THEN 'CSV'
        WHEN lower(file_name) LIKE '%.json' THEN 'JSON'
        WHEN lower(file_name) LIKE '%.log' THEN 'LOG'
        ELSE 'TEXT'
    END,
    parse_method = 'UTF8_TEXT',
    embedding_status = 'SUCCESS',
    vector_index_type = 'HNSW',
    metadata_index_status = 'READY',
    doc_level = security_level,
    doc_group = group_id,
    doc_status = 'ACTIVE'
WHERE file_type IS NULL OR doc_level IS NULL;

CREATE INDEX IF NOT EXISTS idx_rag_file_vector_meta
ON sys_rag_file(file_type, embedding_status, doc_level, doc_group, doc_status);
