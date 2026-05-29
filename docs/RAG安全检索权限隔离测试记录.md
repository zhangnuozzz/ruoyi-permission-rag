# RAG 安全检索权限隔离测试记录

## 一、测试目的

本次测试用于验证权限控制 RAG 链路中的权限隔离效果。

重点验证：

1. 平台侧能根据当前登录用户生成权限上下文；
2. 平台侧能根据权限上下文生成 metadataFilter；
3. RAG Server 真实检索时会接收带权限过滤条件的安全检索请求；
4. 平台侧会对候选结果执行二次过滤；
5. 检索行为会写入 RAG 检索审计日志；
6. 管理员用户与普通受限用户查询同一问题时，返回结果不同。

## 二、测试用户

### 1. 管理员用户

用户：admin

权限特点：

- admin = true
- 可访问标签包含 INTERNAL、DOC_ADMIN
- 查询 username 时可以返回 INTERNAL 文档

### 2. 受限用户

用户：test_user

数据库配置：

- user_id = 1001
- user_name = test_user
- nick_name = 受限演示用户
- 用户组 = GROUP_PUBLIC
- 用户组名称 = 公开文档组
- scope_code = PUBLIC
- 策略 = RAG_PUBLIC_READ

## 三、数据库配置记录

### 1. test_user 用户信息

SQL：

SELECT user_id, user_name, nick_name, status
FROM sys_user
WHERE user_name = 'test_user';

结果：

- user_id = 1001
- user_name = test_user
- nick_name = 受限演示用户
- status = 0

### 2. test_user 用户组

SQL：

SELECT 
    u.user_id,
    u.user_name,
    g.id AS group_id,
    g.group_code,
    g.group_name,
    g.scope_code,
    g.status
FROM sys_user u
LEFT JOIN sys_user_group_rel r ON u.user_id = r.user_id
LEFT JOIN sys_group g ON r.group_id = g.id
WHERE u.user_name = 'test_user';

结果：

- user_id = 1001
- user_name = test_user
- group_id = 4
- group_code = GROUP_PUBLIC
- group_name = 公开文档组
- scope_code = PUBLIC
- status = 0

### 3. test_user 策略绑定

SQL：

SELECT 
    u.user_id,
    u.user_name,
    g.id AS group_id,
    g.group_code,
    g.group_name,
    g.scope_code,
    p.id AS policy_id,
    p.policy_code,
    p.policy_name,
    p.effect,
    p.status AS policy_status,
    b.status AS bind_status
FROM sys_user u
LEFT JOIN sys_user_group_rel r ON u.user_id = r.user_id
LEFT JOIN sys_group g ON r.group_id = g.id
LEFT JOIN sys_policy_bind b ON g.id = b.bind_target_id
LEFT JOIN sys_policy p ON b.policy_id = p.id
WHERE u.user_name = 'test_user';

结果：

- user_id = 1001
- user_name = test_user
- group_code = GROUP_PUBLIC
- scope_code = PUBLIC
- policy_code = RAG_PUBLIC_READ
- policy_name = 公开文档读取策略
- effect = 0
- policy_status = 0
- bind_status = 0

## 四、测试步骤与结果

### 1. 管理员检索

登录用户：admin

检索页面：RAG 安全检索测试

检索模式：RAG Server 真实检索

检索问题：username

实际现象：

- admin 具备 INTERNAL、DOC_ADMIN 等访问标签；
- metadataFilter 包含 INTERNAL、DOC_ADMIN；
- 检索 username 后返回了 INTERNAL 文档片段；
- 文档内容包含 username is zhangnuo；
- 平台侧二次过滤通过；
- RAG 检索审计日志记录本次请求。

### 2. 受限用户检索

登录用户：test_user

检索页面：RAG 安全检索测试

检索模式：RAG Server 真实检索

检索问题：username

实际权限上下文：

- 当前用户 = test_user
- 用户 ID = 1001
- admin = false
- 用户组编码 = GROUP_PUBLIC
- 可访问标签 = PUBLIC

生成的 Metadata Filter：

scope_code in ["PUBLIC"]

实际检索结果：

- 候选结果 = 0 条
- 过滤后文档结果 = 0 条
- 被权限过滤拦截的候选结果 = 0 条
- 原始候选结果 = 0 条

AI 回答区域：

- 外部模型未启用；
- 授权片段数量为 0；
- 页面提示当前仅展示授权检索片段。

审计日志记录：

- 用户 = test_user
- 用户 ID = 1001
- 可访问标签 = PUBLIC
- Metadata Filter = scope_code in ["PUBLIC"]
- 访问决策 = 放行
- 耗时 = 67 ms

## 五、测试结论

本次测试证明：

1. 平台侧能够根据登录用户实时加载权限上下文；
2. 管理员用户与普通受限用户会生成不同的可访问标签集合；
3. 普通用户 test_user 只具备 PUBLIC 访问范围；
4. test_user 查询 username 时，平台侧生成的安全检索条件为 scope_code in ["PUBLIC"]；
5. INTERNAL 文档不会返回给 test_user；
6. admin 查询同一问题时可以返回 INTERNAL 文档；
7. RAG 检索审计日志能够记录用户身份、访问标签、metadataFilter、访问决策与耗时。

当前系统已经具备权限控制 RAG 的基础安全检索闭环：

用户登录
→ 权限上下文加载
→ 策略决策
→ Metadata Filter 生成
→ RAG Server 真实检索
→ 平台侧二次过滤
→ 前端展示
→ 审计日志留痕

## 六、后续工作

后续仍需继续完善：

1. 接入正式 Embedding 模型，替换当前 RAG Server 中的 textVector 伪向量逻辑；
2. 接入外部或本地大模型，生成最终 answer；
3. 固化更多测试文档，例如 PUBLIC、INTERNAL、DOC_ADMIN、SECRET；
4. 验证不同用户组之间的检索差异；
5. 将模型生成耗时、授权片段数量、引用片段等信息进一步写入审计日志；
6. 整理完整演示脚本和验收材料。
