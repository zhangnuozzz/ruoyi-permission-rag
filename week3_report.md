# Week 3 RAG功能接入报告

## 本周我完成的工作

本周我把 `rag_server` 作为一个独立 HTTP 服务接入到了 RuoYi 主项目中，没有把 `rag_server` 的代码迁移进主后端。整体方案是：

- `rag_server` 继续独立运行，默认端口是 `8081`。
- RuoYi 后端新增一个轻量代理层，对外提供 `/rag/proxy/file/**` 接口。
- RuoYi 前端只调用主后端代理接口，不直接调用 `rag_server`。
- 前端新增了一个功能目录“RAG功能测试”，包含四个页面。

这样做的好处是：RAG 服务可以独立开发、独立部署，RuoYi 主系统只负责鉴权、转发和前端展示。

## 已实现的功能

我在前端新增了“RAG功能测试”目录，包含以下四个功能界面：

1. 上传文件
2. milvus存储的内容
3. mariadb存储的内容
4. minIO中存储的内容

对应前端文件：

- `ruoyi-ui/src/views/rag/upload/index.vue`
- `ruoyi-ui/src/views/rag/milvus/index.vue`
- `ruoyi-ui/src/views/rag/mariadb/index.vue`
- `ruoyi-ui/src/views/rag/minio/index.vue`
- `ruoyi-ui/src/api/rag/file.js`

前端路由已加入：

- `ruoyi-ui/src/router/index.js`

## 接口对接方式

### 1. RAG独立服务接口

`rag_server` 默认地址：

```text
http://localhost:8081
```

#### 上传文件

```text
POST /rag/file/upload
```

请求类型：

```text
multipart/form-data
```

表单参数：

```text
file            上传文件
securityLevel   文件密级，例如 INTERNAL
scopeCode       文档级权限标签，例如 INTERNAL
```

可选请求头：

```text
X-User-Id
X-Username
X-Group-Id
X-Group-Name
```

说明：RuoYi 代理层会自动把当前登录用户信息转成这些请求头传给 `rag_server`。

#### 查询 Milvus 中存储的切块内容

```text
GET /rag/file/milvus/list?limit=100
```

返回内容包括：

```text
chunk_id
file_id
security_level
scope_code
group_id
group_name
content
```

我也做了旧 Milvus collection 的兼容处理。如果旧 collection 没有 `scope_code` 字段，接口会自动降级查询已有字段，避免直接报错。

#### 查询 MariaDB 中存储的文件元数据

```text
GET /rag/file/mariadb/list
```

支持查询参数：

```text
fileName
securityLevel
scopeCode
groupId
```

返回内容包括：

```text
fileId
fileName
uploadUserId
uploadUserName
securityLevel
scopeCode
groupId
groupName
minioObjectName
createBy
createTime
```

#### 查询 MinIO 中存储的原始文件对象

```text
GET /rag/file/minio/list?limit=100
```

返回内容包括：

```text
objectName
size
etag
lastModified
```

### 2. RuoYi主后端代理接口

前端实际调用的是 RuoYi 主后端代理接口，默认地址：

```text
http://localhost:8080
```

代理控制器文件：

```text
ruoyi-admin/src/main/java/com/ruoyi/web/controller/rag/RagProxyController.java
```

代理接口如下：

```text
POST /rag/proxy/file/upload
GET  /rag/proxy/file/milvus/list
GET  /rag/proxy/file/mariadb/list
GET  /rag/proxy/file/minio/list
```

代理目标关系：

```text
/rag/proxy/file/upload       -> http://localhost:8081/rag/file/upload
/rag/proxy/file/milvus/list  -> http://localhost:8081/rag/file/milvus/list
/rag/proxy/file/mariadb/list -> http://localhost:8081/rag/file/mariadb/list
/rag/proxy/file/minio/list   -> http://localhost:8081/rag/file/minio/list
```

RAG 服务地址配置在：

```yaml
rag:
  service:
    url: http://localhost:8081
```

配置文件位置：

```text
ruoyi-admin/src/main/resources/application.yml
```

## 数据库和旧环境兼容

我发现本地旧表 `sys_rag_file` 缺少 `scope_code` 字段，所以补了一个升级脚本：

```text
rag_server/src/main/resources/sql/rag_file_processing_upgrade_20260513.sql
```

如果合作者本地已经有旧表，需要执行：

```sql
source rag_server/src/main/resources/sql/rag_file_processing_upgrade_20260513.sql;
```

如果是新环境，可以执行原始初始化脚本：

```sql
source rag_server/src/main/resources/sql/rag_file_processing_20260512.sql;
```

## 启动方式

### 启动 RAG 依赖服务

RAG 上传链路依赖 MinIO、Milvus、etcd。可以用：

```bash
docker compose -f docker-compose.rag.yml up -d minio etcd milvus
```

### 启动 RAG 独立服务

我新增了脚本：

```bash
./scripts/start-rag-server.sh
```

默认端口：

```text
8081
```

### 启动 RuoYi 后端

```bash
./scripts/start-backend.sh
```

默认端口：

```text
8080
```

我顺手修正了本地 MySQL 8 连接串，加入了：

```text
allowPublicKeyRetrieval=true
```

避免启动时报：

```text
Public Key Retrieval is not allowed
```

### 启动 RuoYi 前端

```bash
./scripts/start-frontend.sh
```

我把前端默认端口改成了：

```text
8082
```

原因是 `rag_server` 默认占用 `8081`，前端继续用 `8081` 会冲突。

## 已验证内容

我已经完成以下验证：

- `rag_server` 编译通过。
- RuoYi 后端编译通过。
- RuoYi 前端生产构建通过。
- `rag_server` 可以独立启动。
- RuoYi 后端可以启动，并加载 RAG 代理接口。
- RuoYi 前端可以启动，并出现“RAG功能测试”目录。
- MariaDB 查询接口可用。
- MinIO 查询接口可用。
- Milvus 查询接口可用。
- 上传接口已验证可成功写入 MariaDB、MinIO、Milvus。

## 需要注意的点

1. 前端不要直接请求 `http://localhost:8081`，统一请求 RuoYi 后端代理接口 `/rag/proxy/file/**`。
2. `rag_server` 必须单独启动，否则 RuoYi 代理接口会请求失败。
3. 上传文件前，需要先启动 MinIO、Milvus、etcd。
4. 旧库如果没有 `scope_code` 字段，需要执行升级脚本。
5. 如果 Milvus 里已有旧 collection，接口已经做了兼容，但如果后续要完整使用 `scope_code` 做向量权限过滤，最好重建 collection 或做正式迁移。

## 本次最终交付

我这次交付的是一个“RuoYi 前端 + RuoYi 后端代理 + 独立 rag_server”的完整接入链路。RAG 相关功能已经可以在 RuoYi 前端页面中测试上传、查看 MariaDB 元数据、查看 Milvus 切块内容、查看 MinIO 原始文件对象。
