<template>
  <div class="app-container rag-audit-page">
    <el-card shadow="never" class="box-card">
      <div slot="header" class="card-header">
        <div>
          <div class="page-title">安全审计与行为分析中心</div>
          <div class="page-subtitle">
            展示 RAG 安全检索请求的身份上下文、查询安全上下文、Metadata Filter、访问决策、风险分数、结果过滤与拦截原因。
          </div>
        </div>
        <el-tag type="info" effect="plain">sys_rag_audit_log</el-tag>
      </div>

      <el-alert
        title="审计链路：检索问题 → 查询安全上下文 → 权限上下文 → Metadata Filter → 安全向量检索 → 二次过滤 → 审计留痕"
        type="info"
        :closable="false"
        show-icon
        class="tips-alert"
      />

      <el-row :gutter="12" class="summary-row">
        <el-col :span="6">
          <el-card shadow="never" class="summary-card">
            <div class="summary-label">当前页审计数</div>
            <div class="summary-value">{{ logList.length }}</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="never" class="summary-card">
            <div class="summary-label">当前页放行</div>
            <div class="summary-value success">{{ allowCount }}</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="never" class="summary-card">
            <div class="summary-label">当前页拒绝</div>
            <div class="summary-value danger">{{ denyCount }}</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="never" class="summary-card">
            <div class="summary-label">当前页拦截结果</div>
            <div class="summary-value warning">{{ blockedResultCount }}</div>
          </el-card>
        </el-col>
      </el-row>

      <el-form
        v-show="showSearch"
        ref="queryForm"
        :model="queryParams"
        size="small"
        :inline="true"
        label-width="82px"
      >
        <el-form-item label="用户名" prop="userName">
          <el-input
            v-model="queryParams.userName"
            placeholder="请输入用户名"
            clearable
            style="width: 160px"
            @keyup.enter.native="handleQuery"
          />
        </el-form-item>

        <el-form-item label="查询内容" prop="queryText">
          <el-input
            v-model="queryParams.queryText"
            placeholder="请输入检索内容"
            clearable
            style="width: 210px"
            @keyup.enter.native="handleQuery"
          />
        </el-form-item>

        <el-form-item label="用户组" prop="groupCodes">
          <el-input
            v-model="queryParams.groupCodes"
            placeholder="用户组编码"
            clearable
            style="width: 160px"
            @keyup.enter.native="handleQuery"
          />
        </el-form-item>

        <el-form-item label="权限标签" prop="scopeCodes">
          <el-input
            v-model="queryParams.scopeCodes"
            placeholder="scopeCode"
            clearable
            style="width: 160px"
            @keyup.enter.native="handleQuery"
          />
        </el-form-item>

        <el-form-item label="访问决策" prop="allowAccess">
          <el-select
            v-model="queryParams.allowAccess"
            placeholder="访问决策"
            clearable
            style="width: 120px"
          >
            <el-option label="放行" value="1" />
            <el-option label="拒绝" value="0" />
          </el-select>
        </el-form-item>

        <el-form-item label="受限查询" prop="limitedQuery">
          <el-select
            v-model="queryParams.limitedQuery"
            placeholder="是否受限"
            clearable
            style="width: 120px"
          >
            <el-option label="是" value="1" />
            <el-option label="否" value="0" />
          </el-select>
        </el-form-item>

        <el-form-item label="拦截原因" prop="blockedReasons">
          <el-input
            v-model="queryParams.blockedReasons"
            placeholder="blockedReason"
            clearable
            style="width: 210px"
            @keyup.enter.native="handleQuery"
          />
        </el-form-item>

        <el-form-item label="审计时间">
          <el-date-picker
            v-model="daterangeCreateTime"
            style="width: 240px"
            value-format="yyyy-MM-dd"
            type="daterange"
            range-separator="-"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
          <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
          <el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport">导出</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="logList" border>
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column label="审计ID" align="center" prop="id" width="80" />
        <el-table-column label="用户" align="center" prop="userName" width="110" />
        <el-table-column label="检索内容" align="center" prop="queryText" min-width="180" show-overflow-tooltip />

        <el-table-column label="访问决策" align="center" prop="allowAccess" width="100">
          <template slot-scope="scope">
            <el-tag :type="scope.row.allowAccess === '1' ? 'success' : 'danger'" size="small">
              {{ scope.row.allowAccess === '1' ? '放行' : '拒绝' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="风险分数" align="center" prop="riskScore" width="90">
          <template slot-scope="scope">
            <el-tag :type="riskTagType(scope.row.riskScore)" size="small">
              {{ scope.row.riskScore || 0 }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="受限查询" align="center" prop="limitedQuery" width="90">
          <template slot-scope="scope">
            <el-tag :type="scope.row.limitedQuery === '1' ? 'warning' : 'info'" size="small">
              {{ scope.row.limitedQuery === '1' ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="通过/拦截" align="center" width="110">
          <template slot-scope="scope">
            <span class="pass-count">{{ scope.row.passedCount || 0 }}</span>
            <span>/</span>
            <span class="block-count">{{ scope.row.blockedCount || 0 }}</span>
          </template>
        </el-table-column>

        <el-table-column label="用户组" align="center" prop="groupCodes" min-width="170" show-overflow-tooltip />
        <el-table-column label="权限标签" align="center" prop="scopeCodes" min-width="170" show-overflow-tooltip />
        <el-table-column label="Metadata Filter" align="center" prop="metadataFilter" min-width="240" show-overflow-tooltip />
        <el-table-column label="拒绝原因" align="center" prop="denyReasons" min-width="150" show-overflow-tooltip />
        <el-table-column label="结果拦截原因" align="center" prop="blockedReasons" min-width="220" show-overflow-tooltip />
        <el-table-column label="耗时(ms)" align="center" prop="costTime" width="100" />
        <el-table-column label="审计时间" align="center" prop="createTime" width="170">
          <template slot-scope="scope">
            <span>{{ parseTime(scope.row.createTime) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="160" fixed="right">
          <template slot-scope="scope">
            <el-button
              size="mini"
              type="text"
              icon="el-icon-view"
              @click="handleViewJson(scope.row)"
            >详情</el-button>
            <el-button
              size="mini"
              type="text"
              icon="el-icon-delete"
              @click="handleDelete(scope.row)"
            >删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <pagination
        v-show="total > 0"
        :total="total"
        :page.sync="queryParams.pageNum"
        :limit.sync="queryParams.pageSize"
        @pagination="getList"
      />
    </el-card>

    <el-dialog
      title="RAG 安全审计详情"
      :visible.sync="jsonDialogOpen"
      width="82%"
      append-to-body
    >
      <el-descriptions :column="3" border size="small" class="audit-desc">
        <el-descriptions-item label="审计ID">{{ jsonDetail.id }}</el-descriptions-item>
        <el-descriptions-item label="用户">{{ jsonDetail.userName }}</el-descriptions-item>
        <el-descriptions-item label="访问决策">
          <el-tag :type="jsonDetail.allowAccess === '1' ? 'success' : 'danger'" size="small">
            {{ jsonDetail.allowAccess === '1' ? '放行' : '拒绝' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="风险分数">{{ jsonDetail.riskScore || 0 }}</el-descriptions-item>
        <el-descriptions-item label="受限查询">{{ jsonDetail.limitedQuery === '1' ? '是' : '否' }}</el-descriptions-item>
        <el-descriptions-item label="通过/拦截">
          {{ jsonDetail.passedCount || 0 }} / {{ jsonDetail.blockedCount || 0 }}
        </el-descriptions-item>
        <el-descriptions-item label="拦截原因" :span="3">
          {{ jsonDetail.blockedReasons || jsonDetail.denyReasons || '暂无' }}
        </el-descriptions-item>
      </el-descriptions>

      <el-tabs v-model="jsonActiveTab" class="json-tabs">
        <el-tab-pane label="查询安全上下文" name="secureContext">
          <pre class="json-viewer">{{ formatJson(jsonDetail.secureContextJson) }}</pre>
        </el-tab-pane>

        <el-tab-pane label="用户权限上下文" name="userContext">
          <pre class="json-viewer">{{ formatJson(jsonDetail.userContextJson) }}</pre>
        </el-tab-pane>

        <el-tab-pane label="Metadata Filter" name="metadataFilter">
          <pre class="json-viewer">{{ formatJson(jsonDetail.metadataFilter) }}</pre>
        </el-tab-pane>

        <el-tab-pane label="请求JSON" name="request">
          <pre class="json-viewer">{{ formatJson(jsonDetail.requestJson) }}</pre>
        </el-tab-pane>

        <el-tab-pane label="原始候选" name="raw">
          <pre class="json-viewer">{{ formatJson(jsonDetail.rawResultsJson) }}</pre>
        </el-tab-pane>

        <el-tab-pane label="通过结果" name="passed">
          <pre class="json-viewer">{{ formatJson(jsonDetail.passedResultsJson) }}</pre>
        </el-tab-pane>

        <el-tab-pane label="拦截结果" name="blocked">
          <pre class="json-viewer">{{ formatJson(jsonDetail.blockedResultsJson) }}</pre>
        </el-tab-pane>

        <el-tab-pane label="响应JSON" name="response">
          <pre class="json-viewer">{{ formatJson(jsonDetail.responseJson) }}</pre>
        </el-tab-pane>
      </el-tabs>

      <span slot="footer" class="dialog-footer">
        <el-button @click="jsonDialogOpen = false">关闭</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { listLog, delLog, exportLog } from '@/api/system/log'

export default {
  name: 'RagAuditLog',
  data() {
    return {
      loading: true,
      ids: [],
      showSearch: true,
      total: 0,
      logList: [],
      daterangeCreateTime: [],
      jsonDialogOpen: false,
      jsonActiveTab: 'secureContext',
      jsonDetail: {},
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        userName: null,
        queryText: null,
        groupCodes: null,
        scopeCodes: null,
        allowAccess: null,
        limitedQuery: null,
        blockedReasons: null
      }
    }
  },
  computed: {
    allowCount() {
      return this.logList.filter(item => item.allowAccess === '1').length
    },
    denyCount() {
      return this.logList.filter(item => item.allowAccess === '0').length
    },
    blockedResultCount() {
      return this.logList.reduce((sum, item) => sum + (Number(item.blockedCount) || 0), 0)
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      listLog(this.addDateRange(this.queryParams, this.daterangeCreateTime, 'CreateTime')).then(response => {
        this.logList = response.rows || []
        this.total = response.total || 0
        this.loading = false
      })
    },

    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },

    resetQuery() {
      this.daterangeCreateTime = []
      this.resetForm('queryForm')
      this.handleQuery()
    },

    handleExport() {
      const queryParams = this.addDateRange(this.queryParams, this.daterangeCreateTime, 'CreateTime')
      this.$confirm('是否确认导出当前筛选条件下的 RAG 检索审计日志？', '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(function() {
        return exportLog(queryParams)
      }).then(response => {
        this.download(response.msg)
      })
    },

    handleViewJson(row) {
      this.jsonDetail = row || {}
      this.jsonActiveTab = 'secureContext'
      this.jsonDialogOpen = true
    },

    formatJson(value) {
      if (value === null || value === undefined || value === '') {
        return '暂无数据'
      }
      try {
        if (typeof value === 'string') {
          return JSON.stringify(JSON.parse(value), null, 2)
        }
        return JSON.stringify(value, null, 2)
      } catch (e) {
        return value
      }
    },

    riskTagType(score) {
      const value = Number(score) || 0
      if (value >= 80) {
        return 'danger'
      }
      if (value >= 50) {
        return 'warning'
      }
      if (value >= 20) {
        return ''
      }
      return 'success'
    },

    handleDelete(row) {
      const ids = row.id || this.ids
      this.$confirm('是否确认删除 RAG 检索审计日志编号为 "' + ids + '" 的数据项？', '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(function() {
        return delLog(ids)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess('删除成功')
      })
    }
  }
}
</script>

<style scoped>
.rag-audit-page {
  padding: 20px;
}

.box-card {
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
  color: #909399;
  font-size: 13px;
}

.tips-alert {
  margin-bottom: 16px;
}

.summary-row {
  margin-bottom: 16px;
}

.summary-card {
  border-radius: 6px;
}

.summary-label {
  color: #909399;
  font-size: 13px;
}

.summary-value {
  margin-top: 8px;
  font-size: 24px;
  font-weight: 600;
  color: #303133;
}

.summary-value.success {
  color: #67c23a;
}

.summary-value.danger {
  color: #f56c6c;
}

.summary-value.warning {
  color: #e6a23c;
}

.pass-count {
  color: #67c23a;
  font-weight: 600;
}

.block-count {
  color: #f56c6c;
  font-weight: 600;
}

.audit-desc {
  margin-bottom: 12px;
}

.json-tabs {
  margin-top: 12px;
}

.json-viewer {
  max-height: 520px;
  overflow: auto;
  padding: 12px;
  background: #f6f8fa;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  font-size: 13px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
  font-family: Menlo, Monaco, Consolas, "Courier New", monospace;
}
</style>
