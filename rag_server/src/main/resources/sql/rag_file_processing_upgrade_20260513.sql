-- ----------------------------
-- RAG文件处理旧表升级脚本
-- author: fufu
-- date: 2026-05-13 12:37:47 CST
-- ----------------------------

set @column_exists := (
  select count(1)
  from information_schema.columns
  where table_schema = database()
    and table_name = 'sys_rag_file'
    and column_name = 'scope_code'
);

set @ddl := if(@column_exists = 0,
  'alter table sys_rag_file add column scope_code varchar(64) not null default ''INTERNAL'' comment ''文档级权限标签'' after security_level',
  'select ''scope_code already exists''');
prepare stmt from @ddl;
execute stmt;
deallocate prepare stmt;

set @index_exists := (
  select count(1)
  from information_schema.statistics
  where table_schema = database()
    and table_name = 'sys_rag_file'
    and index_name = 'idx_sys_rag_file_scope'
);

set @ddl := if(@index_exists = 0,
  'alter table sys_rag_file add index idx_sys_rag_file_scope (scope_code)',
  'select ''idx_sys_rag_file_scope already exists''');
prepare stmt from @ddl;
execute stmt;
deallocate prepare stmt;
