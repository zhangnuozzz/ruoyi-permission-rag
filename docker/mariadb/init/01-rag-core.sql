create table if not exists sys_group (
  id bigint(20) not null auto_increment comment '用户组ID',
  group_code varchar(64) not null comment '组编码',
  group_name varchar(128) not null comment '组名称',
  scope_code varchar(64) default null comment '知悉范围编码',
  status char(1) default '0' comment '状态（0正常 1停用）',
  remark varchar(500) default null comment '备注',
  create_by varchar(64) default '' comment '创建者',
  create_time datetime default null comment '创建时间',
  update_by varchar(64) default '' comment '更新者',
  update_time datetime default null comment '更新时间',
  del_flag char(1) default '0' comment '删除标志（0存在 2删除）',
  primary key (id),
  unique key uk_sys_group_code (group_code)
) engine=innodb default charset=utf8mb4 comment='用户组表';

create table if not exists sys_user_group_rel (
  id bigint(20) not null auto_increment comment '关系ID',
  user_id bigint(20) not null comment '用户ID',
  group_id bigint(20) not null comment '用户组ID',
  remark varchar(500) default null comment '备注',
  create_by varchar(64) default '' comment '创建者',
  create_time datetime default null comment '创建时间',
  primary key (id),
  unique key uk_user_group_rel (user_id, group_id),
  key idx_user_group_rel_user (user_id),
  key idx_user_group_rel_group (group_id)
) engine=innodb default charset=utf8mb4 comment='用户与用户组关联表';

create table if not exists sys_policy (
  id bigint(20) not null auto_increment comment '策略ID',
  policy_code varchar(64) not null comment '策略编码',
  policy_name varchar(128) not null comment '策略名称',
  effect varchar(16) default 'ALLOW' comment '策略效果',
  subject_type varchar(32) default null comment '主体类型',
  subject_expr varchar(1000) default null comment '主体表达式',
  resource_expr varchar(1000) default null comment '资源表达式',
  env_expr varchar(1000) default null comment '环境表达式',
  priority int default 100 comment '优先级',
  status char(1) default '0' comment '状态（0正常 1停用）',
  remark varchar(500) default null comment '备注',
  create_by varchar(64) default '' comment '创建者',
  create_time datetime default null comment '创建时间',
  update_by varchar(64) default '' comment '更新者',
  update_time datetime default null comment '更新时间',
  del_flag char(1) default '0' comment '删除标志（0存在 2删除）',
  primary key (id),
  unique key uk_sys_policy_code (policy_code)
) engine=innodb default charset=utf8mb4 comment='访问控制策略表';

create table if not exists sys_policy_bind (
  id bigint(20) not null auto_increment comment '绑定ID',
  policy_id bigint(20) not null comment '策略ID',
  bind_type varchar(32) not null comment '绑定类型',
  bind_target_id bigint(20) not null comment '绑定目标ID',
  status char(1) default '0' comment '状态（0正常 1停用）',
  remark varchar(500) default null comment '备注',
  create_by varchar(64) default '' comment '创建者',
  create_time datetime default null comment '创建时间',
  primary key (id),
  key idx_policy_bind_policy (policy_id),
  key idx_policy_bind_target (bind_type, bind_target_id)
) engine=innodb default charset=utf8mb4 comment='策略绑定表';

create table if not exists sys_rag_doc (
  id bigint(20) not null auto_increment comment '主键ID',
  doc_id varchar(64) not null comment '文档ID',
  doc_name varchar(255) not null comment '文档名称',
  scope_code varchar(64) not null default 'PUBLIC' comment '知悉范围编码',
  security_level varchar(32) not null default 'PUBLIC' comment '文档密级',
  owner_group_code varchar(64) default null comment '所属用户组编码',
  status char(1) default '0' comment '状态（0正常 1停用）',
  remark varchar(500) default null comment '备注',
  create_by varchar(64) default '' comment '创建者',
  create_time datetime default null comment '创建时间',
  update_by varchar(64) default '' comment '更新者',
  update_time datetime default null comment '更新时间',
  del_flag char(1) default '0' comment '删除标志（0存在 2删除）',
  primary key (id),
  unique key uk_sys_rag_doc_id (doc_id),
  key idx_sys_rag_doc_scope (scope_code),
  key idx_sys_rag_doc_group (owner_group_code)
) engine=innodb default charset=utf8mb4 comment='文档权限标签表';

create table if not exists sys_rag_audit_log (
  id bigint(20) not null auto_increment comment '审计ID',
  user_id bigint(20) default null comment '用户ID',
  user_name varchar(64) default '' comment '用户名',
  query_text varchar(1000) default '' comment '检索文本',
  group_codes varchar(1000) default null comment '用户组编码',
  scope_codes varchar(1000) default null comment '知悉范围编码',
  metadata_filter varchar(1000) default null comment '元数据过滤条件',
  allow_access int(1) default null comment '是否允许访问',
  deny_reasons varchar(1000) default null comment '拒绝原因',
  cost_time bigint(20) default null comment '耗时毫秒',
  create_time datetime default current_timestamp comment '创建时间',
  primary key (id),
  key idx_rag_audit_user (user_id),
  key idx_rag_audit_time (create_time)
) engine=innodb default charset=utf8mb4 comment='RAG检索审计日志表';

create table if not exists sys_access_log (
  access_id bigint(20) not null auto_increment comment '访问ID',
  user_id bigint(20) default null comment '用户ID',
  user_name varchar(64) default '' comment '用户名',
  ipaddr varchar(128) default '' comment '访问IP',
  request_uri varchar(500) default '' comment '请求地址',
  request_method varchar(16) default '' comment '请求方式',
  status char(1) default '0' comment '访问状态（0成功 1失败）',
  error_msg varchar(1000) default null comment '错误信息',
  cost_time bigint(20) default null comment '耗时毫秒',
  create_time datetime default current_timestamp comment '创建时间',
  primary key (access_id),
  key idx_access_log_user (user_id),
  key idx_access_log_time (create_time)
) engine=innodb default charset=utf8mb4 comment='系统访问监控日志表';

create table if not exists sys_ip_blacklist (
  blacklist_id bigint(20) not null auto_increment comment '黑名单ID',
  ipaddr varchar(128) not null comment 'IP地址',
  reason varchar(500) default null comment '封禁原因',
  status char(1) default '0' comment '状态（0启用 1停用）',
  create_by varchar(64) default '' comment '创建者',
  create_time datetime default null comment '创建时间',
  update_by varchar(64) default '' comment '更新者',
  update_time datetime default null comment '更新时间',
  remark varchar(500) default null comment '备注',
  primary key (blacklist_id),
  unique key uk_ip_blacklist_addr (ipaddr)
) engine=innodb default charset=utf8mb4 comment='IP黑名单表';

insert into sys_group
(group_code, group_name, scope_code, status, remark, create_by, create_time, del_flag)
values
('GROUP_PUBLIC', '公开文档组', 'PUBLIC', '0', '公开级文档默认用户组', 'admin', now(), '0'),
('GROUP_RD_01', '研发一组', 'INTERNAL', '0', '研发内部资料用户组', 'admin', now(), '0'),
('GROUP_PROJ_A', '项目A组', 'PROJECT_A', '0', '项目A专项资料用户组', 'admin', now(), '0'),
('GROUP_DOC_ADMIN', '文档管理员组', 'DOC_ADMIN', '0', '文档管理员资料用户组', 'admin', now(), '0')
on duplicate key update
group_name = values(group_name),
scope_code = values(scope_code),
status = values(status),
remark = values(remark),
del_flag = values(del_flag);
