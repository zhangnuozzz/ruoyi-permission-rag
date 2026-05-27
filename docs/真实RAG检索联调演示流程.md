# 真实 RAG 检索联调演示流程

本文档用于记录“权限控制 RAG / 大模型向量库平台”的真实检索联调演示步骤，方便后续向老师、甲方或团队成员展示当前阶段成果。

---

## 一、当前阶段结论

当前项目已经完成第一轮真实 RAG 检索联调：

1. 若依平台侧已经完成权限上下文生成；
2. 平台侧能够生成 Metadata Filter；
3. RAG Server 已支持 /rag/search 真实检索接口；
4. RAG Server 能够从 Milvus 返回真实向量检索结果；
5. 若依平台能够通过 useRemote=true 调用 RAG Server；
6. 平台侧能够对远程返回结果继续执行二次权限过滤；
7. RAG 检索审计日志能够正常落库；
8. sys_rag_audit_log.allow_access 字段语义已统一为：1 表示放行，0 表示拒绝。

---

## 二、环境端口

| 服务 | 端口 | 说明 |
|---|---:|---|
| 若依前端 | 1024 | 平台前端页面 |
| 若依后端 | 8080 | 平台后端接口 |
| RAG Server | 8081 | fufu 的 RAG 服务 |
| MariaDB | 3306 | 若依与 RAG Server 数据库 |
| Redis | 6379 | 若依登录与缓存 |
| MinIO | 9000 / 9001 | 原文件对象存储 |
| Milvus | 19530 | 向量切块存储 |

---

## 三、启动顺序

### 1. 启动基础组件

确认 MariaDB、Redis、MinIO、Milvus 已启动。

可通过如下命令检查端口：

    lsof -i :3306
    lsof -i :6379
    lsof -i :9000
    lsof -i :9001
    lsof -i :19530

---

### 2. 启动 RAG Server

真实 RAG Server 代码位于 fufu_week4 分支。当前推荐通过 worktree 单独启动，避免切换平台侧开发分支。

    cd "/Users/zhangnuo/Desktop/大模型向量库项目/RuoYi-3.2.0-final/RuoYi-Vue/RuoYi-Vue-fufu-week4/rag_server"
    mvn spring-boot:run

如果本地已经配置了快捷命令，也可以直接执行：

    start-rag

启动成功后应看到：

    Tomcat started on port 8081
    Started RagServerApplication

---

### 3. 启动若依后端

    cd "/Users/zhangnuo/Desktop/大模型向量库项目/RuoYi-3.2.0-final/RuoYi-Vue"
    mvn -pl ruoyi-admin spring-boot:run

若依后端启动成功后，访问端口为：

    http://localhost:8080

---

### 4. 启动若依前端

    cd "/Users/zhangnuo/Desktop/大模型向量库项目/RuoYi-3.2.0-final/RuoYi-Vue/ruoyi-ui"
    npm run dev

前端访问地址：

    http://localhost:1024

---

## 四、演示数据准备

### 1. 准备测试文件

测试文件路径示例：

    /Users/zhangnuo/Desktop/username.txt

文件内容示例：

    username is zhangnuo.
    This is an internal RAG test document.
    The document is used to test Milvus vector retrieval and permission filtering.

---

### 2. 上传文件到 RAG Server

    curl -i -X POST http://localhost:8081/rag/file/upload \
      -H "X-User-Id: 1" \
      -H "X-Username: admin" \
      -H "X-Group-Id: default" \
      -H "X-Group-Name: default" \
      -F "file=@/Users/zhangnuo/Desktop/username.txt" \
      -F "securityLevel=INTERNAL" \
      -F "scopeCode=INTERNAL"

预期返回：

    msg = 操作成功
    code = 200
    fileName = username.txt
    securityLevel = INTERNAL
    scopeCode = INTERNAL
    chunkCount = 1

---

## 五、检查 Milvus 入库结果

    curl -s "http://localhost:8081/rag/file/milvus/list?limit=10"

预期能够看到：

    scope_code = INTERNAL
    security_level = INTERNAL
    content 包含 username is zhangnuo

这一步用于证明文件已经完成：

    上传文件 -> RAG Server 接收 -> MariaDB 元数据 -> MinIO 原文件 -> Milvus 切块向量

---

## 六、直接测试 RAG Server 真实检索

    curl -i -X POST http://localhost:8081/rag/search \
      -H "Content-Type: application/json" \
      -d '{
        "query": "username",
        "topK": 5,
        "scopeCodes": ["INTERNAL"],
        "metadataFilter": "scope_code in [\"INTERNAL\"]"
      }'

预期返回：

    HTTP/1.1 200
    msg = 操作成功
    code = 200
    data 中包含 username is zhangnuo
    scopeCode = INTERNAL
    level = INTERNAL

这一步证明 RAG Server 的真实检索接口已经跑通。

---

## 七、通过若依前端测试真实检索

进入若依前端：

    http://localhost:1024

操作路径：

    RAG 检索测试 -> 选择 “RAG Server 真实检索” -> 输入 username -> 开始检索

重点观察：

1. 页面是否显示策略放行；
2. 检索模式是否为真实 RAG Server；
3. 候选结果是否来自 /rag/search；
4. 返回内容中是否包含 username is zhangnuo；
5. 二次过滤后是否仍有可返回文档；
6. 耗时、metadataFilter、scopeCodes 是否正常展示。

---

## 八、检查 RAG 检索审计日志

### 1. 前端页面查看

进入：

    系统管理 -> RAG 审计日志

重点查看字段：

1. 用户名；
2. 查询内容；
3. 用户组；
4. 可访问标签；
5. Metadata Filter；
6. 访问决策；
7. 耗时；
8. 创建时间。

其中访问决策字段约定为：

    1 = 放行
    0 = 拒绝

---

### 2. 数据库查看

    mysql -uroot -p -D ry-vue-320 -e "
    SELECT
      id,
      user_id,
      user_name,
      query_text,
      group_codes,
      scope_codes,
      metadata_filter,
      allow_access,
      deny_reasons,
      cost_time,
      create_time
    FROM sys_rag_audit_log
    ORDER BY id DESC
    LIMIT 10;
    "

预期结果中应看到：

    query_text = username
    allow_access = 1
    metadata_filter = scope_code in ["INTERNAL", "DOC_ADMIN"]

---

## 九、演示链路总结

当前完整链路为：

    用户登录若依平台
            ↓
    生成 PermissionContext 权限上下文
            ↓
    策略决策生成 Metadata Filter
            ↓
    useRemote=true 调用 RAG Server /rag/search
            ↓
    RAG Server 查询 Milvus 返回候选切块
            ↓
    平台侧执行二次权限过滤
            ↓
    返回安全检索结果
            ↓
    写入 RAG 检索审计日志

---

## 十、当前可展示成果

当前系统已经可以展示：

1. 权限治理基础能力；
2. RAG 文件入库；
3. MariaDB / MinIO / Milvus 三段式存储；
4. RAG Server 真实检索；
5. 平台侧远程检索适配；
6. Metadata Filter 生成；
7. 二次权限过滤；
8. 检索审计日志；
9. 首页 Dashboard 项目状态总览；
10. Git 分支与文档沉淀。

---

## 十一、后续建议

后续优先推进：

1. 固化更多测试文件，例如 INTERNAL、DOC_ADMIN、SECRET 等不同权限标签；
2. 测试普通用户与管理员用户的检索差异；
3. 完善文档权限标签自动回写；
4. 增强检索结果中的文件名、标题、摘要展示；
5. 评估是否将 feature/platform-remote-search 合并回 main；
6. 继续补充权限标签联动、检索异常兜底与演示脚本。
