<template>
  <div class="app-container rag-search-page">
    <el-card shadow="never" class="box-card">
      <div slot="header" class="card-header">
        <div>
          <div class="page-title">RAG 安全检索测试</div>
          <div class="page-subtitle">
            支持平台 Mock 检索与 RAG Server 真实检索联调，验证“权限上下文 → Milvus 粗过滤 → 平台二次过滤 → 审计留痕 → AI 回答”的完整安全链路。
          </div>
        </div>
        <el-tag :type="form.useRemote ? 'success' : 'warning'" effect="plain">
          {{ form.useRemote ? '真实 RAG Server 检索' : '平台 Mock 检索' }}
        </el-tag>
      </div>

      <el-alert
        title="链路定位：用户问题 → 查询安全上下文 → Milvus 兼容粗过滤 → 真实向量召回 → 平台精细二次过滤 → 审计日志与行为分析"
        type="info"
        :closable="false"
        show-icon
        class="tips-alert"
      />

      <el-form :model="form" label-width="100px" class="search-form">
        <el-form-item label="检索模式">
          <el-radio-group v-model="form.useRemote">
            <el-radio-button :label="false">平台 Mock 检索</el-radio-button>
            <el-radio-button :label="true">RAG Server 真实检索</el-radio-button>
          </el-radio-group>
          <span class="mode-tip">
            {{ form.useRemote ? '调用 localhost:8081/rag/search' : '读取 sys_rag_doc 模拟候选集' }}
          </span>
        </el-form-item>

        <el-form-item label="TopK">
          <el-input-number v-model="form.topK" :min="1" :max="20" />
        </el-form-item>

        <el-form-item label="检索问题">
          <el-input
            v-model="form.query"
            type="textarea"
            :rows="4"
            placeholder="请输入检索问题，例如：username"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="loading" icon="el-icon-search" @click="handleSearch">
            开始检索
          </el-button>
          <el-button icon="el-icon-refresh" @click="reset">
            重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card v-if="result && result.allowAccess === false" shadow="never" class="box-card">
      <div slot="header" class="section-header">
        <span class="section-title">查询安全拦截诊断</span>
        <el-tag type="danger" effect="plain">{{ result.searchMode || 'blocked' }}</el-tag>
      </div>

      <el-alert
        :title="result.message || result.decisionMessage || '本次查询被安全策略拦截'"
        type="warning"
        :closable="false"
        show-icon
        class="tips-alert"
      />

      <el-row :gutter="12" class="context-row">
        <el-col :span="8">
          <div class="context-card">
            <div class="context-label">风险分</div>
            <div class="context-value">{{ result.riskScore || getSecureValue('riskScore') || 0 }}</div>
            <div class="context-extra">limitedQuery = {{ result.limitedQuery || getSecureValue('limitedQuery') || false }}</div>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="context-card">
            <div class="context-label">安全 TopK</div>
            <div class="context-value">{{ result.safeTopK || getSecureValue('safeTopK') || result.topK || '-' }}</div>
            <div class="context-extra">原始 TopK：{{ getSecureValue('rawTopK') || form.topK }}</div>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="context-card">
            <div class="context-label">净化后查询</div>
            <div class="context-value text-value">{{ result.sanitizedQuery || getSecureValue('sanitizedQuery') || result.query || '-' }}</div>
            <div class="context-extra">原始查询：{{ getSecureValue('rawQuery') || form.query }}</div>
          </div>
        </el-col>
      </el-row>

      <div class="detail-block compact">
        <div class="detail-label">拦截 / 限制原因</div>
        <div class="tag-list">
          <el-tag
            v-for="item in securityReasons"
            :key="item"
            size="small"
            type="danger"
            effect="plain"
          >
            {{ item }}
          </el-tag>
          <span v-if="securityReasons.length === 0" class="empty-text">后端未返回具体原因</span>
        </div>
      </div>

      <div class="detail-block">
        <div class="detail-label">常见原因对照</div>
        <pre class="filter-code">{{ securityReasonHelp }}</pre>
      </div>
    </el-card>

    <el-card v-if="result" shadow="never" class="box-card">
      <div slot="header" class="section-header">
        <span class="section-title">本次检索权限上下文</span>
        <div>
          <el-tag :type="result.searchMode === 'remote_rag_server' ? 'success' : 'warning'" effect="plain">
            {{ result.searchMode === 'remote_rag_server' ? 'remote_rag_server' : 'mock_sys_rag_doc' }}
          </el-tag>
          <el-tag :type="result.allowAccess ? 'success' : 'danger'" effect="plain" style="margin-left: 8px;">
            {{ result.allowAccess ? '策略放行' : '策略拒绝' }}
          </el-tag>
        </div>
      </div>

      <el-row :gutter="12" class="context-row">
        <el-col :span="6">
          <div class="context-card">
            <div class="context-label">当前用户</div>
            <div class="context-value">{{ result.userName || '-' }}</div>
            <div class="context-extra">用户ID：{{ result.userId || '-' }}</div>
          </div>
        </el-col>

        <el-col :span="6">
          <div class="context-card">
            <div class="context-label">管理员身份</div>
            <div class="context-value">
              <el-tag :type="result.admin ? 'success' : 'info'" size="small">
                {{ result.admin ? '管理员' : '普通用户' }}
              </el-tag>
            </div>
            <div class="context-extra">admin = {{ result.admin }}</div>
          </div>
        </el-col>

        <el-col :span="6">
          <div class="context-card">
            <div class="context-label">候选结果</div>
            <div class="context-value">{{ result.rawResultCount || 0 }} 条</div>
            <div class="context-extra">
              {{ result.searchMode === 'remote_rag_server' ? '来自 RAG Server /rag/search + Milvus' : '来自 sys_rag_doc 模拟候选集' }}
            </div>
          </div>
        </el-col>

        <el-col :span="6">
          <div class="context-card">
            <div class="context-label">过滤统计</div>
            <div class="context-value">{{ result.filteredResultCount || 0 }} / {{ result.rejectedResultCount || 0 }}</div>
            <div class="context-extra">通过 / 拦截，耗时：{{ result.costTime || 0 }} ms</div>
          </div>
        </el-col>
      </el-row>

      <el-divider />

      <el-row :gutter="16">
        <el-col :span="12">
          <div class="detail-block compact">
            <div class="detail-label">用户组编码</div>
            <div class="tag-list">
              <el-tag v-for="item in normalizeArray(result.groupCodes)" :key="item" size="small" type="info" effect="plain">
                {{ item }}
              </el-tag>
              <span v-if="normalizeArray(result.groupCodes).length === 0" class="empty-text">-</span>
            </div>
          </div>
        </el-col>

        <el-col :span="12">
          <div class="detail-block compact">
            <div class="detail-label">可访问标签</div>
            <div class="tag-list">
              <el-tag v-for="item in normalizeArray(result.scopeCodes)" :key="item" size="small" type="success" effect="plain">
                {{ item }}
              </el-tag>
              <span v-if="normalizeArray(result.scopeCodes).length === 0" class="empty-text">-</span>
            </div>
          </div>
        </el-col>
      </el-row>

      <el-row :gutter="16">
        <el-col :span="12">
          <div class="detail-block">
            <div class="detail-label">平台完整 VACP Metadata Filter</div>
            <pre class="filter-code">{{ result.metadataFilter || '-' }}</pre>
          </div>
        </el-col>

        <el-col :span="12">
          <div class="detail-block">
            <div class="detail-label">发给 RAG Server 的 Milvus 粗过滤表达式</div>
            <pre class="filter-code">{{ buildMilvusCompatibleFilter() }}</pre>
          </div>
        </el-col>
      </el-row>

      <div class="detail-block">
        <div class="detail-label">远程联调说明</div>
        <div class="decision-message">
          {{ result.decisionMessage || result.message || '-' }}
        </div>
        <div v-if="result.searchMode === 'remote_rag_server'" class="remote-note">
          说明：RAG Server 当前 Milvus collection 支持 scope_code、chunk_id 等字段，因此远程检索阶段先用 scope_code 做粗过滤；
          平台侧继续使用完整 VACP Metadata Filter 完成文档状态、密级、用户组、知悉范围等精细二次过滤。
        </div>
      </div>
    </el-card>

    <el-row v-if="result" :gutter="16">
      <el-col :span="12">
        <el-card class="box-card" shadow="never">
          <div slot="header" class="section-header">
            <span class="section-title">平台后端收到的检索请求 JSON</span>
            <el-button size="mini" type="primary" plain @click="copyText(formatJson(form), '平台请求 JSON 已复制')">复制</el-button>
          </div>
          <pre class="json-box">{{ formatJson(form) }}</pre>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card class="box-card" shadow="never">
          <div slot="header" class="section-header">
            <span class="section-title">后端传给 RAG Server 的检索请求 JSON</span>
            <el-button size="mini" type="primary" plain @click="copyText(formatJson(buildRagServerRequest()), 'RAG Server 请求 JSON 已复制')">复制</el-button>
          </div>
          <pre class="json-box">{{ formatJson(buildRagServerRequest()) }}</pre>
        </el-card>
      </el-col>
    </el-row>

    <el-card v-if="result" class="box-card" shadow="never">
      <div slot="header" class="section-header">
        <span class="section-title">AI 生成回答</span>
        <el-tag size="mini" :type="result.answerEnabled ? 'success' : 'info'">
          {{ result.answerEnabled ? '外部模型已启用' : '外部模型未启用' }}
        </el-tag>
      </div>

      <div class="answer-box">
        <div class="answer-meta">
          <span>模型：{{ result.answerModel || '-' }}</span>
          <span>生成耗时：{{ result.answerCostTime || 0 }} ms</span>
          <span>授权片段：{{ result.filteredResultCount || 0 }} 条</span>
        </div>
        <div class="answer-content">
          {{ result.answer || '暂无回答' }}
        </div>
      </div>
    </el-card>

    <el-card v-if="result" class="box-card" shadow="never">
      <div slot="header" class="section-header">
        <span class="section-title">过滤后文档结果</span>
        <span class="section-desc">展示经过平台侧二次权限过滤后允许返回给当前用户的文档。</span>
      </div>

      <el-table :data="result.filteredResults || []" border stripe class="result-table">
        <el-table-column label="文档ID" prop="docId" width="220" align="center" show-overflow-tooltip>
          <template slot-scope="scope">
            <span class="mono-text">{{ scope.row.docId }}</span>
          </template>
        </el-table-column>
        <el-table-column label="标题" prop="title" min-width="180" show-overflow-tooltip />
        <el-table-column label="知悉范围" prop="scopeCode" width="130" align="center">
          <template slot-scope="scope">
            <el-tag size="small" type="success" effect="plain">{{ scope.row.scopeCode || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="密级" width="120" align="center">
          <template slot-scope="scope">
            <el-tag size="small" :type="levelTagType(scope.row.level || scope.row.securityLevel)" effect="plain">
              {{ scope.row.level || scope.row.securityLevel || '-' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="过滤状态" width="120" align="center">
          <template slot-scope="scope">
            <el-tag :type="isPassed(scope.row) ? 'success' : 'danger'" size="small" effect="plain">
              {{ isPassed(scope.row) ? '通过' : '拒绝' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="所属用户组" min-width="150" show-overflow-tooltip>
          <template slot-scope="scope">
            {{ scope.row.ownerGroupName || scope.row.ownerGroupCode || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="元数据状态" prop="metadataStatus" width="120" align="center">
          <template slot-scope="scope">
            <el-tag :type="scope.row.metadataStatus === 'ACTIVE' ? 'success' : 'info'" size="small" effect="plain">
              {{ scope.row.metadataStatus || '-' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="过滤说明" prop="filterReason" min-width="260" show-overflow-tooltip />
        <el-table-column label="内容摘要" prop="content" min-width="420" show-overflow-tooltip />
      </el-table>

      <el-empty v-if="!result.filteredResults || result.filteredResults.length === 0" description="暂无可返回文档" />
    </el-card>

    <el-card v-if="result" class="box-card" shadow="never">
      <div slot="header" class="section-header">
        <span class="section-title">被权限过滤拦截的候选结果</span>
        <span class="section-desc">展示命中但不允许返回给当前用户的文档。</span>
      </div>

      <el-table :data="result.rejectedResults || []" border stripe class="result-table">
        <el-table-column label="文档ID" prop="docId" width="220" align="center" show-overflow-tooltip />
        <el-table-column label="标题" prop="title" min-width="180" show-overflow-tooltip />
        <el-table-column label="知悉范围" prop="scopeCode" width="130" align="center">
          <template slot-scope="scope">
            <el-tag size="small" type="warning" effect="plain">{{ scope.row.scopeCode || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="密级" width="120" align="center">
          <template slot-scope="scope">
            <el-tag size="small" :type="levelTagType(scope.row.level || scope.row.securityLevel)" effect="plain">
              {{ scope.row.level || scope.row.securityLevel || '-' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="拦截原因" prop="blockedReason" min-width="220" show-overflow-tooltip>
          <template slot-scope="scope">
            <el-tag type="danger" size="small" effect="plain">
              {{ scope.row.blockedReason || 'UNKNOWN' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="过滤说明" prop="filterReason" min-width="300" show-overflow-tooltip />
        <el-table-column label="内容摘要" prop="content" min-width="420" show-overflow-tooltip />
      </el-table>

      <el-empty v-if="!result.rejectedResults || result.rejectedResults.length === 0" description="暂无被拦截候选结果" />
    </el-card>

    <el-card v-if="result" class="box-card" shadow="never">
      <div slot="header" class="section-header">
        <span class="section-title">原始候选结果</span>
        <span class="section-desc">用于对比 RAG Server 或 mock 候选集返回内容。</span>
      </div>

      <el-table :data="result.rawResults || []" border stripe class="result-table">
        <el-table-column label="文档ID" prop="docId" width="220" align="center" show-overflow-tooltip />
        <el-table-column label="标题" prop="title" min-width="180" show-overflow-tooltip />
        <el-table-column label="知悉范围" prop="scopeCode" width="130" align="center" />
        <el-table-column label="密级" prop="level" width="120" align="center" />
        <el-table-column label="内容摘要" prop="content" min-width="500" show-overflow-tooltip />
      </el-table>
    </el-card>
  </div>
</template>

<script>
import { ragSearch } from '@/api/rag/search'

export default {
  name: 'RagSearch',
  data() {
    return {
      loading: false,
      result: null,
      form: {
        query: 'username',
        topK: 5,
        useRemote: true
      }
    }
  },
  computed: {
    securityReasons() {
      if (!this.result) {
        return []
      }
      const reasons = []
      reasons.push.apply(reasons, this.normalizeArray(this.result.denyReasons))
      if (this.result.secureQueryContext) {
        reasons.push.apply(reasons, this.normalizeArray(this.result.secureQueryContext.reasons))
      }
      return Array.from(new Set(reasons))
    },
    securityReasonHelp() {
      const map = {
        USER_SECURITY_ATTR_NOT_FOUND: '当前用户缺少 sys_user_security_attr 安全属性，需要补一条 ACTIVE/LOW 的用户安全属性。',
        USER_ACCESS_STATUS_NOT_ACTIVE: '当前用户访问状态不是 ACTIVE，请在用户安全属性里恢复启用。',
        OUT_OF_USER_ACCESS_TIME_WINDOW: '当前时间不在用户允许访问时间窗口内，可放宽 access_start_time/access_end_time。',
        NO_VALID_GROUP_ASSIGNED: '普通用户没有绑定有效用户组，需要维护 sys_user_group_rel。',
        NO_VALID_SCOPE_ASSIGNED: '普通用户所属用户组没有 scope_code，无法生成可访问知悉范围。',
        BBAC_IP_BLACKLIST_DENY: '当前访问 IP 命中黑名单。',
        BBAC_FAIL_COUNT_TEMP_LOCK: '失败次数达到阈值，用户被临时锁定。',
        RISK_SCORE_TOO_HIGH_QUERY_BLOCKED: '风险分达到拦截阈值，通常由 HIGH 风险、敏感词、高频/重复查询叠加造成。'
      }
      const reasons = this.securityReasons
      if (reasons.length === 0) {
        return '暂无原因。'
      }
      return reasons.map(item => item + '：' + (map[item] || '请查看 secureQueryContext 详情或审计日志。')).join('\n')
    }
  },
  methods: {
    handleSearch() {
      if (!this.form.query || this.form.query.trim().length === 0) {
        this.$message.warning('请输入检索问题')
        return
      }

      this.loading = true
      const payload = {
        query: this.form.query,
        topK: this.form.topK,
        useRemote: this.form.useRemote
      }

      ragSearch(payload).then(response => {
        const data = response && response.data ? response.data : response
        this.result = data
        if (response && response.code && response.code !== 200) {
          this.$message.warning(response.msg || data.message || '检索请求返回异常')
        } else {
          this.$message.success(this.form.useRemote ? '真实 RAG Server 检索完成' : '平台 Mock 检索完成')
        }
      }).catch(error => {
        console.error(error)
        this.$message.error('检索失败，请检查后端 8080 与 RAG Server 8081 是否正常运行')
      }).finally(() => {
        this.loading = false
      })
    },

    reset() {
      this.form = {
        query: 'username',
        topK: 5,
        useRemote: true
      }
      this.result = null
    },

    buildRagServerRequest() {
      return {
        query: this.form.query,
        topK: this.form.topK,
        userContext: {
          userId: this.result ? this.result.userId : null,
          userName: this.result ? this.result.userName : null,
          admin: this.result ? this.result.admin : null,
          groupCodes: this.result ? this.result.groupCodes : [],
          scopeCodes: this.result ? this.result.scopeCodes : []
        },
        metadataFilter: this.buildMilvusCompatibleFilter(),
        platformMetadataFilter: this.result ? this.result.metadataFilter : null,
        platformFilterMode: 'milvus_scope_prefilter_and_platform_second_filter'
      }
    },

    buildMilvusCompatibleFilter() {
      const scopes = this.result ? this.normalizeArray(this.result.scopeCodes) : []
      if (!scopes || scopes.length === 0) {
        return 'chunk_id != ""'
      }
      return 'scope_code in [' + scopes.map(item => '"' + item + '"').join(', ') + ']'
    },

    getSecureValue(key) {
      if (!this.result || !this.result.secureQueryContext) {
        return null
      }
      return this.result.secureQueryContext[key]
    },

    normalizeArray(value) {
      if (!value) {
        return []
      }
      if (Array.isArray(value)) {
        return value
      }
      return String(value).split(',').map(item => item.trim()).filter(item => item.length > 0)
    },

    formatJson(value) {
      return JSON.stringify(value || {}, null, 2)
    },

    copyText(text, message) {
      const input = document.createElement('textarea')
      input.value = text
      document.body.appendChild(input)
      input.select()
      document.execCommand('copy')
      document.body.removeChild(input)
      this.$message.success(message || '已复制')
    },

    isPassed(row) {
      return row && row.passed !== false
    },

    levelTagType(level) {
      if (level === 'SECRET' || level === 'HIGH') {
        return 'danger'
      }
      if (level === 'INTERNAL' || level === 'MEDIUM') {
        return 'warning'
      }
      if (level === 'PUBLIC' || level === 'LOW') {
        return 'success'
      }
      return 'info'
    }
  }
}
</script>

<style scoped>
.rag-search-page {
  background: #f5f7fa;
  min-height: calc(100vh - 84px);
}

.box-card {
  margin-bottom: 16px;
  border-radius: 6px;
}

.card-header,
.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.page-subtitle,
.section-desc,
.context-extra,
.mode-tip {
  margin-top: 6px;
  font-size: 13px;
  color: #909399;
}

.mode-tip {
  margin-left: 12px;
}

.tips-alert,
.search-form {
  margin-top: 16px;
}

.context-card {
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 14px 16px;
  background: #fafafa;
}

.context-label {
  font-size: 13px;
  color: #909399;
}

.context-value {
  margin-top: 8px;
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}

.context-value.text-value {
  font-size: 14px;
  line-height: 22px;
  word-break: break-all;
}

.detail-block {
  margin-top: 16px;
}

.detail-label {
  margin-bottom: 8px;
  font-weight: 600;
  color: #606266;
}

.tag-list .el-tag {
  margin-right: 8px;
  margin-bottom: 6px;
}

.filter-code,
.json-box {
  padding: 12px;
  background: #f6f8fa;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  font-family: Menlo, Monaco, Consolas, monospace;
  white-space: pre-wrap;
  word-break: break-all;
  color: #606266;
}

.decision-message {
  color: #606266;
}

.mono-text {
  font-family: Menlo, Monaco, Consolas, monospace;
}

.empty-text {
  color: #c0c4cc;
}

.result-table {
  width: 100%;
}

.answer-box {
  line-height: 1.8;
}

.answer-meta {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  margin-bottom: 12px;
  color: #909399;
  font-size: 13px;
}

.answer-content {
  white-space: pre-wrap;
  color: #303133;
  background: #f8f9fb;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 14px 16px;
}

.remote-note {
  margin-top: 10px;
  padding: 10px 12px;
  background: #ecf5ff;
  border: 1px solid #d9ecff;
  border-radius: 6px;
  color: #606266;
  line-height: 1.7;
}

</style>
