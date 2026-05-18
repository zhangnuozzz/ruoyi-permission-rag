<template>
  <div class="app-container rag-search-page">
    <el-card class="box-card" shadow="never">
      <div slot="header" class="card-header">
        <div>
          <div class="page-title">RAG 安全检索测试</div>
          <div class="page-subtitle">
            基于当前登录用户生成权限上下文，完成策略决策、Metadata Filter 生成、二次过滤与审计留痕。
          </div>
        </div>
      </div>

      <el-form :model="form" label-width="90px" class="search-form">
        <el-form-item label="检索问题">
          <el-input
            v-model="form.query"
            type="textarea"
            :rows="4"
            placeholder="请输入要检索的问题，例如：测试从文档权限标签表读取候选结果"
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

    <el-card v-if="result" class="box-card" shadow="never">
      <div slot="header" class="section-header">
        <span class="section-title">权限决策结果</span>
        <el-tag :type="result.allowAccess ? 'success' : 'danger'" effect="plain">
          {{ result.allowAccess ? '策略放行' : '策略拒绝' }}
        </el-tag>
      </div>

      <div class="summary-grid">
        <div class="summary-item">
          <div class="summary-label">用户ID</div>
          <div class="summary-value">{{ result.userId }}</div>
        </div>
        <div class="summary-item">
          <div class="summary-label">用户名</div>
          <div class="summary-value">{{ result.userName }}</div>
        </div>
        <div class="summary-item">
          <div class="summary-label">管理员</div>
          <div class="summary-value">
            <el-tag :type="result.admin ? 'success' : 'info'" size="small">
              {{ result.admin ? '是' : '否' }}
            </el-tag>
          </div>
        </div>
        <div class="summary-item">
          <div class="summary-label">耗时</div>
          <div class="summary-value">{{ result.costTime }} ms</div>
        </div>
        <div class="summary-item">
          <div class="summary-label">候选结果数</div>
          <div class="summary-value">{{ result.rawResultCount }}</div>
        </div>
        <div class="summary-item">
          <div class="summary-label">过滤后结果数</div>
          <div class="summary-value">{{ result.filteredResultCount }}</div>
        </div>
      </div>

      <div class="detail-block">
        <div class="detail-label">用户组</div>
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

      <div class="detail-block">
        <div class="detail-label">知悉范围</div>
        <div class="tag-list">
          <el-tag
            v-for="item in normalizeArray(result.scopeCodes)"
            :key="item"
            size="small"
            effect="plain"
          >
            {{ item }}
          </el-tag>
          <span v-if="normalizeArray(result.scopeCodes).length === 0" class="empty-text">-</span>
        </div>
      </div>

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
        <el-table-column label="文档ID" prop="docId" width="110" align="center" />
        <el-table-column label="标题" prop="title" min-width="170" />
        <el-table-column label="知悉范围" prop="scopeCode" width="140" align="center">
          <template slot-scope="scope">
            <el-tag size="small" effect="plain">
              {{ scope.row.scopeCode }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="密级" width="120" align="center">
          <template slot-scope="scope">
            {{ scope.row.level || scope.row.securityLevel || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="过滤状态" width="120" align="center">
          <template slot-scope="scope">
            <el-tag :type="isPassed(scope.row) ? 'success' : 'danger'" size="small" effect="plain">
              {{ isPassed(scope.row) ? '通过' : '拒绝' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="过滤说明" prop="filterReason" min-width="260">
          <template slot-scope="scope">
            <span class="doc-reason">{{ scope.row.filterReason || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="内容摘要" prop="content" min-width="360">
          <template slot-scope="scope">
            <div class="doc-content">{{ scope.row.content }}</div>
          </template>
        </el-table-column>
      </el-table>

      <el-empty
        v-if="!result.filteredResults || result.filteredResults.length === 0"
        description="暂无可返回文档"
      />
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
        query: '测试从文档权限标签表读取候选结果'
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
        this.$modal.msgSuccess('检索完成')
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

.card-header {
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

.search-form {
  padding: 4px 6px 0 0;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
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

.summary-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(180px, 1fr));
  gap: 14px;
  margin-bottom: 18px;
}

.summary-item {
  padding: 14px 16px;
  background: #f8fafc;
  border: 1px solid #ebeef5;
  border-radius: 6px;
}

.summary-label {
  font-size: 13px;
  color: #909399;
  margin-bottom: 8px;
}

.summary-value {
  font-size: 16px;
  color: #303133;
  font-weight: 500;
  word-break: break-all;
}

.detail-block {
  display: flex;
  align-items: flex-start;
  padding: 12px 0;
  border-top: 1px solid #ebeef5;
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
  white-space: normal;
  word-break: break-all;
}

@media screen and (max-width: 1200px) {
  .summary-grid {
    grid-template-columns: repeat(2, minmax(180px, 1fr));
  }
}

@media screen and (max-width: 768px) {
  .summary-grid {
    grid-template-columns: 1fr;
  }

  .detail-block {
    display: block;
  }

  .detail-label {
    width: auto;
    margin-bottom: 6px;
  }
}
</style>
