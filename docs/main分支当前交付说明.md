# main 分支当前交付说明

## 一、当前分支状态

当前稳定主线分支为 main。

平台侧远程 RAG 检索联调分支 feature/platform-remote-search 已经合并回 main。

合并提交：

566906bf merge: integrate platform remote RAG search into main

合并后已确认：

local main  = 566906bfc3b11dda201e94b55ddc474586962524
origin main = 566906bfc3b11dda201e94b55ddc474586962524
ragold main = 566906bfc3b11dda201e94b55ddc474586962524

说明本地 main、个人仓库 origin/main、team 仓库 ragold/main 三者已经一致。

## 二、main 分支当前包含的内容

### 1. 权限治理模块

已包含：

1. 用户组管理；
2. 策略管理；
3. 策略绑定管理；
4. 用户与用户组关系；
5. 用户组知悉范围；
6. 策略与用户组绑定；
7. 基于用户身份加载权限上下文。

相关表包括：

sys_group
sys_user_group_rel
sys_policy
sys_policy_bind

### 2. RAG 文件入库平台入口

已包含平台侧文件上传入口：

POST /rag/file/upload

平台侧负责：

1. 接收前端上传请求；
2. 获取当前登录用户身份；
3. 补充用户上下文；
4. 代理转发到 RAG Server；
5. 接收 RAG Server 返回的文件入库结果；
6. 将文档权限标签同步回平台侧；
7. 支撑后续检索、二次过滤和审计解释。

### 3. RAG 检索平台入口

已包含平台侧检索入口：

POST /rag/search

平台侧检索链路包括：

1. 登录态身份解析；
2. 用户权限上下文加载；
3. 用户组和策略加载；
4. 权限决策；
5. 生成 metadataFilter；
6. 调用 RAG Server；
7. 接收候选片段；
8. 平台侧二次过滤；
9. 生成检索审计信息；
10. 返回前端展示。

### 4. Mock / Remote 双模式

当前检索支持两种模式：

useRemote=false：走平台侧 mock 检索，用于演示权限过滤闭环；
useRemote=true ：调用 RAG Server 的 /rag/search，用于真实知识库联调。

这样可以保证 RAG Server 未启动时，平台侧仍能演示权限治理、策略决策和二次过滤；RAG Server 启动后，可以切换到真实知识库检索。

### 5. 外部模型回答生成演示

当前平台侧已增加外部模型回答生成演示能力。

已新增：

IRagAnswerService
RagAnswerServiceImpl

前端 RAG 检索页面已增加“AI 生成回答”展示区域。

该能力用于演示 RAG 检索结果进入模型回答生成环节。模型调用失败时保留检索结果，不影响安全检索主链路。

这部分属于平台侧演示接入，不等价于完整正式 RAG 生成链路。

### 6. 审计与展示修复

已完成：

1. RAG 审计记录展示修复；
2. allowAccess 字段显示修复；
3. 检索结果过滤前后数量展示；
4. 拒绝结果展示；
5. Dashboard 首页改造成权限控制 RAG 平台总览页；
6. 隐藏若依默认官网菜单；
7. 若依模板痕迹减少。

### 7. 安全检索权限隔离测试

已补充 test_user 权限隔离测试数据 SQL：

sql/rag_permission_isolation_test_user_20260529.sql

该 SQL 用于构造一个仅具备 PUBLIC 权限的普通测试用户。

测试目标：

1. 管理员可检索更多权限范围文档；
2. test_user 只能检索 PUBLIC 范围；
3. INTERNAL、DOC_ADMIN、SECRET 等非授权文档不能返回给 test_user；
4. 即使 RAG Server 返回了非授权候选片段，平台侧二次过滤也必须拦截；
5. 审计日志能记录允许、拒绝、过滤数量和原因。

## 三、main 分支暂未包含的内容

当前 main 还没有完整合并 fufu_week4 中的 rag_server 最新实现。

main 当前主要是平台侧稳定成果；
fufu_week4 中的 rag_server 仍需要后续单独整理和合并。

不要直接随手把 fufu_week4 合并进 main。

后续如果要合并 RAG Server，应单独开一次整理流程：

1. 对比 main 与 fufu_week4；
2. 只迁移需要的 rag_server 内容；
3. 排除 target、临时文件、IDE 文件；
4. 检查配置文件中的数据库密码、MinIO 密钥等敏感信息；
5. 单独构建 RAG Server；
6. 联调若依平台 8080 与 RAG Server 8081；
7. 再提交合并。

## 四、本地启动方式

### 1. 启动基础依赖

需要保证以下服务可用：

MariaDB：3306
Redis：6379
MinIO：9000 / 9001
Milvus：19530
RAG Server：8081
若依后端：8080
若依前端：通常为 80 或 1024，按本地配置为准

### 2. 启动若依后端

在项目根目录执行：

cd "/Users/zhangnuo/Desktop/大模型向量库项目/RuoYi-3.2.0-final/RuoYi-Vue"
mvn -pl ruoyi-admin spring-boot:run

后端正常启动后，端口为：

http://localhost:8080

### 3. 启动若依前端

另开一个终端：

cd "/Users/zhangnuo/Desktop/大模型向量库项目/RuoYi-3.2.0-final/RuoYi-Vue/ruoyi-ui"
npm run dev

### 4. 启动 RAG Server

RAG Server 目前在 fufu 的分支或独立目录中。

常见路径示例：

cd "/Users/zhangnuo/Desktop/大模型向量库项目/fufu-RAGproject/rag_server"
mvn spring-boot:run

RAG Server 正常启动后，端口为：

http://localhost:8081

## 五、演示流程

### 1. 平台侧权限治理演示

进入若依后台后，可以演示：

1. 用户组管理；
2. 策略管理；
3. 策略绑定管理；
4. 用户与用户组关系；
5. test_user 仅绑定 PUBLIC 权限；
6. 管理员具备更高权限。

### 2. RAG 文件入库演示

进入 RAG 文件入库页面，上传测试文件。

建议准备不同权限标签的文件：

PUBLIC：公开文档
INTERNAL：内部文档
DOC_ADMIN：文档管理员可见文档
SECRET：高密级文档

上传后检查：

1. 前端提示上传成功；
2. 平台侧有文件记录；
3. RAG Server 处理成功；
4. MariaDB 有文件元数据；
5. MinIO 有原文件对象；
6. Milvus 有切块向量记录。

### 3. RAG 检索演示

进入安全检索页面。

管理员账号测试：

查询 PUBLIC 文档：应返回
查询 INTERNAL 文档：应返回
查询 DOC_ADMIN 文档：视管理员权限返回
查询 SECRET 文档：视权限配置返回

test_user 测试：

查询 PUBLIC 文档：应允许返回
查询 INTERNAL 文档：应被过滤或无结果
查询 DOC_ADMIN 文档：应被过滤或无结果
查询 SECRET 文档：应被过滤或无结果

重点看页面上的：

allowAccess
metadataFilter
rawResultCount
filteredResultCount
rejectedResultCount
filteredResults
rejectedResults
AI 生成回答

### 4. 审计演示

检索后查看 RAG 审计或访问监控相关页面。

重点确认：

1. 用户名；
2. 查询内容；
3. 是否允许访问；
4. 权限过滤条件；
5. 原始结果数量；
6. 过滤后结果数量；
7. 拒绝结果数量；
8. 拒绝原因；
9. 耗时信息。

## 六、已经通过的构建检查

合并前和合并后均执行过：

mvn clean install -DskipTests

结果：

BUILD SUCCESS

合并前和合并后均执行过：

cd ruoyi-ui
npm run build:prod

结果：

DONE Build complete

前端构建中出现的 asset size warning 属于 Vue 打包体积提示，不影响构建成功。

## 七、仍需完成的工作

### 1. RAG Server 正式 Embedding

当前 RAG Server 中仍存在 textVector 伪向量逻辑。

后续必须替换为正式 Embedding 模型。

要求：

1. 文档切块后调用正式 Embedding；
2. query 检索时调用同一个 Embedding 模型；
3. 向量维度和 Milvus collection 配置一致；
4. 老的伪向量数据需要清理或重建；
5. 检索结果需要验证语义相关性。

### 2. Milvus Metadata Filtering

RAG Server 检索时必须将平台传来的 metadataFilter 应用到 Milvus 检索条件中。

不能只先查全量结果再由业务代码过滤。

平台侧二次过滤是兜底，RAG Server 的向量数据库过滤是第一道安全边界。

### 3. LiteLLM / 外部模型正式回答

当前平台侧已经有外部模型回答生成演示能力，但完整交付还需要：

1. 明确模型服务地址；
2. 明确模型名称；
3. 明确请求格式；
4. 只把授权片段交给模型；
5. 模型失败时保留检索结果；
6. answer、引用片段、耗时进入前端展示和审计记录。

### 4. fufu_week4 合并整理

fufu 的 fufu_week4 中包含 RAG Server 相关代码，后续需要单独整理进入主线。

整理时注意：

1. 不提交 target/；
2. 不提交真实数据库密码；
3. 不提交 MinIO 真实密钥；
4. 不破坏当前 main 的平台侧代码；
5. 合并后重新跑后端、前端、RAG Server 构建；
6. 重新验证上传、检索、权限过滤和审计闭环。

### 5. 验收材料

后续需要整理：

1. 项目部署说明；
2. 演示账号说明；
3. 测试数据说明；
4. 安全检索流程图；
5. 权限模型说明；
6. RAG 入库流程说明；
7. RAG 检索流程说明；
8. 行为审计说明；
9. 已完成功能清单；
10. 未完成功能与风险说明。

## 八、当前结论

当前 main 已经具备平台侧权限控制 RAG 的稳定演示基础。

已完成：

1. 权限治理基础模块；
2. 文件入库平台入口；
3. 安全检索平台入口；
4. mock / remote 双模式；
5. RAG Server 远程检索适配；
6. 平台侧二次过滤；
7. 检索审计展示；
8. 外部模型回答生成演示；
9. Dashboard 改造；
10. 权限隔离测试记录；
11. test_user PUBLIC 权限隔离测试 SQL；
12. main 分支合并与双仓库同步。

仍未完成：

1. RAG Server 正式 Embedding；
2. RAG Server 侧 Milvus metadata filtering 的完整验收；
3. LiteLLM / 外部模型正式生成链路；
4. fufu_week4 中 RAG Server 的主线整理；
5. 更完整的多权限标签测试数据和最终验收材料。
