# RAG 文件入库演示说明

## 一、功能目标

本功能用于验证权限治理平台与 fufu RAG Server 的第一阶段对接能力。

用户在若依平台上传带权限标签的文件后，系统会通过若依后端代理调用 fufu RAG Server，完成文件入库、原文件备份、文本切块、向量写入、权限标签回写与审计记录。

## 二、当前已完成链路

若依前端 1024
→ 若依后端 8080
→ fufu RAG Server 8081
→ MariaDB sys_rag_file
→ MinIO rag-files bucket
→ Milvus rag_file_chunks
→ 若依平台 sys_rag_doc 文档权限标签回写
→ sys_access_log 入库审计日志

## 三、启动顺序

1. 启动 Docker 依赖环境：

start-rag-env

2. 启动 fufu RAG Server：

start-rag

3. 启动若依后端：

start-ruoyi

4. 启动若依前端：

start-ui

## 四、环境检查

可以执行：

./scripts/check-env.sh

重点检查：

- 1024：若依前端
- 8080：若依后端
- 8081：fufu RAG Server
- 9000：MinIO
- 19530：Milvus

## 五、演示步骤

1. 打开 http://localhost:1024 并登录若依后台。
2. 进入 RAG 文件入库页面。
3. 选择 username.txt。
4. 文件密级选择 INTERNAL。
5. 权限标签选择 INTERNAL。
6. 点击上传入库。
7. 查看 MariaDB、MinIO、Milvus 三端存储结果。
8. 进入文档权限标签页面，确认 username.txt 已自动回写。
9. 进入安全中心 → 访问监控，确认存在 /rag/file/upload#audit 审计日志。

## 六、当前阶段结论

当前已完成 RAG 文件入库平台侧闭环：

文件上传
→ 三段式存储
→ 权限标签回写
→ 审计日志记录
→ 前端可视化展示

该能力为后续安全检索、权限过滤、用户组授权和审计追踪提供基础。
