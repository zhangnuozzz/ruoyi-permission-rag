# RAG 正式 Embedding 与 AI 生成待办说明

## 一、阶段结论

截至目前，权限控制 RAG 平台已经完成平台侧安全检索链路与 RAG Server 真实检索接口的第一轮联调。

已完成内容如下：

1. 若依平台提供 RAG 文件上传入口；
2. 若依后端代理调用 RAG Server；
3. RAG Server 完成文件入库；
4. MariaDB 写入文件元数据；
5. MinIO 保存原始文件对象；
6. Milvus 写入文档切块和向量字段；
7. RAG Server 提供 `/rag/search` 检索接口；
8. 若依平台通过 `useRemote=true` 调用 RAG Server；
9. 平台侧根据当前登录用户生成权限上下文；
10. 平台侧根据权限上下文生成 `metadataFilter`；
11. 平台侧对 RAG Server 返回的候选结果进行二次权限过滤；
12. 平台侧写入 `sys_rag_audit_log` 检索审计日志；
13. 前端 RAG 安全检索页面支持平台 Mock 检索和 RAG Server 真实检索两种模式；
14. RAG 检索审计日志中 `allow_access` 字段语义已统一为：`1` 表示放行，`0` 表示拒绝。

目前项目已经具备端到端演示能力：

文件上传 → RAG Server 入库 → MariaDB / MinIO / Milvus 存储 → RAG Server 真实检索 → 若依平台远程调用 → 权限二次过滤 → 审计留痕 → 前端展示。

---

## 二、当前边界

当前链路已经跑通，但还没有完成正式大模型能力接入。

### 1. 当前向量生成不是正式 Embedding 模型

RAG Server 中 Milvus 向量字段目前由 `MilvusChunkStoreService` 中的 `textVector(chunk.getContent())` 生成。

该逻辑属于本地链路验证用的伪向量生成方式，能够支撑工程流程跑通，但不等同于正式语义 Embedding 模型。

当前已经验证的是：

- 文件能够完成入库；
- 文档能够完成切块；
- 切块能够写入 Milvus；
- 检索接口能够返回候选切块；
- 平台侧能够完成权限过滤和审计。

当前尚未验证的是：

- 正式 Embedding 模型的语义召回效果；
- query 和文档 chunk 在同一语义向量空间中的相似度检索效果；
- 多文档、多标签、多权限场景下的真实语义检索质量。

### 2. 当前 `/rag/search` 返回的是候选切块

当前 `/rag/search` 主要返回检索命中的文档切块，包括：

- `docId`
- `chunkId`
- `content`
- `summary`
- `scopeCode`
- `level`
- `score`

该接口用于验证向量检索和权限过滤链路。

当前还没有完成完整问答链路：

检索候选切块 → 拼接上下文 → 构造 Prompt → 调用大语言模型 → 生成自然语言回答 → 返回 answer 和 sources。

### 3. 当前还没有 `/rag/ask`

当前已经具备 `/rag/search`，但还没有面向问答的 `/rag/ask`。

后续完整 RAG 问答接口应包含：

1. 接收用户问题；
2. 根据用户权限生成检索约束；
3. 执行向量检索；
4. 获取候选切块；
5. 按权限规则过滤候选结果；
6. 拼接上下文；
7. 调用大语言模型；
8. 生成回答；
9. 返回回答内容和引用来源；
10. 写入检索与问答审计日志。

---

## 三、RAG Server 后续任务

RAG Server 侧继续补齐模型能力，重点包括正式 Embedding、语义检索和 AI 生成回答。

### 1. 抽象 EmbeddingService

新增统一 Embedding 接口：

    public interface EmbeddingService {
        List<Float> embed(String text);
    }

文档入库和用户查询都统一调用该接口生成向量。

这样可以保证：

- 文档 chunk 使用统一向量模型；
- 用户 query 使用同一个向量模型；
- 后续切换模型时不影响 Milvus 存储和检索主流程；
- fake embedding 和正式 embedding 可以通过配置切换。

### 2. 保留 fake embedding 模式

当前 `textVector` 逻辑保留为本地演示模式。

后续可整理为：

    FakeEmbeddingServiceImpl

配置示例：

    rag:
      embedding:
        mode: fake

该模式用于无 API Key、无外部模型服务时的本地演示，保证入库、检索、权限过滤、审计链路仍然能够运行。

### 3. 接入正式 Embedding 模型

正式模型模式配置示例：

    rag:
      embedding:
        mode: real
        provider: dashscope
        model: text-embedding-v4
        api-key: ${DASHSCOPE_API_KEY}

也可以替换为本地模型、Ollama、OpenAI-compatible 接口或其他国产 Embedding 模型。

正式接入时需要处理：

1. 文档入库时，chunk content 调用 Embedding 模型生成向量；
2. 用户检索时，query 调用同一个 Embedding 模型生成向量；
3. Milvus collection 的 vector dimension 与模型输出维度一致；
4. fake embedding 产生的旧数据需要清空后重新入库；
5. 向量检索结果需要返回 score、chunkId、docId、content、scopeCode、level 等字段。

### 4. 完成 Query Embedding + Milvus Search

完整检索流程如下：

    query text
    → EmbeddingService.embed(query)
    → Milvus vector search
    → metadata filter 过滤
    → 返回 topK chunks
    → 平台侧二次权限过滤
    → 前端展示检索结果
    → 审计日志记录

### 5. 增加 `/rag/ask`

新增接口：

    POST /rag/ask

请求示例：

    {
      "query": "username 是什么？",
      "topK": 5,
      "scopeCodes": ["INTERNAL"],
      "metadataFilter": "scope_code in [\"INTERNAL\"]"
    }

返回示例：

    {
      "code": 200,
      "msg": "操作成功",
      "data": {
        "answer": "根据内部测试文档，username 是 zhangnuo。",
        "sources": [
          {
            "docId": "xxx",
            "chunkId": "xxx-1",
            "content": "username is zhangnuo.",
            "scopeCode": "INTERNAL",
            "level": "INTERNAL",
            "score": 19.97
          }
        ]
      }
    }

---

## 四、平台侧后续任务

平台侧继续围绕权限控制、安全过滤、审计留痕和前端展示推进。

### 1. 保留平台侧二次过滤

即使 RAG Server 已经支持 `metadataFilter`，平台侧仍然保留二次过滤。

原因如下：

1. RAG Server 的 metadata filter 属于检索前置约束；
2. 平台侧二次过滤属于最终安全兜底；
3. 审计日志需要记录最终允许返回给用户的结果；
4. 权限控制平台的核心价值体现在“检索前约束 + 返回前过滤 + 全链路审计”。

### 2. 适配 `/rag/ask`

RAG Server 增加 `/rag/ask` 后，平台侧新增 AI 问答模式。

前端展示内容包括：

- 用户问题；
- AI 生成回答；
- 引用来源；
- 权限上下文；
- `metadataFilter`；
- 候选结果；
- 被过滤结果；
- 审计记录。

### 3. 完善检索页面模式区分

RAG 安全检索页面区分三种模式：

1. 平台 Mock 检索：从 `sys_rag_doc` 读取模拟候选结果，用于验证平台侧权限过滤；
2. RAG Server 真实检索：调用 `/rag/search`，返回真实候选切块；
3. AI 问答生成：调用 `/rag/ask`，返回自然语言答案和引用来源。

### 4. 扩展审计字段

后续扩展 `sys_rag_audit_log`，增加以下字段：

    search_mode
    rag_server_status
    raw_result_count
    filtered_result_count
    rejected_result_count
    answer_generated
    model_name

用于记录检索模式、远程服务状态、候选结果数量、过滤结果数量、AI 回答状态和模型名称。

---

## 五、项目状态表述

本阶段已经完成权限控制 RAG 平台的核心工程链路。

文件可以通过若依平台上传至 RAG Server，并完成 MariaDB、MinIO、Milvus 三段式存储。RAG Server 已提供真实 `/rag/search` 检索接口。若依平台侧已完成远程检索调用、权限上下文构建、`metadataFilter` 生成、二次权限过滤、检索审计和前端展示。

当前系统已经具备端到端演示能力。

下一阶段工作重点是将 RAG Server 中用于链路验证的伪向量生成逻辑替换为正式 Embedding 模型，并接入大语言模型生成回答，形成完整的“权限约束检索 + 安全过滤 + AI 问答 + 审计追踪”闭环。

---

## 六、分工

### 平台侧

负责人：zhangnuo

工作内容：

1. 若依平台侧权限治理；
2. 用户组、策略、策略绑定；
3. 权限上下文生成；
4. `metadataFilter` 生成与传递；
5. 平台侧二次权限过滤；
6. RAG 检索审计；
7. 前端检索页面；
8. 审计日志页面；
9. Dashboard 总览页；
10. 项目文档、演示流程和分支管理。

### RAG Server 侧

负责人：fufu

工作内容：

1. RAG Server 文件解析；
2. 文档切块；
3. MinIO 原文件存储；
4. MariaDB 文件元数据写入；
5. Milvus 向量写入和查询；
6. EmbeddingService 抽象；
7. 正式 Embedding 模型接入；
8. `/rag/search` 检索质量优化；
9. `/rag/ask` AI 生成回答接口；
10. answer + sources 返回结构。

---

## 七、后续优先级

### P0：正式 Embedding 接入

1. 明确当前 `textVector` 为 fake embedding；
2. 新增 `EmbeddingService`；
3. 文档入库和 query 检索统一使用 `EmbeddingService`；
4. 保留 fake mode；
5. 接入正式 Embedding 模型；
6. 清空旧 fake 向量数据并重新入库；
7. 验证正式语义检索效果。

### P1：AI 问答生成

1. 新增 `/rag/ask`；
2. 检索候选切块；
3. 拼接上下文；
4. 调用大语言模型；
5. 返回 `answer + sources`；
6. 平台侧展示 AI 回答；
7. 审计日志记录问答状态。

### P2：演示与工程增强

1. 固化 INTERNAL、DOC_ADMIN、SECRET 等不同权限标签测试文件；
2. 测试普通用户与管理员用户检索差异；
3. 完善文档权限标签自动回写；
4. 优化检索结果中的文件名、标题和摘要展示；
5. 增强检索异常兜底；
6. 完善启动脚本和演示脚本；
7. 评估将 `feature/platform-remote-search` 合并回 `main`。
