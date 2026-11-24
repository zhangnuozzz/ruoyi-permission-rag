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

全新本机测试环境的正式 embedding 配置、模型服务、空库迁移与端到端验收见[本地运行记录](../docs/本地测试环境准备.md)。`rag.embedding.mode=http` 时文档与查询共用本地向量模型，文档通过同一 tokenizer 分块；`fake` 仅保留历史演示用途。

## 初始化表

执行：

```sql
source src/main/resources/sql/rag_file_processing_20260512.sql;
```

如果已有旧表，需要先手动补充 `scope_code` 字段和索引，或按现场数据迁移策略处理。

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
