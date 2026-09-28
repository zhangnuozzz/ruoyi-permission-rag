-- ============================================================
-- VACP 向量索引元数据一致性修复
-- Milvus 实际使用 AUTOINDEX，平台元数据同步保持一致
-- ============================================================

ALTER TABLE sys_rag_file
MODIFY COLUMN vector_index_type VARCHAR(32)
DEFAULT 'AUTOINDEX'
COMMENT 'Milvus向量索引类型';

UPDATE sys_rag_file
SET vector_index_type = 'AUTOINDEX'
WHERE vector_index_type IS NULL
   OR vector_index_type = ''
   OR vector_index_type = 'HNSW';
