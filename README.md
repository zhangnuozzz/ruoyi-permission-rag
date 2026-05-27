# 权限控制 RAG / 大模型向量库平台

## 一、项目简介

本项目基于 **RuoYi-Vue 3.2.0** 二次开发，面向大模型向量库与企业知识库场景，重点建设“权限治理 + RAG 文件入库 + 安全检索 + 二次过滤 + 审计追踪”能力。

平台侧主要负责用户组、权限策略、文档权限标签、访问监控、安全规则、IP 黑名单以及 RAG 文件入库代理能力；RAG Server 侧负责文件处理、原文存储、文本切块、向量写入与真实检索能力。

当前项目已完成 RAG 文件入库与真实检索第一轮联调：平台侧通过 `useRemote=true` 调用 RAG Server 的 `/rag/search`，RAG Server 基于 Milvus 返回真实候选结果，若依平台继续执行权限上下文构造、Metadata Filter 生成、二次权限过滤与审计留痕。

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

### RAG Server 侧

- Java 17
- Spring Boot
- MariaDB
- MinIO
- Milvus
- LangChain4j 相关文本处理能力

## 三、当前已完成能力

### 1. 权限治理

已完成权限治理核心数据表与后台管理能力，包括：

- 用户组管理
- 用户与用户组关系
- 权限策略管理
- 策略绑定管理
- 文档权限标签
- PermissionContext 权限上下文生成
- Metadata Filter 生成

### 2. RAG 文件入库

已完成若依平台到 RAG Server 的文件入库链路：

~~~text
若依前端上传文件
        ↓
若依后端代理转发
        ↓
RAG Server 接收文件
        ↓
MariaDB 写入文件元数据
        ↓
MinIO 保存原始文件
        ↓
Milvus 写入文本切块向量
        ↓
平台侧回显文件与权限标签
~~~

当前已验证上传 `username.txt` 后，RAG Server 可写入 MariaDB、MinIO 与 Milvus，并能通过 Milvus 列表接口查看切块内容。

### 3. 安全检索 Mock 闭环

平台侧已完成基于 `sys_rag_doc` 的模拟检索闭环：

~~~text
当前登录用户
        ↓
PermissionContext 权限上下文
        ↓
PolicyDecision 策略决策
        ↓
Metadata Filter
        ↓
sys_rag_doc 模拟候选结果
        ↓
二次权限过滤
        ↓
RAG 检索审计日志
~~~

该能力用于验证平台侧安全检索逻辑，即使候选结果来源从 Mock 替换为真实 RAG Server，平台侧仍能统一执行权限控制与审计。

### 4. 真实 RAG 检索联调

已完成平台侧真实检索适配：

- `RagSearchRequest` 支持 `topK`
- `RagSearchRequest` 支持 `useRemote`
- 新增 `IRagRemoteSearchService`
- 新增 `RagRemoteSearchServiceImpl`
- `RagSearchController` 支持 Mock / Remote 双模式
- 前端 RAG 检索页面支持真实检索模式
- 支持调用 RAG Server `/rag/search`
- 支持解析 RAG Server 返回的直接列表格式
- 支持真实检索结果继续进入平台侧二次权限过滤

当前真实检索链路为：

~~~text
若依前端选择真实检索模式
        ↓
若依后端 /rag/search
        ↓
useRemote=true
        ↓
RagRemoteSearchService 调用 RAG Server /rag/search
        ↓
RAG Server 查询 Milvus
        ↓
返回真实候选切块
        ↓
平台侧二次权限过滤
        ↓
写入 RAG 检索审计日志
~~~

### 5. 审计追踪

已完成 RAG 检索审计记录，主要字段包括：

- 当前用户
- 用户组
- 可访问标签
- 检索问题
- Metadata Filter
- 访问决策
- 拒绝原因
- 检索耗时
- 创建时间

其中 `allow_access` 字段语义已统一为：

~~~text
1 = 放行
0 = 拒绝
~~~

## 四、核心分支说明

### main

稳定展示分支，保留权限治理、文件入库、Mock 安全检索闭环等相对稳定能力。

### fufu_week4

RAG Server 真实检索相关分支，已支持：

- 文件上传
- MariaDB 文件元数据写入
- MinIO 原文件保存
- Milvus 文本切块写入
- `/rag/search` 真实检索接口

### feature/platform-remote-search

平台侧真实检索联调分支，已支持：

- `useRemote=true`
- `topK`
- 远程 RAG Server 调用
- RAG Server 响应格式适配
- 前端真实检索模式
- 审计日志语义修复
- 个人仓库与工作仓库同步推送

## 五、当前演示流程

建议演示时按以下顺序进行：

~~~text
1. 启动 MariaDB、Redis、MinIO、Milvus
2. 启动 fufu RAG Server，端口 8081
3. 启动若依后端，端口 8080
4. 启动若依前端，端口 1024
5. 在 RAG 文件入库页面上传测试文件
6. 查看 RAG Server Milvus 列表，确认切块写入
7. 在 RAG 安全检索页面选择真实检索模式
8. 输入 username 等关键词进行检索
9. 查看真实检索结果是否返回
10. 查看 RAG 审计日志，确认 allow_access、metadata_filter、cost_time 等字段
~~~

## 六、下一步任务

后续建议优先处理：

1. 固化演示流程：上传文件 → Milvus 查看 → 真实检索 → 审计日志；
2. 补充权限边界测试：`INTERNAL`、`DOC_ADMIN`、非授权用户；
3. 整理项目截图和汇报材料；
4. 继续完善真实检索场景下的错误提示与异常兜底；
5. 在真实检索稳定后，再考虑将相关能力合并回 `main` 稳定展示分支。

## 七、注意事项

- 不要提交真实数据库密码；
- 不要提交本地 MinIO、Milvus、MariaDB 的敏感配置；
- `rag_server` 部分如存在本地环境配置，应优先 stash 或放入本地忽略文件；
- 前端构建出现 asset size warning 属于当前若依前端常见体积警告，不影响本地演示。

