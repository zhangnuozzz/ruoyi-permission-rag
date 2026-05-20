<template>
  <div class="app-container access-log-page">
    <el-card shadow="never" class="box-card">
      <div slot="header" class="card-header">
        <div>
          <div class="page-title">访问监控</div>
          <div class="page-subtitle">
            记录后台接口访问来源、访问路径、请求方式、访问状态和耗时，为安全审计与异常访问分析提供依据。
          </div>
        </div>
      </div>

      <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="78px">
        <el-form-item label="用户名" prop="userName">
          <el-input
            v-model="queryParams.userName"
            placeholder="请输入用户名"
            clearable
            style="width: 180px"
            @keyup.enter.native="handleQuery"
          />
        </el-form-item>

        <el-form-item label="访问IP" prop="ipaddr">
          <el-input
            v-model="queryParams.ipaddr"
            placeholder="请输入访问IP"
            clearable
            style="width: 180px"
            @keyup.enter.native="handleQuery"
          />
        </el-form-item>

        <el-form-item label="请求路径" prop="requestUri">
          <el-input
            v-model="queryParams.requestUri"
            placeholder="请输入请求路径"
            clearable
            style="width: 220px"
            @keyup.enter.native="handleQuery"
          />
        </el-form-item>

        <el-form-item label="请求方式" prop="requestMethod">
          <el-select v-model="queryParams.requestMethod" placeholder="请求方式" clearable style="width: 120px">
            <el-option label="GET" value="GET" />
            <el-option label="POST" value="POST" />
            <el-option label="PUT" value="PUT" />
            <el-option label="DELETE" value="DELETE" />
          </el-select>
        </el-form-item>

        <el-form-item label="访问状态" prop="status">
          <el-select v-model="queryParams.status" placeholder="访问状态" clearable style="width: 120px">
            <el-option label="成功" value="0" />
            <el-option label="失败" value="1" />
          </el-select>
        </el-form-item>

        <el-form-item label="访问时间">
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
            v-hasPermi="['system:accessLog:remove']"
          >删除</el-button>
        </el-col>
        <el-col :span="1.5">
          <el-button
            type="danger"
            plain
            icon="el-icon-delete-solid"
            size="mini"
            @click="handleClean"
            v-hasPermi="['system:accessLog:remove']"
          >清空</el-button>
        </el-col>
        <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
      </el-row>

      <el-table v-loading="loading" :data="accessLogList" border stripe @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="50" align="center" />
        <el-table-column label="ID" align="center" prop="accessId" width="80" />
        <el-table-column label="用户ID" align="center" prop="userId" width="90" />
        <el-table-column label="用户名" align="center" prop="userName" width="120" />
        <el-table-column label="访问IP" align="center" prop="ipaddr" width="140">
          <template slot-scope="scope">
            <el-tag size="small" effect="plain">{{ scope.row.ipaddr || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="请求路径" prop="requestUri" min-width="250" show-overflow-tooltip>
          <template slot-scope="scope">
            <el-tag v-if="isRagAudit(scope.row)" size="small" type="warning" effect="plain" class="mr5">
              RAG入库审计
            </el-tag>
            <span class="uri-text">{{ scope.row.requestUri }}</span>
          </template>
        </el-table-column>
        <el-table-column label="请求方式" align="center" prop="requestMethod" width="100">
          <template slot-scope="scope">
            <el-tag size="small" :type="methodTagType(scope.row.requestMethod)" effect="plain">
              {{ scope.row.requestMethod }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="访问状态" align="center" prop="status" width="100">
          <template slot-scope="scope">
            <el-tag v-if="scope.row.status === '0' || scope.row.status === 0" type="success" size="small" effect="plain">
              成功
            </el-tag>
            <el-tag v-else type="danger" size="small" effect="plain">
              失败
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="耗时" align="center" prop="costTime" width="100">
          <template slot-scope="scope">
            <span :class="scope.row.costTime > 1000 ? 'slow-time' : ''">{{ scope.row.costTime }} ms</span>
          </template>
        </el-table-column>
        <el-table-column label="访问时间" align="center" prop="createTime" width="170">
          <template slot-scope="scope">
            <span>{{ parseTime(scope.row.createTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="错误信息 / 审计说明" prop="errorMsg" min-width="360" show-overflow-tooltip>
          <template slot-scope="scope">
            <template v-if="scope.row.errorMsg">
              <el-tag
                v-if="scope.row.errorMsg.indexOf('RAG_FILE_UPLOAD_SUCCESS') !== -1"
                size="small"
                type="success"
                effect="plain"
                class="mr5"
              >
                入库成功
              </el-tag>
              <el-tag
                v-else-if="scope.row.errorMsg.indexOf('RAG_FILE_UPLOAD_FAIL') !== -1"
                size="small"
                type="danger"
                effect="plain"
                class="mr5"
              >
                入库失败
              </el-tag>
              <span class="error-text">{{ scope.row.errorMsg }}</span>
            </template>
            <span v-else class="empty-text">-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="150" fixed="right">
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
              v-hasPermi="['system:accessLog:remove']"
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

      <el-dialog title="访问日志详情" :visible.sync="detailOpen" width="760px" append-to-body>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="日志ID">{{ detail.accessId }}</el-descriptions-item>
          <el-descriptions-item label="用户">{{ detail.userName }}（{{ detail.userId }}）</el-descriptions-item>
          <el-descriptions-item label="访问IP">{{ detail.ipaddr }}</el-descriptions-item>
          <el-descriptions-item label="请求方式">{{ detail.requestMethod }}</el-descriptions-item>
          <el-descriptions-item label="请求路径" :span="2">
            <span class="uri-text">{{ detail.requestUri }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="访问状态">
            <el-tag :type="detail.status === '0' || detail.status === 0 ? 'success' : 'danger'" size="small">
              {{ detail.status === '0' || detail.status === 0 ? '成功' : '失败' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="耗时">{{ detail.costTime }} ms</el-descriptions-item>
          <el-descriptions-item label="访问时间" :span="2">{{ parseTime(detail.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="错误信息 / 审计说明" :span="2">
            <pre class="detail-box">{{ detail.errorMsg || '-' }}</pre>
          </el-descriptions-item>
        </el-descriptions>
      </el-dialog>
    </el-card>
  </div>
</template>

<script>
import { listAccessLog, delAccessLog, cleanAccessLog } from '@/api/system/accessLog'

export default {
  name: 'AccessLog',
  data() {
    return {
      loading: true,
      ids: [],
      multiple: true,
      showSearch: true,
      total: 0,
      accessLogList: [],
      daterangeCreateTime: [],
      detailOpen: false,
      detail: {},
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        userName: null,
        ipaddr: null,
        requestUri: null,
        requestMethod: null,
        status: null
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      listAccessLog(this.addDateRange(this.queryParams, this.daterangeCreateTime, 'CreateTime')).then(response => {
        this.accessLogList = response.rows
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
      this.ids = selection.map(item => item.accessId)
      this.multiple = !selection.length
    },
    handleDelete(row) {
      const accessIds = row.accessId || this.ids
      this.$confirm('是否确认删除访问监控日志编号为"' + accessIds + '"的数据项？', '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(function() {
        return delAccessLog(accessIds)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess('删除成功')
      })
    },
    handleClean() {
      this.$confirm('是否确认清空所有访问监控日志？', '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(function() {
        return cleanAccessLog()
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess('清空成功')
      })
    },
    methodTagType(method) {
      if (method === 'GET') {
        return 'success'
      }
      if (method === 'POST') {
        return 'primary'
      }
      if (method === 'PUT') {
        return 'warning'
      }
      if (method === 'DELETE') {
        return 'danger'
      }
      return 'info'
    },
    isRagAudit(row) {
      return row && row.requestUri && row.requestUri.indexOf('/rag/file/upload#audit') !== -1
    },
    handleView(row) {
      this.detail = row || {}
      this.detailOpen = true
    }
  }
}
</script>

<style scoped>
.access-log-page {
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

.uri-text {
  font-family: Menlo, Monaco, Consolas, monospace;
  color: #606266;
}

.slow-time {
  color: #e6a23c;
  font-weight: 600;
}

.error-text {
  color: #606266;
  font-size: 13px;
}

.empty-text {
  color: #c0c4cc;
}

.mr5 {
  margin-right: 5px;
}

.detail-box {
  margin: 0;
  padding: 10px 12px;
  min-height: 60px;
  max-height: 260px;
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
