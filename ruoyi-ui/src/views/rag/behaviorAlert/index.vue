<template>
  <div class="app-container behavior-alert-page">
    <el-card shadow="never" class="box-card">
      <div slot="header" class="card-header">
        <div>
          <div class="page-title">RAG 行为分析告警中心</div>
          <div class="page-subtitle">
            基于 RAG 审计日志自动识别拒绝访问、高风险查询、敏感词查询、topK 异常、大量结果拦截与慢查询等安全行为。
          </div>
        </div>
        <el-tag type="danger" effect="plain">sys_rag_behavior_alert</el-tag>
      </div>

      <el-alert
        title="分析链路：审计日志 → 风险评分 → 行为规则匹配 → 告警生成 → 安全处置"
        type="warning"
        :closable="false"
        show-icon
        class="tips-alert"
      />

      <el-row :gutter="12" class="summary-row">
        <el-col :span="6">
          <el-card shadow="never" class="summary-card">
            <div class="summary-label">当前页告警数</div>
            <div class="summary-value">{{ alertList.length }}</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="never" class="summary-card">
            <div class="summary-label">严重 / 高危</div>
            <div class="summary-value danger">{{ criticalCount + highCount }}</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="never" class="summary-card">
            <div class="summary-label">中危告警</div>
            <div class="summary-value warning">{{ mediumCount }}</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="never" class="summary-card">
            <div class="summary-label">未处理</div>
            <div class="summary-value danger">{{ unhandledCount }}</div>
          </el-card>
        </el-col>
      </el-row>

      <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" label-width="90px">
        <el-form-item label="用户名" prop="userName">
          <el-input
            v-model="queryParams.userName"
            placeholder="请输入用户名"
            clearable
            style="width: 160px"
            @keyup.enter.native="handleQuery"
          />
        </el-form-item>

        <el-form-item label="告警类型" prop="alertType">
          <el-select v-model="queryParams.alertType" placeholder="告警类型" clearable style="width: 180px">
            <el-option label="严重风险查询" value="CRITICAL_RISK_QUERY" />
            <el-option label="高风险查询" value="HIGH_RISK_QUERY" />
            <el-option label="拒绝访问" value="DENY_ACCESS" />
            <el-option label="敏感词查询" value="SENSITIVE_QUERY" />
            <el-option label="topK 过大" value="LARGE_TOPK_QUERY" />
            <el-option label="大量结果拦截" value="MASSIVE_RESULT_BLOCK" />
            <el-option label="二次过滤拦截" value="SECOND_FILTER_BLOCK" />
            <el-option label="异常耗时" value="SLOW_QUERY" />
            <el-option label="普通访问" value="NORMAL_ACCESS" />
          </el-select>
        </el-form-item>

        <el-form-item label="告警等级" prop="alertLevel">
          <el-select v-model="queryParams.alertLevel" placeholder="告警等级" clearable style="width: 130px">
            <el-option label="严重" value="critical" />
            <el-option label="高危" value="high" />
            <el-option label="中危" value="medium" />
            <el-option label="低危" value="low" />
          </el-select>
        </el-form-item>

        <el-form-item label="处理状态" prop="status">
          <el-select v-model="queryParams.status" placeholder="处理状态" clearable style="width: 130px">
            <el-option label="未处理" value="unhandled" />
            <el-option label="已处理" value="handled" />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button type="danger" icon="el-icon-cpu" size="mini" @click="handleAnalyze">触发行为分析</el-button>
          <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
          <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
          <el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport">导出</el-button>
          <el-button type="info" plain icon="el-icon-document" size="mini" @click="handleSyslogExport">导出syslog</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="alertList" border>
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column label="告警ID" align="center" prop="id" width="90" />
        <el-table-column label="来源日志ID" align="center" prop="sourceLogId" width="110" />
        <el-table-column label="用户" align="center" prop="userName" width="110" />
        <el-table-column label="检索内容" align="center" prop="queryText" min-width="190" show-overflow-tooltip />

        <el-table-column label="告警类型" align="center" prop="alertType" min-width="160">
          <template slot-scope="scope">
            <el-tag :type="alertTypeTag(scope.row.alertType)" size="small">
              {{ alertTypeName(scope.row.alertType) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="等级" align="center" prop="alertLevel" width="100">
          <template slot-scope="scope">
            <el-tag :type="alertLevelTag(scope.row.alertLevel)" size="small">
              {{ alertLevelName(scope.row.alertLevel) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="访问决策" align="center" prop="allowAccess" width="100">
          <template slot-scope="scope">
            <el-tag :type="scope.row.allowAccess === '1' ? 'success' : 'danger'" size="small">
              {{ scope.row.allowAccess === '1' ? '放行' : '拒绝' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="告警原因" align="center" prop="alertReason" min-width="260" show-overflow-tooltip />
        <el-table-column label="风险摘要" align="center" prop="remark" min-width="240" show-overflow-tooltip />
        <el-table-column label="耗时(ms)" align="center" prop="costTime" width="100" />

        <el-table-column label="处理状态" align="center" prop="status" width="100">
          <template slot-scope="scope">
            <el-tag v-if="scope.row.status === 'handled'" type="success" size="small">已处理</el-tag>
            <el-tag v-else type="danger" size="small">未处理</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="告警时间" align="center" prop="createTime" width="170" />

        <el-table-column label="操作" align="center" width="210" fixed="right">
          <template slot-scope="scope">
            <el-button size="mini" type="text" icon="el-icon-view" @click="handleView(scope.row)">详情</el-button>
            <el-button
              v-if="scope.row.status !== 'handled'"
              size="mini"
              type="text"
              icon="el-icon-check"
              @click="openHandleDialog(scope.row)"
            >处理</el-button>
            <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <pagination
        v-show="total>0"
        :total="total"
        :page.sync="queryParams.pageNum"
        :limit.sync="queryParams.pageSize"
        @pagination="getList"
      />
    </el-card>

    <el-dialog title="行为告警详情" :visible.sync="detailOpen" width="760px" append-to-body>
      <div class="alert-detail">
        <div class="detail-header">
          <div>
            <div class="detail-title">
              {{ alertTypeName(detail.alertType) }}
            </div>
            <div class="detail-subtitle">
              来源日志ID：{{ detail.sourceLogId || '暂无' }} ｜ 告警ID：{{ detail.id || '暂无' }}
            </div>
          </div>
          <div>
            <el-tag :type="alertLevelTag(detail.alertLevel)" size="medium">
              {{ alertLevelName(detail.alertLevel) }}
            </el-tag>
            <el-tag :type="detail.allowAccess === '1' ? 'success' : 'danger'" size="medium" class="ml8">
              {{ detail.allowAccess === '1' ? '放行' : '拒绝' }}
            </el-tag>
          </div>
        </div>

        <el-row :gutter="12" class="detail-grid">
          <el-col :span="8">
            <div class="detail-card">
              <div class="detail-label">用户</div>
              <div class="detail-value">{{ detail.userName || '暂无' }}</div>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="detail-card">
              <div class="detail-label">处理状态</div>
              <div class="detail-value">
                <el-tag :type="detail.status === 'handled' ? 'success' : 'danger'" size="small">
                  {{ detail.status === 'handled' ? '已处理' : '未处理' }}
                </el-tag>
              </div>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="detail-card">
              <div class="detail-label">告警时间</div>
              <div class="detail-value">{{ detail.createTime || '暂无' }}</div>
            </div>
          </el-col>
        </el-row>

        <div class="detail-section">
          <div class="section-title">检索内容</div>
          <div class="section-content">{{ detail.queryText || '暂无' }}</div>
        </div>

        <div class="detail-section">
          <div class="section-title">告警原因</div>
          <div class="section-content danger-text">{{ detail.alertReason || '暂无' }}</div>
        </div>

        <div class="detail-section">
          <div class="section-title">风险摘要</div>
          <pre class="remark-box">{{ formatRemark(detail.remark) }}</pre>
        </div>

        <div class="detail-section">
          <div class="section-title">处置信息</div>
          <div class="section-content">
            <div>处理人：{{ detail.handledBy || '暂无' }}</div>
            <div>处理时间：{{ detail.handledTime || '暂无' }}</div>
            <div>处理说明：{{ detail.handleRemark || '暂无' }}</div>
          </div>
        </div>
      </div>

      <span slot="footer" class="dialog-footer">
        <el-button @click="detailOpen = false">关闭</el-button>
      </span>
    </el-dialog>

    <el-dialog title="处理行为告警" :visible.sync="handleOpen" width="620px" append-to-body>
      <el-form :model="handleForm" label-width="90px">
        <el-form-item label="告警ID">
          <el-input v-model="handleForm.id" disabled />
        </el-form-item>

        <el-form-item label="告警类型">
          <el-input :value="alertTypeName(handleForm.alertType)" disabled />
        </el-form-item>

        <el-form-item label="处置动作">
          <el-radio-group v-model="handleForm.action">
            <el-radio label="CONFIRM">确认</el-radio>
            <el-radio label="IGNORE">忽略</el-radio>
            <el-radio label="LIMIT_USER">限流</el-radio>
            <el-radio label="BLOCK_USER">封禁</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="处理说明">
          <el-input
            v-model="handleForm.handleRemark"
            type="textarea"
            :rows="5"
            placeholder="请输入处理说明，例如：已核查该告警，系统拦截符合预期，已完成处置。"
          />
        </el-form-item>
      </el-form>

      <span slot="footer" class="dialog-footer">
        <el-button @click="handleOpen = false">取消</el-button>
        <el-button type="primary" :loading="handleLoading" @click="submitHandle">确定处理</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { listBehaviorAlert, analyzeBehaviorAlert, handleBehaviorAlert, delBehaviorAlert, exportBehaviorAlert, exportBehaviorAlertSyslog } from '@/api/rag/behaviorAlert'

export default {
  name: 'BehaviorAlert',
  data() {
    return {
      loading: true,
      total: 0,
      alertList: [],
      detailOpen: false,
      detail: {},
      handleOpen: false,
      handleLoading: false,
      handleForm: {
        id: null,
        alertType: '',
        action: 'CONFIRM',
        handleRemark: ''
      },
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        userName: undefined,
        alertType: undefined,
        alertLevel: undefined,
        status: undefined
      }
    }
  },
  computed: {
    criticalCount() {
      return this.alertList.filter(item => item.alertLevel === 'critical').length
    },
    highCount() {
      return this.alertList.filter(item => item.alertLevel === 'high').length
    },
    mediumCount() {
      return this.alertList.filter(item => item.alertLevel === 'medium').length
    },
    unhandledCount() {
      return this.alertList.filter(item => item.status !== 'handled').length
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      listBehaviorAlert(this.queryParams).then(response => {
        this.alertList = response.rows || []
        this.total = response.total || 0
        this.loading = false
      })
    },

    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },

    resetQuery() {
      this.resetForm('queryForm')
      this.handleQuery()
    },

    handleAnalyze() {
      this.$confirm('是否确认根据当前 RAG 审计日志执行行为分析并生成告警？重复告警会自动忽略。', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        return analyzeBehaviorAlert()
      }).then(response => {
        this.msgSuccess(response.msg)
        this.getList()
      })
    },

    handleExport() {
      this.$confirm('是否确认导出 RAG 行为分析告警数据？', '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(function() {
        return exportBehaviorAlert({})
      }).then(response => {
        this.download(response.msg)
      })
    },

    handleSyslogExport() {
      exportBehaviorAlertSyslog(this.queryParams).then(response => {
        const text = response.syslog || ''
        const blob = new Blob([text], { type: 'text/plain;charset=utf-8' })
        const url = window.URL.createObjectURL(blob)
        const link = document.createElement('a')
        link.href = url
        link.download = 'vacp-rag-alert-syslog.log'
        link.click()
        window.URL.revokeObjectURL(url)
      })
    },

    handleView(row) {
      this.detail = row || {}
      this.detailOpen = true
    },

    openHandleDialog(row) {
      this.handleForm = {
        id: row.id,
        alertType: row.alertType,
        action: 'CONFIRM',
        handleRemark: '已核查该告警，系统安全策略与二次过滤结果符合预期，已完成处置。'
      }
      this.handleOpen = true
    },

    submitHandle() {
      if (!this.handleForm.handleRemark || this.handleForm.handleRemark.trim().length === 0) {
        this.$message.warning('请输入处理说明')
        return
      }

      this.handleLoading = true
      handleBehaviorAlert(this.handleForm.id, {
        handleRemark: this.handleForm.handleRemark
      }, this.handleForm.action).then(() => {
        this.msgSuccess('告警处理成功')
        this.handleOpen = false
        this.getList()
      }).finally(() => {
        this.handleLoading = false
      })
    },

    handleDelete(row) {
      const ids = row.id
      this.$confirm('是否确认删除告警编号为 "' + ids + '" 的数据项？', '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(function() {
        return delBehaviorAlert(ids)
      }).then(() => {
        this.getList()
        this.msgSuccess('删除成功')
      })
    },

    alertTypeName(type) {
      const map = {
        CRITICAL_RISK_QUERY: '严重风险查询',
        HIGH_RISK_QUERY: '高风险查询',
        DENY_ACCESS: '拒绝访问',
        SENSITIVE_QUERY: '敏感词查询',
        LARGE_TOPK_QUERY: 'topK过大',
        MASSIVE_RESULT_BLOCK: '大量结果拦截',
        SECOND_FILTER_BLOCK: '二次过滤拦截',
        SLOW_QUERY: '异常耗时',
        NORMAL_ACCESS: '普通访问'
      }
      return map[type] || type || '未知'
    },

    alertTypeTag(type) {
      const map = {
        CRITICAL_RISK_QUERY: 'danger',
        HIGH_RISK_QUERY: 'danger',
        DENY_ACCESS: 'danger',
        SENSITIVE_QUERY: 'warning',
        LARGE_TOPK_QUERY: 'info',
        MASSIVE_RESULT_BLOCK: 'warning',
        SECOND_FILTER_BLOCK: 'warning',
        SLOW_QUERY: 'info',
        NORMAL_ACCESS: 'success'
      }
      return map[type] || 'info'
    },

    alertLevelName(level) {
      const map = {
        critical: '严重',
        high: '高危',
        medium: '中危',
        low: '低危'
      }
      return map[level] || level || '未知'
    },

    alertLevelTag(level) {
      const map = {
        critical: 'danger',
        high: 'danger',
        medium: 'warning',
        low: 'info'
      }
      return map[level] || 'info'
    },

    formatRemark(remark) {
      if (!remark) {
        return '暂无'
      }
      return String(remark)
        .replace(/; /g, '\n')
        .replace(/;/g, '\n')
    }
  }
}
</script>

<style scoped>
.behavior-alert-page {
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
  font-size: 18px;
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

.summary-value.danger {
  color: #f56c6c;
}

.summary-value.warning {
  color: #e6a23c;
}

.ml8 {
  margin-left: 8px;
}

.alert-detail {
  padding: 4px 2px;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  margin-bottom: 14px;
  background: #f8fafc;
  border: 1px solid #ebeef5;
  border-radius: 6px;
}

.detail-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.detail-subtitle {
  margin-top: 6px;
  font-size: 13px;
  color: #909399;
}

.detail-grid {
  margin-bottom: 14px;
}

.detail-card {
  padding: 12px;
  background: #ffffff;
  border: 1px solid #ebeef5;
  border-radius: 6px;
}

.detail-label {
  font-size: 13px;
  color: #909399;
  margin-bottom: 8px;
}

.detail-value {
  font-size: 14px;
  color: #303133;
  word-break: break-all;
}

.detail-section {
  margin-bottom: 14px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  overflow: hidden;
}

.section-title {
  padding: 10px 12px;
  background: #f5f7fa;
  color: #606266;
  font-weight: 600;
  border-bottom: 1px solid #ebeef5;
}

.section-content {
  padding: 12px;
  line-height: 1.7;
  color: #303133;
  word-break: break-all;
}

.danger-text {
  color: #f56c6c;
}

.remark-box {
  margin: 0;
  padding: 12px;
  min-height: 80px;
  background: #f6f8fa;
  color: #303133;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-all;
  font-family: Menlo, Monaco, Consolas, "Courier New", monospace;
}

</style>
