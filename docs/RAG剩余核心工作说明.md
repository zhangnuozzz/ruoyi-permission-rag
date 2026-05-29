# RAG 剩余核心工作说明

## 一、当前已完成内容

平台侧已经完成 RAG 安全检索外层链路：

1. 前端只访问若依后端，不直接访问 RAG Server；
2. 若依后端从登录态获取当前用户身份；
3. 平台侧构建权限上下文；
4. 平台侧执行策略决策；
5. 平台侧生成 metadataFilter；
6. 平台侧支持 useRemote=true 调用 RAG Server；
7. RAG Server 返回候选结果后，平台侧执行二次过滤；
8. 平台侧记录 RAG 检索审计；
9. 审计字段 allow_access 已统一为 1 表示放行，0 表示拒绝。

RAG Server 侧已经完成第一轮知识库链路：

1. 文件上传；
2. 文本解析与切块；
3. MinIO 原文件备份；
4. MariaDB 文件元数据写入；
5. Milvus 向量数据写入；
6. /rag/search 候选片段检索；
7. 与平台侧 useRemote=true 联调通过。

## 二、当前未完成内容

正式交付仍缺少两个核心能力。

### 1. 正式 Embedding

当前 RAG Server 代码中仍存在 textVector() 伪向量逻辑。该逻辑可以用于早期联调，但不能作为最终正式向量化方案。

正式版本需要完成：

1. 入库时对每个 chunk 调用正式 Embedding 模型；
2. 查询时对 query 调用同一个 Embedding 模型；
3. 入库向量和查询向量维度一致；
4. Milvus collection 维度与 Embedding 模型输出维度一致；
5. textVector() 不再作为主链路使用；
6. application.yml 中提供 Embedding 模型配置；
7. Embedding 调用失败时有明确异常和日志。

### 2. AI 生成回答

当前 /rag/search 已能返回候选片段，但最终 answer 生成链路尚未落地。

正式版本需要完成：

1. 将权限过滤后的 filteredResults 作为模型上下文；
2. 只允许授权片段进入模型输入；
3. 未授权片段不得进入 LiteLLM 上下文；
4. 调用本地 LiteLLM OpenAI-compatible 接口；
5. 返回 answer 字段；
6. 返回 answer 的同时保留 rawResults、filteredResults、rejectedResults；
7. 模型调用失败时保留检索结果返回，并给出明确错误信息；
8. 审计日志中记录模型生成耗时和检索总耗时。

## 三、责任边界

平台侧负责：

1. 登录态身份解析；
2. 权限上下文生成；
3. 策略决策；
4. metadataFilter 生成；
5. RAG Server 调用；
6. 二次过滤；
7. 审计日志；
8. 前端展示；
9. 演示流程和验收文档。

RAG Server 侧负责：

1. 文件解析；
2. 文本切块；
3. MinIO 存储；
4. MariaDB 元数据；
5. 正式 Embedding；
6. Milvus 向量写入；
7. Milvus metadata filtering；
8. 候选片段召回；
9. LiteLLM 生成回答；
10. 返回 answer 和检索结果。

## 四、下一阶段验收标准

下一阶段必须跑通以下完整链路：

上传文件
→ 正式 Embedding 生成 chunk 向量
→ Milvus 写入
→ 用户检索
→ query 正式 Embedding
→ Milvus metadata filtering
→ 返回候选片段
→ 平台侧二次过滤
→ 授权片段进入 LiteLLM
→ 生成 answer
→ 前端展示 answer 和引用片段
→ 审计日志记录完整过程

## 五、当前结论

当前项目不是最终完成状态。

目前完成的是权限控制 RAG 的外层平台链路和第一轮知识库检索联调。

正式交付前，还必须补齐正式 Embedding 和 AI 生成回答链路。
