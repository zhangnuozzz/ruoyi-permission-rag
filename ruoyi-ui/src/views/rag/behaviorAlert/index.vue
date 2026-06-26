<template>
  <div class="app-container">
    <el-card shadow="never" class="box-card">
      <div slot="header" class="clearfix">
        <span class="page-title">RAG 行为分析告警</span>
        <el-tag style="float: right" type="danger">sys_rag_behavior_alert</el-tag>
      </div>

      <div class="tip-box">
        <i class="el-icon-warning-outline"></i>
        行为分析链路：审计日志 → 规则分析 → 告警入库 → 页面查看。当前支持拒绝访问、二次过滤拦截、异常耗时三类告警。
      </div>

      <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" label-width="90px">
        <el-form-item label="用户名" prop="userName">
          <el-input
            v-model="queryParams.userName"
            placeholder="请输入用户名"
            clearable
            @keyup.enter.native="handleQuery"
          />
        </el-form-item>

        <el-form-item label="告警类型" prop="alertType">
          <el-select v-model="queryParams.alertType" placeholder="告警类型" clearable>
            <el-option label="拒绝访问" value="DENY_ACCESS" />
            <el-option label="二次过滤拦截" value="SECOND_FILTER_BLOCK" />
            <el-option label="异常耗时" value="SLOW_QUERY" />
            <el-option label="普通访问" value="NORMAL_ACCESS" />
          </el-select>
        </el-form-item>

        <el-form-item label="告警等级" prop="alertLevel">
          <el-select v-model="queryParams.alertLevel" placeholder="告警等级" clearable>
            <el-option label="高危" value="high" />
            <el-option label="中危" value="medium" />
            <el-option label="低危" value="low" />
          </el-select>
        </el-form-item>

        <el-form-item label="处理状态" prop="status">
          <el-select v-model="queryParams.status" placeholder="处理状态" clearable>
            <el-option label="未处理" value="unhandled" />
            <el-option label="已处理" value="handled" />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
          <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
          <el-button type="danger" icon="el-icon-cpu" size="mini" @click="handleAnalyze">行为分析</el-button>
          <el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport">导出</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="alertList" border>
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column label="告警ID" align="center" prop="id" width="90" />
        <el-table-column label="来源日志ID" align="center" prop="sourceLogId" width="110" />
        <el-table-column label="用户" align="center" prop="userName" width="120" />
        <el-table-column label="检索内容" align="center" prop="queryText" min-width="180" show-overflow-tooltip />
        <el-table-column label="告警类型" align="center" prop="alertType" width="150">
          <template slot-scope="scope">
            <el-tag v-if="scope.row.alertType === 'DENY_ACCESS'" type="danger">拒绝访问</el-tag>
            <el-tag v-else-if="scope.row.alertType === 'SECOND_FILTER_BLOCK'" type="warning">二次过滤拦截</el-tag>
            <el-tag v-else-if="scope.row.alertType === 'SLOW_QUERY'" type="info">异常耗时</el-tag>
            <el-tag v-else-if="scope.row.alertType === 'NORMAL_ACCESS'" type="success">普通访问</el-tag>
            <el-tag v-else>{{ scope.row.alertType }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="等级" align="center" prop="alertLevel" width="100">
          <template slot-scope="scope">
            <el-tag v-if="scope.row.alertLevel === 'high'" type="danger">高危</el-tag>
            <el-tag v-else-if="scope.row.alertLevel === 'medium'" type="warning">中危</el-tag>
            <el-tag v-else type="info">低危</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="告警原因" align="center" prop="alertReason" min-width="220" show-overflow-tooltip />
        <el-table-column label="耗时(ms)" align="center" prop="costTime" width="100" />
        <el-table-column label="处理状态" align="center" prop="status" width="100">
          <template slot-scope="scope">
            <el-tag v-if="scope.row.status === 'handled'" type="success">已处理</el-tag>
            <el-tag v-else type="danger">未处理</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="告警时间" align="center" prop="createTime" width="170" />
        <el-table-column label="操作" align="center" width="100">
          <template slot-scope="scope">
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
  </div>
</template>

<script>
import { listBehaviorAlert, analyzeBehaviorAlert, delBehaviorAlert, exportBehaviorAlert } from '@/api/rag/behaviorAlert'

export default {
  name: 'BehaviorAlert',
  data() {
    return {
      loading: true,
      total: 0,
      alertList: [],
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
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      listBehaviorAlert(this.queryParams).then(response => {
        this.alertList = response.rows
        this.total = response.total
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
      this.$confirm('是否确认根据当前 RAG 审计日志执行行为分析并生成告警？', '提示', {
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
    handleDelete(row) {
      const ids = row.id
      this.$confirm('是否确认删除告警编号为"' + ids + '"的数据项?', '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(function() {
        return delBehaviorAlert(ids)
      }).then(() => {
        this.getList()
        this.msgSuccess('删除成功')
      })
    }
  }
}
</script>

<style scoped>
.page-title {
  font-size: 18px;
  font-weight: 600;
}
.tip-box {
  margin-bottom: 16px;
  padding: 10px 14px;
  background: #f5f7fa;
  color: #606266;
  border-radius: 4px;
  font-size: 13px;
}
</style>
