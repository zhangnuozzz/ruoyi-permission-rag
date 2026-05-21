# 权限控制 RAG / 大模型向量库平台

## 一、项目简介

本项目基于 RuoYi-Vue 3.2.0 二次开发，面向大模型向量库场景，重点建设“权限治理 + RAG 文件入库 + 安全审计”能力。

平台侧主要负责用户组、权限策略、文档权限标签、访问监控、安全规则、IP 黑名单以及 RAG 文件入库代理能力；RAG Server 侧由 RAG Server 模块提供文件处理、原文件备份、文本切块与向量写入能力。

当前已完成 RAG 文件入库第一阶段对接：用户可在若依平台上传带权限标签的文件，系统通过后端代理调用 RAG Server，完成 MariaDB 元数据、MinIO 原文件、Milvus 向量切块三段式存储，并自动回写平台侧文档权限标签和入库审计日志。

## 二、技术栈

### 平台侧

- RuoYi-Vue 3.2.0
- Spring Boot 2.3.12.RELEASE
- Spring Security
- MyBatis
- MariaDB
- Redis
- Vue 2.6.10
- Element UI
- Java 8
- Node.js 16

### RAG Server 侧

- Spring Boot
- Java 17
- MariaDB
- MinIO
- Milvus
- LangChain4j

## 三、已完成功能

### 1. 权限治理基础模块

- 用户组管理
- 用户与用户组关系维护
- 权限策略管理
- 策略绑定管理
- 文档权限标签管理
- RAG 审计日志基础页面
- RAG 检索测试页面基础能力

### 2. 安全中心

- 访问监控
- 静态安全规则拦截
- 危险路径拦截
- 危险参数拦截
- IP 黑名单管理
- IP 黑名单启用 / 停用
- 黑名单请求拦截
- 安全访问日志记录

### 3. RAG 文件入库对接

当前已完成平台侧与 RAG Server 的文件入库链路对接。

链路如下：

若依前端 1024
→ 若依后端 8080
→ RAG Server 8081
→ MariaDB sys_rag_file
→ MinIO rag-files bucket
→ Milvus rag_file_chunks
→ 若依平台 sys_rag_doc 文档权限标签回写
→ sys_access_log 入库审计日志

已验证能力：

- 若依前端上传文件
- 选择文件密级
- 选择权限标签
- 若依后端代理转发至 RAG Server
- RAG Server 写入 MariaDB 元数据
- RAG Server 备份原文件到 MinIO
- RAG Server 写入 Milvus 向量切块
- 平台侧自动回写 sys_rag_doc
- 平台侧记录 sys_access_log 入库审计
- 前端页面展示 MariaDB / MinIO / Milvus 三端结果
- 访问监控页面展示 RAG 入库审计详情

### 4. RAG 安全检索预对接

当前 RAG Server 真实检索接口尚未完成，平台侧已先完成安全检索预对接链路。

已验证能力：

- 根据当前登录用户生成 PermissionContext 权限上下文
- 查询用户所属用户组
- 查询用户命中的权限策略
- 汇总 groupCodes 与 scopeCodes
- 生成 Metadata Filter
- 使用 sys_rag_doc 模拟候选检索结果
- 执行平台侧二次权限过滤
- 记录 RAG 检索审计日志
- 前端展示本次检索权限上下文、过滤表达式、过滤结果和后续传给 RAG Server 的检索请求 JSON

安全检索预对接链路如下：

当前用户
→ 用户组关系 sys_user_group_rel
→ 用户组 sys_group
→ 策略绑定 sys_policy_bind
→ 策略 sys_policy
→ PermissionContext
→ Metadata Filter
→ 模拟候选结果
→ 二次过滤
→ sys_rag_audit_log 检索审计

## 四、核心页面

### RAG 文件入库

- 文件上传
- 文件密级选择
- 权限标签选择
- 上传结果结构化展示
- MariaDB 文件元数据展示
- Milvus 切块内容展示
- MinIO 原始文件对象展示
- 入库链路状态展示

### 文档权限标签

- 查看文档权限标签
- 展示自动入库回写的文档记录
- 展示文档 ID、文档名称、知悉范围、密级、所属用户组、来源说明
- 支持新增、修改、删除、查询

### 安全中心 / 访问监控

- 展示后台访问日志
- 展示 RAG 入库业务审计
- 支持查看 RAG_FILE_UPLOAD_SUCCESS / RAG_FILE_UPLOAD_FAIL 详情
- 展示访问 IP、请求路径、请求方式、状态、耗时、审计说明

### 安全中心 / IP 黑名单

- 新增黑名单 IP
- 修改黑名单 IP
- 删除黑名单 IP
- 启用 / 停用黑名单
- 黑名单请求拦截
- 本地开发环境保护

### RAG 权限上下文

- 展示当前登录用户权限画像
- 展示所属用户组
- 展示可访问 scopeCode
- 展示命中策略
- 展示 allowAccess 与 denyReasons
- 展示后续传给 RAG Server 检索接口的权限上下文 JSON

### RAG 安全检索测试

- 输入检索问题
- 自动生成权限上下文
- 自动生成 Metadata Filter
- 使用 sys_rag_doc 模拟候选检索结果
- 展示二次过滤后结果
- 展示后续传给 RAG Server 的检索请求 JSON
- 写入 RAG 检索审计日志

### RAG 检索审计日志

- 展示检索用户
- 展示检索内容
- 展示用户组上下文
- 展示可访问权限标签
- 展示 Metadata Filter
- 展示访问决策、拒绝原因、耗时和审计时间
- 支持查看完整审计详情

## 五、本地启动方式

本项目本地联调需要启动四类服务：

- Docker 依赖环境：MinIO / Milvus / etcd
- RAG Server：8081
- 若依后端：8080
- 若依前端：1024

### 1. 启动 Docker 依赖环境

需要先打开 Docker Desktop。

start-rag-env

或者进入 RAG Server 项目根目录执行：

docker compose -f docker-compose.rag.yml up -d

### 2. 启动 RAG Server

start-rag

默认端口：8081。

### 3. 启动若依后端

start-ruoyi

默认端口：8080。

### 4. 启动若依前端

start-ui

默认端口：1024。

访问地址：

http://localhost:1024

## 六、环境检查

项目提供环境检查脚本：

./scripts/check-env.sh

检查内容包括：

- 若依前端 1024
- 若依后端 8080
- RAG Server 8081
- MinIO 9000
- Milvus 19530
- Docker 容器状态
- MinIO 健康检查
- RAG Server MariaDB 接口

## 七、演示流程

### 1. 打开若依后台

访问：

http://localhost:1024

### 2. 进入 RAG 文件入库页面

选择测试文件，例如：

username.txt

选择：

- 文件密级：INTERNAL
- 权限标签：INTERNAL

点击上传入库。

### 3. 查看三端存储

上传成功后，在当前页面查看：

- MariaDB 文件元数据
- Milvus 切块内容
- MinIO 原始文件对象

### 4. 查看文档权限标签回写

进入“文档权限标签”，确认上传文件已自动生成权限标签记录。

### 5. 查看入库审计

进入“安全中心 → 访问监控”，确认存在：

/rag/file/upload#audit

RAG_FILE_UPLOAD_SUCCESS

点击详情，可查看：

- fileName
- fileId
- securityLevel
- scopeCode
- minioObjectName
- chunkCount

## 八、当前阶段成果

当前已完成 RAG 文件入库与安全检索预对接平台侧闭环：

文件上传
→ 三段式存储
→ 权限标签回写
→ 入库审计记录
→ 访问监控展示
→ 权限上下文构建
→ Metadata Filter 生成
→ 模拟安全检索
→ 二次权限过滤
→ 检索审计记录
→ 检索审计页面展示
→ 运维检查脚本
→ 演示说明文档

该能力为后续安全检索、权限过滤、用户组授权和审计追踪提供基础。

## 九、后续计划

RAG Server 检索链路完成后，平台侧将继续推进：

- 对接真实 RAG 检索接口
- 将当前模拟检索替换为远程 RAG Server 检索
- 请求中携带用户身份、用户组、权限标签上下文
- 基于 scopeCode、securityLevel、groupId 进行检索过滤
- 保留平台侧二次权限过滤能力
- 增强 RAG 检索审计
- 完善权限拒绝原因展示
- 完成项目部署文档和阶段汇报材料

## 十、注意事项

- 不要将真实数据库密码提交到公开仓库。
- 本地启动脚本中的密码仅用于个人开发环境。
- 如果 Docker 未启动，start-rag-env 会失败，需要先打开 Docker Desktop。
- 如果 Milvus 未启动，RAG 文件上传会在向量写入阶段超时。
- 如果 MinIO bucket 不存在，需要确认 rag-files bucket 是否已创建。

## 十一、原始项目说明

本项目基于 RuoYi-Vue 3.2.0 二次开发。原始若依 README 已备份为：

README_RuoYi_Original.md
