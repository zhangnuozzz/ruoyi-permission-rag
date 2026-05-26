<template>
  <div class="app-container rag-search-page">
    <el-card class="box-card" shadow="never">
      <div slot="header" class="card-header">
        <div>
          <div class="page-title">数据库查询</div>
          <div class="page-subtitle">
            查询会先经过权限上下文与策略决策，再由 RAG Server 执行带 Metadata Filter 的向量检索，最终交给本地文本模型生成回复。
          </div>
        </div>
        <el-tag type="success" effect="plain">安全检索模式</el-tag>
      </div>

      <el-alert
        title="链路定位：用户问题 → 当前登录用户权限上下文 → 策略决策 → Metadata Filter → 候选结果二次过滤 → RAG 检索审计"
        type="info"
        :closable="false"
        show-icon
        class="tips-alert"
      />

      <el-form :model="form" label-width="90px" class="search-form">
        <el-form-item label="检索问题">
          <el-input
            v-model="form.query"
            type="textarea"
            :rows="4"
            placeholder="请输入要检索的问题，例如：查询内部研发制度"
          />
        </el-form-item>

        <el-form-item label="Top-K">
          <el-input-number v-model="form.topK" :min="1" :max="20" />
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

    <el-card v-if="result" class="box-card" shadow="never">
      <div slot="header" class="section-header">
        <span class="section-title">本次检索权限上下文</span>
        <el-tag :type="result.allowAccess ? 'success' : 'danger'" effect="plain">
          {{ result.allowAccess ? '策略放行' : '策略拒绝' }}
        </el-tag>
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
            <div class="context-extra">{{ result.searchMode || 'remote' }}，Top-K：{{ result.topK || form.topK }}</div>
          </div>
        </el-col>

        <el-col :span="6">
          <div class="context-card">
            <div class="context-label">过滤统计</div>
            <div class="context-value">{{ result.filteredResultCount || 0 }} / {{ result.rejectedResultCount || 0 }}</div>
            <div class="context-extra">允许 / 拦截，耗时：{{ result.costTime || 0 }} ms</div>
          </div>
        </el-col>
      </el-row>

      <el-divider />

      <el-row :gutter="16">
        <el-col :span="12">
          <div class="detail-block compact">
            <div class="detail-label">用户组编码</div>
            <div class="tag-list">
              <el-tag
                v-for="item in normalizeArray(result.groupCodes)"
                :key="item"
                size="small"
                type="info"
                effect="plain"
              >
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
              <el-tag
                v-for="item in normalizeArray(result.scopeCodes)"
                :key="item"
                size="small"
                type="success"
                effect="plain"
              >
                {{ item }}
              </el-tag>
              <span v-if="normalizeArray(result.scopeCodes).length === 0" class="empty-text">-</span>
            </div>
          </div>
        </el-col>
      </el-row>

      <div class="detail-block">
        <div class="detail-label">Metadata Filter</div>
        <pre class="filter-code">{{ result.metadataFilter || '-' }}</pre>
      </div>

      <div class="detail-block">
        <div class="detail-label">决策说明</div>
        <div class="decision-message">
          {{ result.decisionMessage || result.message || '-' }}
        </div>
      </div>

      <div v-if="result.denyReasons && result.denyReasons.length" class="detail-block">
        <div class="detail-label">拒绝原因</div>
        <div class="tag-list">
          <el-tag
            v-for="item in result.denyReasons"
            :key="item"
            type="danger"
            size="small"
            effect="plain"
          >
            {{ item }}
          </el-tag>
        </div>
      </div>
    </el-card>

    <el-row v-if="result" :gutter="16">
      <el-col :span="12">
        <el-card class="box-card" shadow="never">
          <div slot="header" class="section-header">
            <span class="section-title">已转发给 RAG Server 的安全检索请求 JSON</span>
            <el-button size="mini" type="primary" plain @click="copyRequestJson">复制</el-button>
          </div>
          <pre class="json-box">{{ formatJson(buildRagServerRequest()) }}</pre>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card class="box-card" shadow="never">
          <div slot="header" class="section-header">
            <span class="section-title">模型回复</span>
            <el-tag size="mini" :type="result.llmEnabled ? 'success' : 'info'">
              {{ result.llmEnabled ? 'liteLLM' : '未启用' }}
            </el-tag>
          </div>
          <div class="answer-box">
            {{ result.answer || '-' }}
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card v-if="result" class="box-card" shadow="never">
      <div slot="header" class="section-header">
        <span class="section-title">过滤后文档结果</span>
        <span class="section-desc">展示经过权限二次过滤后允许返回给当前用户的文档。</span>
      </div>

      <el-table
        :data="result.filteredResults || []"
        border
        stripe
        class="result-table"
      >
        <el-table-column label="文档ID" prop="docId" width="220" align="center" show-overflow-tooltip>
          <template slot-scope="scope">
            <span class="mono-text">{{ scope.row.docId }}</span>
          </template>
        </el-table-column>

        <el-table-column label="标题" prop="title" min-width="180" show-overflow-tooltip />

        <el-table-column label="切块ID" prop="chunkId" width="220" align="center" show-overflow-tooltip>
          <template slot-scope="scope">
            <span class="mono-text">{{ scope.row.chunkId || '-' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="分数" prop="score" width="90" align="center">
          <template slot-scope="scope">
            {{ formatScore(scope.row.score) }}
          </template>
        </el-table-column>

        <el-table-column label="知悉范围" prop="scopeCode" width="130" align="center">
          <template slot-scope="scope">
            <el-tag size="small" type="success" effect="plain">
              {{ scope.row.scopeCode }}
            </el-tag>
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

        <el-table-column label="过滤说明" prop="filterReason" min-width="260" show-overflow-tooltip>
          <template slot-scope="scope">
            <span class="doc-reason">{{ scope.row.filterReason || '-' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="内容摘要" prop="content" min-width="420" show-overflow-tooltip>
          <template slot-scope="scope">
            <span class="doc-content">{{ scope.row.content }}</span>
          </template>
        </el-table-column>
      </el-table>

      <el-empty
        v-if="!result.filteredResults || result.filteredResults.length === 0"
        description="暂无可返回文档"
      />
    </el-card>


    <el-card v-if="result" class="box-card" shadow="never">
      <div slot="header" class="section-header">
        <span class="section-title">被权限过滤拦截的候选结果</span>
        <span class="section-desc">展示模拟候选结果中命中但不允许返回给当前用户的文档。</span>
      </div>

      <el-table
        :data="result.rejectedResults || []"
        border
        stripe
        class="result-table"
      >
        <el-table-column label="文档ID" prop="docId" width="220" align="center" show-overflow-tooltip>
          <template slot-scope="scope">
            <span class="mono-text">{{ scope.row.docId }}</span>
          </template>
        </el-table-column>

        <el-table-column label="标题" prop="title" min-width="180" show-overflow-tooltip />

        <el-table-column label="知悉范围" prop="scopeCode" width="130" align="center">
          <template slot-scope="scope">
            <el-tag size="small" type="danger" effect="plain">
              {{ scope.row.scopeCode }}
            </el-tag>
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
          <template>
            <el-tag type="danger" size="small" effect="plain">已拦截</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="拦截说明" prop="filterReason" min-width="300" show-overflow-tooltip>
          <template slot-scope="scope">
            <span class="doc-reason">{{ scope.row.filterReason || '当前用户无该文档 scopeCode 访问权限' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="内容摘要" prop="content" min-width="420" show-overflow-tooltip>
          <template slot-scope="scope">
            <span class="doc-content">{{ scope.row.content }}</span>
          </template>
        </el-table-column>
      </el-table>

      <el-empty
        v-if="!result.rejectedResults || result.rejectedResults.length === 0"
        description="暂无被拦截候选结果"
      />
    </el-card>

    <el-card v-if="result" class="box-card" shadow="never">
      <div slot="header" class="section-header">
        <span class="section-title">完整后端返回 JSON</span>
        <el-button size="mini" type="primary" plain @click="copyResultJson">复制</el-button>
      </div>
      <pre class="json-box large">{{ formatJson(result) }}</pre>
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
      form: {
        query: '查询内部研发制度',
        topK: 5
      },
      result: null
    }
  },
  methods: {
    handleSearch() {
      if (!this.form.query) {
        this.$modal.msgWarning('请输入检索问题')
        return
      }

      this.loading = true
      ragSearch(this.form).then(response => {
        this.result = response.data
        this.$modal.msgSuccess('检索完成，已生成权限上下文与过滤结果')
      }).finally(() => {
        this.loading = false
      })
    },
    reset() {
      this.form.query = ''
      this.result = null
    },
    normalizeArray(value) {
      if (!value) {
        return []
      }
      if (Array.isArray(value)) {
        return value
      }
      return String(value).split(',').map(item => item.trim()).filter(Boolean)
    },
    isPassed(row) {
      if (!row) {
        return false
      }
      if (typeof row.passed !== 'undefined') {
        return row.passed
      }
      if (typeof row.allowAccess !== 'undefined') {
        return row.allowAccess
      }
      if (typeof row.allowed !== 'undefined') {
        return row.allowed
      }
      return true
    },
    levelTagType(value) {
      if (value === 'SECRET') {
        return 'danger'
      }
      if (value === 'INTERNAL') {
        return 'warning'
      }
      if (value === 'PUBLIC') {
        return 'success'
      }
      return 'info'
    },
    buildRagServerRequest() {
      if (!this.result) {
        return {}
      }
      return {
        query: this.result.query || this.form.query,
        topK: this.result.topK || this.form.topK,
        userContext: {
          userId: this.result.userId,
          userName: this.result.userName,
          admin: this.result.admin,
          groupCodes: this.normalizeArray(this.result.groupCodes),
          scopeCodes: this.normalizeArray(this.result.scopeCodes)
        },
        metadataFilter: this.result.metadataFilter || '',
        platformFilterMode: 'metadata_filter_and_second_filter'
      }
    },
    formatScore(value) {
      if (value === null || typeof value === 'undefined' || value === '') {
        return '-'
      }
      const num = Number(value)
      return isNaN(num) ? String(value) : num.toFixed(4)
    },
    formatJson(value) {
      try {
        return JSON.stringify(value || {}, null, 2)
      } catch (e) {
        return String(value)
      }
    },
    copyRequestJson() {
      this.copyText(this.formatJson(this.buildRagServerRequest()), '检索请求 JSON 已复制')
    },
    copyResultJson() {
      this.copyText(this.formatJson(this.result), '后端返回 JSON 已复制')
    },
    copyText(text, successMsg) {
      if (navigator.clipboard) {
        navigator.clipboard.writeText(text).then(() => {
          this.$modal.msgSuccess(successMsg)
        })
      } else {
        this.$modal.msgWarning('当前浏览器不支持自动复制，请手动复制')
      }
    }
  }
}
</script>

<style scoped>
.rag-search-page {
  padding: 20px;
}

.box-card {
  margin-bottom: 18px;
  border-radius: 6px;
}

.card-header,
.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.page-title {
  font-size: 17px;
  font-weight: 600;
  color: #303133;
  line-height: 26px;
}

.page-subtitle {
  margin-top: 4px;
  font-size: 13px;
  color: #909399;
}

.tips-alert {
  margin-bottom: 16px;
}

.search-form {
  padding: 4px 6px 0 0;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.section-desc {
  font-size: 13px;
  color: #909399;
  margin-left: 12px;
}

.context-row {
  margin-bottom: 12px;
}

.context-card {
  padding: 14px 16px;
  background: #f8fafc;
  border: 1px solid #ebeef5;
  border-radius: 6px;
}

.context-label {
  font-size: 13px;
  color: #909399;
  margin-bottom: 8px;
}

.context-value {
  font-size: 17px;
  color: #303133;
  font-weight: 600;
  margin-bottom: 8px;
  word-break: break-all;
}

.context-extra {
  font-size: 12px;
  color: #909399;
}

.detail-block {
  display: flex;
  align-items: flex-start;
  padding: 12px 0;
  border-top: 1px solid #ebeef5;
}

.detail-block.compact {
  border-top: none;
  padding-top: 4px;
}

.detail-label {
  width: 130px;
  flex-shrink: 0;
  color: #606266;
  font-weight: 500;
  line-height: 28px;
}

.tag-list {
  flex: 1;
  line-height: 28px;
}

.tag-list .el-tag {
  margin-right: 8px;
  margin-bottom: 6px;
}

.empty-text {
  color: #909399;
}

.filter-code {
  flex: 1;
  margin: 0;
  padding: 10px 12px;
  background: #f4f6f8;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  color: #606266;
  font-family: Menlo, Monaco, Consolas, monospace;
  white-space: pre-wrap;
  word-break: break-all;
  line-height: 1.6;
}

.decision-message {
  flex: 1;
  color: #606266;
  line-height: 28px;
}

.result-table {
  width: 100%;
}

.doc-reason {
  color: #606266;
  line-height: 1.6;
}

.doc-content {
  color: #606266;
  line-height: 1.6;
}

.json-box {
  margin: 0;
  padding: 12px;
  max-height: 280px;
  overflow: auto;
  background: #f5f7fa;
  border-radius: 4px;
  color: #606266;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
}

.json-box.large {
  max-height: 420px;
}

.answer-box {
  color: #606266;
  font-size: 13px;
  line-height: 1.8;
  white-space: pre-wrap;
}

.mono-text {
  font-family: Menlo, Monaco, Consolas, "Courier New", monospace;
  font-size: 12px;
}

::v-deep .el-table .cell {
  white-space: nowrap;
}

@media screen and (max-width: 1200px) {
  .context-row .el-col {
    margin-bottom: 12px;
  }
}

@media screen and (max-width: 768px) {
  .detail-block {
    display: block;
  }

  .detail-label {
    width: auto;
    margin-bottom: 6px;
  }
}
</style>
