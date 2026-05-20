# 权限控制 RAG / 大模型向量库平台

## 一、项目简介

本项目基于 RuoYi-Vue 3.2.0 二次开发，面向大模型向量库场景，重点建设“权限治理 + RAG 文件入库 + 安全审计”能力。

平台侧主要负责用户组、权限策略、文档权限标签、访问监控、安全规则、IP 黑名单以及 RAG 文件入库代理能力；RAG Server 侧由 fufu 模块提供文件处理、原文件备份、文本切块与向量写入能力。

当前已完成 RAG 文件入库第一阶段对接：用户可在若依平台上传带权限标签的文件，系统通过后端代理调用 fufu RAG Server，完成 MariaDB 元数据、MinIO 原文件、Milvus 向量切块三段式存储，并自动回写平台侧文档权限标签和入库审计日志。

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

当前已完成平台侧与 fufu RAG Server 的文件入库链路对接。

链路如下：

```text
若依前端 1024
→ 若依后端 8080
→ fufu RAG Server 8081
→ MariaDB sys_rag_file
→ MinIO rag-files bucket
→ Milvus rag_file_chunks
→ 若依平台 sys_rag_doc 文档权限标签回写
→ sys_access_log 入库审计日志

