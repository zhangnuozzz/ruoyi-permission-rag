# RAG 检索联调接口约定

## 一、目标

用于约定平台侧与 RAG Server 之间的真实检索接口输入输出格式，减少后续联调返工。

当前平台侧已完成：

- 当前登录用户权限上下文构建
- groupCodes / scopeCodes 汇总
- Metadata Filter 生成
- 平台侧二次权限过滤
- RAG 检索审计日志记录与展示

后续 RAG Server 完成真实检索接口后，平台侧将以本文档为基础完成联调接入。

---

## 二、建议接口

### 1. 检索接口路径

POST /rag/search

### 2. 请求头

Content-Type: application/json

### 3. 请求体建议

```json
{
  "query": "查询内部研发制度",
  "topK": 5,
  "userContext": {
    "userId": 1,
    "userName": "admin",
    "admin": true,
    "groupCodes": ["GROUP_RD_01", "GROUP_DOC_ADMIN"],
    "scopeCodes": ["INTERNAL", "DOC_ADMIN"]
  },
  "metadataFilter": "scope_code in [\\"INTERNAL\\", \\"DOC_ADMIN\\"]",
  "platformFilterMode": "metadata_filter_and_second_filter"
}
```

### 4. 字段说明

- query：用户检索问题
- topK：期望召回条数
- userContext：平台侧构建的权限上下文
- metadataFilter：平台侧生成的检索前过滤表达式
- platformFilterMode：平台侧过滤模式说明

---

## 三、建议返回体

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "query": "查询内部研发制度",
    "costTime": 123,
    "rawResultCount": 5,
    "results": [
      {
        "docId": "DOC-001",
        "title": "内部研发制度",
        "content": "......",
        "scopeCode": "INTERNAL",
        "level": "INTERNAL",
        "score": 0.92,
        "chunkId": "chunk-001",
        "fileId": "file-001"
      }
    ]
  }
}
```

### 返回字段建议

- code：状态码
- msg：说明
- data.query：检索问题
- data.costTime：RAG Server 侧耗时
- data.rawResultCount：原始召回结果数量
- data.results：候选结果列表

### 结果项字段建议

- docId：文档ID
- title：文档标题
- content：召回文本内容
- scopeCode：文档权限标签
- level：文档密级
- score：相似度得分
- chunkId：切块ID
- fileId：文件ID

---

## 四、平台侧接入方式

平台侧接入真实接口后将执行以下流程：

当前用户
→ 构建 PermissionContext
→ 生成 Metadata Filter
→ 调用 RAG Server 真实检索接口
→ 获取候选结果
→ 平台侧执行二次权限过滤
→ 写入 RAG 检索审计日志
→ 返回最终允许访问结果

---

## 五、联调重点

1. RAG Server 是否支持 metadataFilter
2. RAG Server 返回结果中是否包含 scopeCode / level
3. 返回结果是否能定位到文档和切块
4. 召回结果数量与平台侧最终过滤结果数量是否可区分
5. 返回异常时是否有明确错误码和说明

---

## 六、当前约定结论

在真实接口完成前，平台侧继续使用 sys_rag_doc 模拟候选结果进行演示。

真实接口完成后，仅替换候选结果来源，平台侧权限上下文、二次过滤、审计日志逻辑保持不变。
