<template>
  <div class="app-container rag-audit-page">
    <el-card shadow="never" class="box-card">
      <div slot="header" class="card-header">
        <div>
          <div class="page-title">RAG 检索审计日志</div>
          <div class="page-subtitle">
            记录每一次 RAG 安全检索请求的用户身份、用户组、权限标签、Metadata Filter、访问决策、拒绝原因和耗时，用于后续安全审计与权限追踪。
          </div>
        </div>
        <el-tag type="info" effect="plain">sys_rag_audit_log</el-tag>
      </div>

      <el-alert
        title="审计链路：检索问题 → 权限上下文 → 策略决策 → Metadata Filter → 二次过滤 → 审计留痕"
        type="info"
        :closable="false"
        show-icon
        class="tips-alert"
      />

      <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="82px">
        <el-form-item label="用户名" prop="userName">
          <el-input
            v-model="queryParams.userName"
            placeholder="请输入用户名"
            clearable
            style="width: 180px"
            @keyup.enter.native="handleQuery"
          />
        </el-form-item>

        <el-form-item label="查询内容" prop="queryText">
          <el-input
            v-model="queryParams.queryText"
            placeholder="请输入检索内容"
            clearable
            style="width: 220px"
            @keyup.enter.native="handleQuery"
          />
        </el-form-item>

        <el-form-item label="用户组" prop="groupCodes">
          <el-input
            v-model="queryParams.groupCodes"
            placeholder="请输入用户组编码"
            clearable
            style="width: 180px"
            @keyup.enter.native="handleQuery"
          />
        </el-form-item>

        <el-form-item label="权限标签" prop="scopeCodes">
          <el-input
            v-model="queryParams.scopeCodes"
            placeholder="请输入 scopeCode"
            clearable
            style="width: 180px"
            @keyup.enter.native="handleQuery"
          />
        </el-form-item>

        <el-form-item label="访问决策" prop="allowAccess">
          <el-select v-model="queryParams.allowAccess" placeholder="访问决策" clearable style="width: 120px">
            <el-option label="放行" value="1" />
            <el-option label="拒绝" value="0" />
          </el-select>
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
        </el-form-item>
      </el-form>

      <el-row :gutter="10" class="mb8">
        <el-col :span="1.5">
          <el-button
            type="danger"
            plain
            icon="el-icon-delete"
            size="mini"
            :disabled="multiple"
            @click="handleDelete"
            v-hasPermi="['system:log:remove']"
          >删除</el-button>
        </el-col>
        <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
      </el-row>

      <el-table
        v-loading="loading"
        :data="logList"
        border
        stripe
        class="audit-table"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="50" align="center" fixed="left" />
        <el-table-column label="ID" prop="id" width="80" align="center" fixed="left" />

        <el-table-column label="用户" min-width="150" show-overflow-tooltip>
          <template slot-scope="scope">
            <div>{{ scope.row.userName || '-' }}</div>
            <div class="sub-text">ID：{{ scope.row.userId || '-' }}</div>
          </template>
        </el-table-column>

        <el-table-column label="检索内容" prop="queryText" min-width="220" show-overflow-tooltip>
          <template slot-scope="scope">
            <span class="query-text">{{ scope.row.queryText || '-' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="用户组上下文" prop="groupCodes" min-width="220" show-overflow-tooltip>
          <template slot-scope="scope">
            <el-tag
              v-for="item in splitCodes(scope.row.groupCodes)"
              :key="item"
              size="mini"
              type="info"
              effect="plain"
              class="tag-item"
            >
              {{ item }}
            </el-tag>
            <span v-if="splitCodes(scope.row.groupCodes).length === 0" class="empty-text">-</span>
          </template>
        </el-table-column>

        <el-table-column label="可访问标签" prop="scopeCodes" min-width="190" show-overflow-tooltip>
          <template slot-scope="scope">
            <el-tag
              v-for="item in splitCodes(scope.row.scopeCodes)"
              :key="item"
              size="mini"
              type="success"
              effect="plain"
              class="tag-item"
            >
              {{ item }}
            </el-tag>
            <span v-if="splitCodes(scope.row.scopeCodes).length === 0" class="empty-text">-</span>
          </template>
        </el-table-column>

        <el-table-column label="Metadata Filter" prop="metadataFilter" min-width="300" show-overflow-tooltip>
          <template slot-scope="scope">
            <span class="mono-text">{{ scope.row.metadataFilter || '-' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="访问决策" prop="allowAccess" width="110" align="center">
          <template slot-scope="scope">
            <el-tag :type="scope.row.allowAccess === '1' ? 'success' : 'danger'" size="small" effect="plain">
              {{ scope.row.allowAccess === '1' ? '放行' : '拒绝' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="拒绝原因" prop="denyReasons" min-width="180" show-overflow-tooltip>
          <template slot-scope="scope">
            <span v-if="scope.row.denyReasons" class="deny-text">{{ scope.row.denyReasons }}</span>
            <span v-else class="empty-text">-</span>
          </template>
        </el-table-column>

        <el-table-column label="耗时" prop="costTime" width="100" align="center">
          <template slot-scope="scope">
            <span :class="scope.row.costTime > 1000 ? 'slow-time' : ''">{{ scope.row.costTime || 0 }} ms</span>
          </template>
        </el-table-column>

        <el-table-column label="审计时间" prop="createTime" width="170" align="center">
          <template slot-scope="scope">
            <span>{{ parseTime(scope.row.createTime) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" align="center" width="150" fixed="right">
          <template slot-scope="scope">
            <el-button
              size="mini"
              type="text"
              icon="el-icon-view"
              @click="handleView(scope.row)"
            >详情</el-button>
            <el-button
              size="mini"
              type="text"
              icon="el-icon-delete"
              @click="handleDelete(scope.row)"
              v-hasPermi="['system:log:remove']"
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

      <el-dialog title="RAG 检索审计详情" :visible.sync="detailOpen" width="860px" append-to-body>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="审计ID">{{ detail.id }}</el-descriptions-item>
          <el-descriptions-item label="用户">{{ detail.userName }}（{{ detail.userId }}）</el-descriptions-item>

          <el-descriptions-item label="访问决策">
            <el-tag :type="detail.allowAccess === '1' ? 'success' : 'danger'" size="small">
              {{ detail.allowAccess === '1' ? '放行' : '拒绝' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="耗时">{{ detail.costTime || 0 }} ms</el-descriptions-item>

          <el-descriptions-item label="检索内容" :span="2">
            <pre class="detail-box">{{ detail.queryText || '-' }}</pre>
          </el-descriptions-item>

          <el-descriptions-item label="用户组上下文" :span="2">
            <el-tag
              v-for="item in splitCodes(detail.groupCodes)"
              :key="item"
              size="small"
              type="info"
              effect="plain"
              class="tag-item"
            >
              {{ item }}
            </el-tag>
            <span v-if="splitCodes(detail.groupCodes).length === 0" class="empty-text">-</span>
          </el-descriptions-item>

          <el-descriptions-item label="可访问标签" :span="2">
            <el-tag
              v-for="item in splitCodes(detail.scopeCodes)"
              :key="item"
              size="small"
              type="success"
              effect="plain"
              class="tag-item"
            >
              {{ item }}
            </el-tag>
            <span v-if="splitCodes(detail.scopeCodes).length === 0" class="empty-text">-</span>
          </el-descriptions-item>

          <el-descriptions-item label="Metadata Filter" :span="2">
            <pre class="detail-box">{{ detail.metadataFilter || '-' }}</pre>
          </el-descriptions-item>

          <el-descriptions-item label="拒绝原因" :span="2">
            <pre class="detail-box">{{ detail.denyReasons || '-' }}</pre>
          </el-descriptions-item>

          <el-descriptions-item label="审计时间" :span="2">{{ parseTime(detail.createTime) }}</el-descriptions-item>
        </el-descriptions>
      </el-dialog>
    </el-card>
  </div>
</template>

<script>
import { listLog, getLog, delLog } from '@/api/system/log'

export default {
  name: 'RagAuditLog',
  data() {
    return {
      loading: true,
      ids: [],
      multiple: true,
      showSearch: true,
      total: 0,
      logList: [],
      daterangeCreateTime: [],
      detailOpen: false,
      detail: {},
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        userName: null,
        queryText: null,
        groupCodes: null,
        scopeCodes: null,
        allowAccess: null
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      listLog(this.addDateRange(this.queryParams, this.daterangeCreateTime, 'CreateTime')).then(response => {
        this.logList = response.rows
        this.total = response.total
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
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.id)
      this.multiple = !selection.length
    },
    handleView(row) {
      getLog(row.id).then(response => {
        this.detail = response.data || row
        this.detailOpen = true
      })
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
    },
    splitCodes(value) {
      if (!value) {
        return []
      }
      return String(value).split(',').map(item => item.trim()).filter(Boolean)
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
  font-size: 13px;
  color: #909399;
}

.tips-alert {
  margin-bottom: 16px;
}

.audit-table {
  width: 100%;
}

.sub-text {
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
}

.query-text {
  color: #303133;
}

.mono-text {
  font-family: Menlo, Monaco, Consolas, "Courier New", monospace;
  font-size: 12px;
  color: #606266;
}

.tag-item {
  margin-right: 6px;
  margin-bottom: 4px;
}

.empty-text {
  color: #c0c4cc;
}

.deny-text {
  color: #f56c6c;
}

.slow-time {
  color: #e6a23c;
  font-weight: 600;
}

.detail-box {
  margin: 0;
  padding: 10px 12px;
  max-height: 240px;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-all;
  background: #f5f7fa;
  border-radius: 4px;
  color: #606266;
  font-size: 12px;
  line-height: 1.6;
}

::v-deep .el-table .cell {
  white-space: nowrap;
}
</style>
