<template>
  <div class="app-container rag-audit-page">
    <el-card shadow="never" class="box-card">
      <div slot="header" class="card-header">
        <div>
          <div class="page-title">RAG 检索审计</div>
          <div class="page-subtitle">
            展示 RAG 安全检索请求的身份上下文、查询安全上下文、Metadata Filter、访问决策、风险分数、结果过滤与拦截原因。
          </div>
        </div>
        <el-tag type="info" effect="plain">全链路审计</el-tag>
      </div>

      <el-alert
        title="请求准入与文档授权分别判定"
        description="请求获准执行后，仍逐条校验文档。停用或不符合权限的候选会被拦截，不会返回给用户。"
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
            <div class="summary-label">当前页获准请求</div>
            <div class="summary-value success">{{ allowCount }}</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="never" class="summary-card">
            <div class="summary-label">当前页拒绝请求</div>
            <div class="summary-value danger">{{ denyCount }}</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="never" class="summary-card">
            <div class="summary-label">当前页文档拦截次数</div>
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
            placeholder="权限标签"
            clearable
            style="width: 160px"
            @keyup.enter.native="handleQuery"
          />
        </el-form-item>

        <el-form-item label="请求准入" prop="allowAccess">
          <el-select
            v-model="queryParams.allowAccess"
            placeholder="请求准入"
            clearable
            style="width: 120px"
          >
            <el-option label="获准执行" value="1" />
            <el-option label="请求拒绝" value="0" />
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
            placeholder="请输入拦截原因"
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
          <el-button type="danger" plain size="mini" @click="showRejectedRequests">筛选请求拒绝</el-button>
          <el-button v-hasPermi="['system:log:export']" type="warning" plain icon="el-icon-download" size="mini" :loading="exporting" @click="handleExport">导出</el-button>
        </el-form-item>
      </el-form>

      <el-alert v-if="loadError || actionError" :title="loadError || actionError" type="error" :closable="false" show-icon class="tips-alert" />

      <el-table v-loading="loading" :data="logList" border>
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column label="审计ID" align="center" prop="id" width="80" />
        <el-table-column label="用户" align="center" prop="userName" width="110" />
        <el-table-column label="检索内容" align="center" prop="queryText" min-width="180" show-overflow-tooltip />

        <el-table-column label="请求准入" align="center" prop="allowAccess" width="110">
          <template slot-scope="scope">
            <el-tag :type="requestTagType(scope.row)" size="small">{{ requestDecisionName(scope.row) }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="执行结果" align="center" width="110">
          <template slot-scope="scope">
            <el-tag :type="executionTagType(scope.row)" size="small">{{ executionStatusName(scope.row) }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="文档校验" align="center" width="150">
          <template slot-scope="scope">
            <el-tag :type="documentTagType(scope.row)" size="small">{{ documentDecisionName(scope.row) }}</el-tag>
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

        <el-table-column label="可访问 / 拦截候选" align="center" width="150">
          <template slot-scope="scope">
            <span class="pass-count">{{ auditPassedCount(scope.row) }}</span>
            <span>/</span>
            <span class="block-count">{{ auditBlockedCount(scope.row) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="用户组" align="center" prop="groupCodes" min-width="170" show-overflow-tooltip />
        <el-table-column label="权限标签" align="center" prop="scopeCodes" min-width="170" show-overflow-tooltip />
        <el-table-column label="Metadata Filter" align="center" prop="metadataFilter" min-width="240" show-overflow-tooltip />
        <el-table-column label="请求拒绝原因" align="center" min-width="180" show-overflow-tooltip>
          <template slot-scope="scope">{{ reasonText(scope.row.denyReasons) }}</template>
        </el-table-column>
        <el-table-column label="文档拦截原因" align="center" min-width="180" show-overflow-tooltip>
          <template slot-scope="scope">{{ reasonText(scope.row.blockedReasons) }}</template>
        </el-table-column>
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

      <p class="count-note">以上数量仅统计当前页；文档拦截按候选次数累计，同一文档在不同请求中分别计数。</p>

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
        <el-descriptions-item label="请求准入">
          <el-tag :type="requestTagType(jsonDetail)" size="small">{{ requestDecisionName(jsonDetail) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="执行结果">
          <el-tag :type="executionTagType(jsonDetail)" size="small">{{ executionStatusName(jsonDetail) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="文档校验">
          <el-tag :type="documentTagType(jsonDetail)" size="small">{{ documentDecisionName(jsonDetail) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="风险分数">{{ jsonDetail.riskScore || 0 }}</el-descriptions-item>
        <el-descriptions-item label="受限查询">{{ jsonDetail.limitedQuery === '1' ? '是' : '否' }}</el-descriptions-item>
        <el-descriptions-item label="可访问 / 拦截候选">
          {{ auditPassedCount(jsonDetail) }} / {{ auditBlockedCount(jsonDetail) }}
        </el-descriptions-item>
        <el-descriptions-item label="请求拒绝原因" :span="3">{{ reasonText(jsonDetail.denyReasons) }}</el-descriptions-item>
        <el-descriptions-item label="文档拦截原因" :span="3">{{ reasonText(jsonDetail.blockedReasons) }}</el-descriptions-item>
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
          <div class="page-subtitle">新记录与对应账号的接口响应一致；历史记录可能包含内部诊断字段。</div>
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
import { listLog, delLog } from '@/api/system/log'
import { exportTable } from '@/utils/tableExport'
import { canUse } from '@/utils/identityAccess'

export default {
  name: 'RagAuditLog',
  data() {
    return {
      loading: true,
      listRequestId: 0,
      loadError: '',
      actionError: '',
      exporting: false,
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
      return this.logList.filter(item => this.requestDecisionCode(item) === '1').length
    },
    denyCount() {
      return this.logList.filter(item => this.requestDecisionCode(item) === '0').length
    },
    blockedResultCount() {
      return this.logList.reduce((sum, item) => {
        return sum + this.auditBlockedCount(item)
      }, 0)
    }
  },
  created() {
    this.getList()
  },
  methods: {
    requestDecisionCode(row) {
      const value = row && row.allowAccess
      if (value === '1' || value === 1 || value === true) return '1'
      if (value === '0' || value === 0 || value === false) return '0'
      return ''
    },
    requestDecisionName(row) {
      const code = this.requestDecisionCode(row)
      return code === '1' ? '获准执行' : code === '0' ? '请求拒绝' : '未判定'
    },
    requestTagType(row) {
      const code = this.requestDecisionCode(row)
      return code === '1' ? 'success' : code === '0' ? 'danger' : 'info'
    },
    executionStatusName(row) {
      const admitted = this.requestDecisionCode(row)
      if (admitted === '0') return '未执行'
      if (admitted !== '1' || !row.responseJson) return '未记录'
      try {
        const response = JSON.parse(row.responseJson)
        const code = response && String(response.code)
        if (code === '200') return '已完成'
        return /^[45][0-9]{2}$/.test(code) ? '执行失败' : '未记录'
      } catch (error) {
        return '未记录'
      }
    },
    executionTagType(row) {
      const name = this.executionStatusName(row)
      return name === '已完成' ? 'success' : name === '执行失败' ? 'danger' : 'info'
    },
    documentDecisionName(row) {
      const code = this.requestDecisionCode(row)
      if (code === '0') return '请求已拒绝'
      if (code !== '1') return '未判定'
      if (this.executionStatusName(row) === '执行失败') return '执行已中断'
      const blocked = this.auditBlockedCount(row)
      if (blocked > 0) return '已拦截 ' + blocked + ' 条候选'
      return this.auditPassedCount(row) > 0 ? '当前候选通过' : '无候选结果'
    },
    documentTagType(row) {
      const code = this.requestDecisionCode(row)
      if (code === '0') return 'danger'
      if (code !== '1') return 'info'
      if (this.executionStatusName(row) === '执行失败') return 'warning'
      if (this.auditBlockedCount(row) > 0) return 'warning'
      return this.auditPassedCount(row) > 0 ? 'success' : 'info'
    },
    reasonText(value) {
      if (!value) return '—'
      const names = {
        DOCUMENT_STATUS_NOT_ACTIVE: '文档已停用',
        DOCUMENT_METADATA_NOT_FOUND: '文档权限信息缺失',
        INVALID_DOCUMENT_SECURITY_LEVEL: '文档密级缺失或无效',
        USER_SECRET_LEVEL_LOWER_THAN_DOCUMENT: '人员密级不足',
        USER_NOT_IN_DOCUMENT_KNOWLEDGE_GROUP: '不在文档知悉范围内',
        STATE_SECRET_DOCUMENT_GROUP_NOT_CONFIGURED: '国家秘密文档未配置知悉组',
        OUT_OF_USER_ACCESS_TIME_WINDOW: '不在允许访问时间内',
        USER_ACCESS_STATUS_NOT_ACTIVE: '知识访问状态未启用',
        USER_SECURITY_ATTR_NOT_FOUND: '人员安全属性缺失',
        BBAC_IP_BLACKLIST_DENY: '访问来源在黑名单中',
        RISK_SCORE_TOO_HIGH_QUERY_BLOCKED: '风险分数过高',
        BBAC_REPEAT_QUERY_PATTERN_LIMITED: '重复查询受到限制'
      }
      return String(value).split(',').map(code => code.trim()).filter(Boolean).map(code => names[code] || code).join('；')
    },
    showRejectedRequests() {
      this.queryParams.allowAccess = '0'
      this.handleQuery()
    },
    buildQueryParams() {
      const query = { ...this.queryParams }
      const dates = this.daterangeCreateTime
      query.beginTime = undefined
      query.endTime = undefined
      if (Array.isArray(dates) && dates.length === 2 && dates[0] && dates[1]) {
        query.beginTime = dates[0] + ' 00:00:00'
        query.endTime = dates[1] + ' 23:59:59'
      }
      return query
    },

    async getList() {
      const requestId = ++this.listRequestId
      this.loading = true
      this.loadError = ''
      try {
        const response = await listLog(this.buildQueryParams())
        if (requestId !== this.listRequestId) return
        this.logList = response.rows || []
        this.total = response.total || 0
      } catch (error) {
        if (requestId !== this.listRequestId) return
        this.logList = []
        this.total = 0
        this.loadError = (error && error.message) || '审计日志加载失败，请重试。'
      } finally {
        if (requestId === this.listRequestId) this.loading = false
      }
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

    async handleExport() {
      if (this.exporting || !canUse(this.$store.getters.permissions, 'system:log:export')) return
      this.exporting = true
      this.actionError = ''
      try {
        await exportTable('/system/log/export', this.buildQueryParams())
      } catch (error) {
        this.actionError = (error && error.message) || '审计日志导出失败，请重试。'
      } finally {
        this.exporting = false
      }
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

    /**
     * 从审计 JSON 中计算结果数量。
     * 兼容历史记录中 passed_count / blocked_count 为 0，
     * 但结果 JSON 已经存在的情况。
     */
    auditJsonArrayCount(value) {
      if (!value) {
        return 0
      }

      if (Array.isArray(value)) {
        return value.length
      }

      if (typeof value === 'string') {
        try {
          const data = JSON.parse(value)
          return Array.isArray(data) ? data.length : 0
        } catch (e) {
          return 0
        }
      }

      return 0
    },

    /**
     * 获取实际通过数量。
     */
    auditPassedCount(row) {
      if (!row) {
        return 0
      }

      const stored = Number(row.passedCount) || 0
      const derived = this.auditJsonArrayCount(row.passedResultsJson)

      return Math.max(stored, derived)
    },

    /**
     * 获取实际拦截数量。
     */
    auditBlockedCount(row) {
      if (!row) {
        return 0
      }

      const stored = Number(row.blockedCount) || 0
      const derived = this.auditJsonArrayCount(row.blockedResultsJson)

      return Math.max(stored, derived)
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
.count-note { color: #909399; font-size: 12px; line-height: 1.6; margin: 12px 0; }
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
