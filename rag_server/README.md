# RAG Server

独立 RAG 文件三段式存储服务。

author: fufu  
date: 2026-05-12

## 运行环境

- OpenJDK 17
- MariaDB
- MinIO
- Milvus

## 启动

```bash
mvn -DskipTests package
java -jar target/rag-server-1.0.0.jar
```

默认端口为 `8081`，配置在 `src/main/resources/application.yml`。
项目根目录也提供了启动脚本：

```bash
./scripts/start-rag-server.sh
```

author: fufu  
date: 2026-05-13 12:37:47 CST

## 初始化表

执行：

```sql
source src/main/resources/sql/rag_file_processing_20260512.sql;
```

如果已有旧表，需要先手动补充 `scope_code` 字段和索引，或按现场数据迁移策略处理。

旧表可执行幂等升级脚本：

```sql
source src/main/resources/sql/rag_file_processing_upgrade_20260513.sql;
```

author: fufu  
date: 2026-05-13 12:37:47 CST

## 上传接口

路径保持为：

```text
POST /rag/file/upload
```

表单参数：

```text
file
securityLevel
scopeCode
```

`scopeCode` 会统一转换为大写后写入 MariaDB 和 Milvus metadata，例如 `internal` 会保存为 `INTERNAL`。

可选请求头，用于替代原若依 `LoginUser` 上下文：

```text
X-User-Id
X-Username
X-Group-Id
X-Group-Name
```

不传时默认使用 `anonymous/default`，接口响应仍保持若依 `AjaxResult` 结构。

## 测试查询接口

以下接口用于RuoYi前端“RAG功能测试”目录展示三类存储内容：

```text
GET /rag/file/milvus/list?limit=100
GET /rag/file/mariadb/list
GET /rag/file/minio/list?limit=100
```

author: fufu  
date: 2026-05-13 12:37:47 CST
