-- ----------------------------
-- RAG文件处理系统初始化脚本
-- author: fufu
-- date: 2026-05-05
-- ----------------------------

drop table if exists sys_rag_file;
create table sys_rag_file (
  file_id            varchar(64)      not null                  comment '文件ID',
  file_name          varchar(255)     not null                  comment '文件名',
  upload_user_id     bigint(20)       not null                  comment '上传文件的用户ID',
  upload_user_name   varchar(64)      not null                  comment '上传文件的用户账号',
  security_level     varchar(64)      not null                  comment '文件密级',
  group_id           varchar(64)      not null                  comment '所属用户组ID',
  group_name         varchar(128)     not null                  comment '所属用户组名称',
  minio_object_name  varchar(512)     not null                  comment 'MinIO原始文件备份对象名',
  create_by          varchar(64)      default ''                comment '创建者',
  create_time        datetime                                  comment '创建时间',
  primary key (file_id),
  key idx_sys_rag_file_user (upload_user_id),
  key idx_sys_rag_file_group (group_id),
  key idx_sys_rag_file_security (security_level)
) engine=innodb default charset=utf8mb4 comment = 'RAG文件元数据表';
