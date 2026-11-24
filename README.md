# 向量库访问控制平台

基于 RuoYi-Vue 的身份权限与知识访问控制平台，包含管理端和普通用户知识门户。平台功能与业务流程见 [功能说明](doc/platform-overview.md)，开发实现见[系统开发文档](doc/向量库访问控制系统开发文档.docx)，安装升级步骤见[运行与升级](doc/platform-upgrade.md)。正式语义嵌入、本地回答和扫描 PDF 已在全新本机测试环境完成基础闭环，运行入口与验证边界见[本地 RAG 运行与验收记录](docs/本地测试环境准备.md)。这不代表生产部署、持续负载和跨存储一致性已完成最终验收。

## 本地快速启动

以下命令用于**已经准备好的本机测试工作区**，不是任意机器上的全自动安装器。当前目录布局为：

```text
/home/fufu/workspace/
├── .local-runtime/           # JDK、Maven、Node、日志及后台进程记录
├── models/                   # 生成模型及 bge-small-zh-v1.5
└── vllm-project/
    ├── .venv/                # vLLM、PyTorch、Transformers、FastAPI 等
    └── RAGproject/           # 本项目
```

在普通终端执行：

```bash
cd /home/fufu/workspace/vllm-project/RAGproject

# 启动容器、检查数据库并执行未应用的迁移，再启动模型及前后端
python3 scripts/local-stack.py start

# 检查容器与各服务入口
python3 scripts/local-stack.py status

# 停止本入口管理的进程和容器，保留数据库、原件与向量数据卷
python3 scripts/local-stack.py stop
```

启动入口要求 Docker Engine/Compose 可用，镜像已在本机、模型和构建工具已就位、后端 JAR 与前端依赖已安装。它使用 `--no-build --pull never`，不会自动下载镜像、模型或重新构建后端。首次迁移到其他机器时，需要先准备这些依赖并核对目录、连接配置和空库初始化；详见[本地运行记录](docs/本地测试环境准备.md)。

### 访问地址与测试账号

网页入口：[http://127.0.0.1:1024](http://127.0.0.1:1024)。当前 Java 服务、模型服务与容器映射端口均仅监听本机。

| 账号 | 本机测试身份 |
| --- | --- |
| `admin` | 管理后台；机密级，具有演示知悉用户组 |
| `test_user` | 普通知识门户；公开级 |
| `rag_internal` | 普通知识门户；内部级，无项目秘密权限 |
| `rag_project` | 普通知识门户；秘密级，项目 A 知悉组成员 |

上述已初始化测试账号的密码均为 `admin123`，登录验证码正常启用。测试身份初始化脚本为 `sql/local_rag_acceptance.sql`，仅适用于专用新测试库，不应对既有业务库执行。

| 服务 | 本机地址 |
| --- | --- |
| 前端 | `127.0.0.1:1024` |
| 平台后端 / 文件、OCR 与检索服务 | `127.0.0.1:8080` / `127.0.0.1:8081` |
| vLLM 生成接口 | `http://127.0.0.1:8000/v1`，模型别名 `rag-local` |
| CPU embedding 与 token 分块接口 | `http://127.0.0.1:8001/v1` |
| MariaDB / Redis | `127.0.0.1:3307` / `127.0.0.1:6380` |
| MinIO API / 控制台 | `127.0.0.1:9000` / `127.0.0.1:9001` |
| Milvus / 健康入口 | `127.0.0.1:19530` / `127.0.0.1:9091` |

本地容器编排使用 `docker-compose.local.yml`，数据保存在独立的 `rag-local` 卷中。连接与模型配置在 [平台配置](deploy/local/platform.yml)、[文件服务配置](deploy/local/rag.yml)；日志和进程记录位于 `/home/fufu/workspace/.local-runtime/`。历史 `docker-compose.rag.yml`、`start_all.sh` 不作为本轮启动入口。

## 当前模型方案

文档处理、embedding 和生成推理都在本机执行，不需要外部模型 API Key。当前 RTX 5080 有约 16 GB 显存，采用**一个 GPU 生成模型配合 CPU embedding**，避免多个生成模型同时常驻占用显存。

| 用途 | 模型 | 当前设置与状态 |
| --- | --- | --- |
| 默认生成 | `Qwen3-4B-Instruct-2507` | vLLM；上下文总长上限 8192 token；最多 4 个并行序列；显存预算 0.80；纯文本、eager 模式。已通过完整本地 RAG 闭环 |
| 正式语义向量 | `BAAI/bge-small-zh-v1.5` | CPU；512 维；CLS pooling 后 L2 归一化；Milvus 使用 COSINE 相似度 |
| 可选生成 | `Qwen3.5-9B-FP8` | 4096 上下文上限、单并发、显存预算 0.90；基础推理测试通过，尚未完成完整 RAG 的质量与持续负载对比 |
| 其他候选 | `Qwen3.5-4B`、`Qwen3.5-2B`、`Qwen2.5-1.5B-Instruct` | 保留作替代主力或低显存候选，当前未启用，不要求全部部署 |

文档与查询共用同一 embedding 模型，查询添加中文检索指令。文档按该模型的 tokenizer 切分为 **384 token、重叠 48 token** 的片段；embedding 接口最多每批 32 条，拒绝超过 512 token 的输入。正式向量写入独立集合 `rag_bge_small_zh_v1`，不混用原有的 128 维过渡向量。

本地配置显式启用 `rag.embedding.mode=http`。`fake` 模式仅保留历史演示用途，正式模式发生错误时不会自动回退到 fake。切换 embedding 模型时必须同步核对分块、维度和集合，并重新生成文档向量。

扫描 PDF 使用文件服务内置的 Tesseract OCR 与 `chi_sim+eng` 语言文件，识别结果经过切块后进入同一入库流程。问答仍通过平台 `/rag/search` 完成：检索前权限约束 → 语义检索 → 最新权限复核 → 授权片段生成回答 → 编号引用与审计。

### 切换到可选 9B 模型

先停止当前服务以释放 GPU，再启动：

```bash
python3 scripts/local-stack.py stop
MODEL_PATH=/home/fufu/workspace/models/Qwen3.5-9B-FP8 \
MAX_MODEL_LEN=4096 MAX_NUM_SEQS=1 GPU_MEMORY_UTILIZATION=0.90 \
python3 scripts/local-stack.py start
```

平台继续使用 `rag-local` 别名，无需改变调用地址；生成请求显式关闭 thinking。9B 已完成连续请求及 3417 输入 token 的基础测试，观察到整卡占用最高为 14268 MiB，包含桌面进程，不是持续采样得到的峰值。该配置作为可选模式，默认仍使用 4B。

恢复默认模型时，停止后在未设置上述覆盖变量的终端执行 `python3 scripts/local-stack.py start`。若此前曾通过 `export` 设置覆盖变量，先执行 `unset MODEL_PATH MAX_MODEL_LEN MAX_NUM_SEQS GPU_MEMORY_UTILIZATION`。

## 目录说明

| 目录 | 内容 |
| --- | --- |
| `ruoyi-admin`、`ruoyi-framework`、`ruoyi-system`、`ruoyi-common` | 平台接口、安全校验、业务实现与通用组件 |
| `ruoyi-ui` | 管理端与普通用户门户 |
| `ruoyi-quartz`、`ruoyi-generator` | 已纳入构建的定时任务与开发辅助模块 |
| `rag_server` | 文件解析、向量检索与模型对接服务 |
| `sql` | 基础库与历史增量迁移，执行前核对适用版本 |
| `doc` | 平台开发、功能、升级与部署说明 |
| `docs` | RAG 服务接口约定及联调资料 |
| `scripts`、`deploy`、`docker` | 启动检查、部署示例与配套配置 |

## 核心业务规则

| 文档类别 | 访问资格 |
| --- | --- |
| 公开 | 不要求人员密级和知悉用户组 |
| 内部 | 人员密级达到内部级或更高 |
| 秘密、机密 | 人员密级满足要求，且文档唯一知悉组属于人员所在的有效组集合 |

人员可以加入多个组，用户组只表示知悉范围。部门、平台角色与人员密级分别维护，管理员身份不代表拥有全部文档访问权限。人员状态、文档状态、个人访问时间、风险控制与已绑定的附加策略仍须通过校验。

附加策略支持访问时段、来源 IP 网段、人员风险等级及联合条件，可作用于全部文档或所选密级。组绑定将策略应用于组成员，文档范围由策略所选密级确定。命中策略按优先级裁决，数值越小越优先，同优先级拒绝优先；允许策略不突破人员密级、知悉组、账号和文档状态等基础条件。被明确绑定策略的管理员也执行对应规则，日期时间按服务端配置时区解释。

## 平台功能

- 人员资料、密级、知悉组及角色维护，人员权限统一保存；删除人员时，同一事务清理其角色、岗位和用户组关系，保留用户组及历史审计。
- 用户组负责人、成员与状态维护，停用组不参与有效知悉范围与组策略传递；存在引用时保护删除。负责人用于责任归属，不自动取得成员身份或文档权限。
- 策略、适用对象及版本记录，服务端验证条件并在事务内保存。
- 文件上传、上传人追踪、本人文件列表、文档标签与启停管理。
- 检索前权限约束、检索后按最新人员属性、有效组和文档标签复核候选，以及普通用户响应字段隔离。
- 请求审计、执行状态区分、行为告警处置、访问监控与真实 Excel 导出。
- 临时导出文件的归属验证与过期清理、IP 黑白名单，以及检索审计和告警摘要的 Syslog 导出与转发。

文件原件、元数据与向量切块分别由对象存储、MariaDB 和 Milvus 保存。停用不会物理删除原件或向量；完整物理删除与回收流程尚未提供。

## 构建与数据库迁移

当前本机已验证的工具为 JDK 8（平台）、JDK 17（文件服务）、Maven 3.9.16、Node.js 24.22.3 与 npm 11.19.1；推理环境为 vLLM 0.34.0、PyTorch 2.14.0+cu132、Transformers 5.22.2。数据库使用容器 MariaDB 10.11，平台和文件服务统一连接本机 `3307/ry-vue-320`，Redis 使用 `6380`。历史文档中的原生数据库 `3306` 和其他机器的版本记录不代表本轮运行目标。

代码变更后，先停止服务，再使用对应 Java 版本构建，避免替换正在运行的 JAR：

```bash
cd /home/fufu/workspace/vllm-project/RAGproject
python3 scripts/local-stack.py stop
source /home/fufu/workspace/.local-runtime/env.sh

# 平台使用 Java 8
mvn -pl ruoyi-admin -am -DskipTests package

# 文件服务使用 Java 17；子 shell 不影响平台 Java 环境
(
  export JAVA_HOME="$JAVA17_HOME"
  export PATH="$JAVA_HOME/bin:$PATH"
  mvn -f rag_server/pom.xml -DskipTests package
)

# 复用本机已生成的前端锁文件
(
  cd ruoyi-ui
  npm ci --legacy-peer-deps
  NODE_OPTIONS=--openssl-legacy-provider npm run build:prod
)

python3 scripts/local-stack.py start
```

前端锁文件目前仍受原仓库忽略规则影响；在没有本地锁文件的新机器上需要先安装依赖并生成锁文件。本轮后端构建跳过单元测试，端到端验证范围见下文。

全新本地库已完成基础初始化和 `scripts/migrate-local-db.py` 中的 14 项后续迁移，执行记录与文件 SHA256 存于 `rag_local_schema_migration`。已有数据卷不会自动重放容器初始化脚本，启动入口只会继续应用尚未记录的后续迁移。不要按文件名遍历执行整个 `sql` 目录。

MinIO 原有镜像不可公开拉取，本机改用官方固定源码 `RELEASE.2025-10-15T17-29-55Z` 编译的本地镜像。需重建时执行 `python3 scripts/prepare-local-minio.py`；源码校验摘要、镜像下载网络处理和恢复步骤见[本地运行记录](docs/本地测试环境准备.md)。

既有业务数据库的升级仍需先备份并核对迁移历史，不应直接套用本轮新测试库初始化。历史增量迁移包括：

1. `sql/add_rag_audit_query_indexes_20261029.sql`：审计频次与重复查询的组合索引。先检查同名索引定义，适用于本轮 MariaDB 环境。
2. `sql/add_access_time_scenarios_20261029.sql`：12 条常用时间策略及首次版本记录。需先完成历史策略版本表迁移；不覆盖既有同编码策略，不添加人员或用户组绑定。
3. `sql/add_group_manager_20261030.sql`：新增可空的用户组负责人编号。升级到本轮服务前先执行，保留原有组及成员关系，不自动指定负责人。
4. `sql/add_governance_export_permissions_20261030.sql`：登记访问监控、IP 黑白名单的独立 Excel 导出权限，不自动分配角色；执行前核对列表菜单及已有导出权限归属，步骤见运行与升级说明。

本地开发配置和示例账号不能直接作为生产部署配置。部署前须替换开发凭据、限制内部服务和数据服务的网络访问，并完成目标环境的备份恢复与联调验证。共享存储的持久化与跨存储恢复尚未完成部署验收，不能仅保留部分数据卷就认定知识库可以完整恢复。

后续账号安全、名称管理、策略格式和统计索引迁移见[运行与升级](doc/platform-upgrade.md)。

## 当前边界

已验证登录、网页扫描 PDF 上传与 OCR、正式语义检索、本地回答、编号引用和审计；公开、内部、项目秘密三类身份隔离，以及组成员关系撤销和文档停用后的权限复核均通过。完整停止／启动后，数据卷与服务正常恢复。

少量本地测试文档下，1／2／4 路完整 RAG 短请求约为 0.4～0.7 秒，跨四种身份的 8 路突发请求约为 0.31～0.60 秒。这些结果不是公司规模语料的持续压测、容量承诺或模型质量评测。

真实扫描件的模糊、倾斜、复杂表格与多栏效果，向量标签一致性、上传异常确认、持续负载及跨存储备份恢复仍需专项验证。完整测试条件与日志位置见[本地 RAG 运行与验收记录](docs/本地测试环境准备.md)。
