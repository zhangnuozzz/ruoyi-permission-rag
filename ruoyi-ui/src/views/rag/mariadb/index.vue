<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" label-width="76px">
      <el-form-item label="文件名" prop="fileName">
        <el-input v-model="queryParams.fileName" placeholder="请输入文件名" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="文件密级" prop="securityLevel">
        <el-input v-model="queryParams.securityLevel" placeholder="请输入文件密级" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="权限标签" prop="scopeCode">
        <el-input v-model="queryParams.scopeCode" placeholder="请输入权限标签" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="rows">
      <el-table-column label="文件ID" align="center" prop="fileId" min-width="220" show-overflow-tooltip />
      <el-table-column label="文件名" align="center" prop="fileName" min-width="180" show-overflow-tooltip />
      <el-table-column label="上传用户" align="center" prop="uploadUserName" width="120" />
      <el-table-column label="文件密级" align="center" prop="securityLevel" width="120" />
      <el-table-column label="权限标签" align="center" prop="scopeCode" width="120" />
      <el-table-column label="用户组" align="center" prop="groupName" width="140" show-overflow-tooltip />
      <el-table-column label="MinIO对象" align="center" prop="minioObjectName" min-width="240" show-overflow-tooltip />
      <el-table-column label="创建时间" align="center" prop="createTime" width="180" />
    </el-table>
  </div>
</template>

<script>
import { listMariadbFiles } from '@/api/rag/file'

// MariaDB存储内容界面
// author: fufu
// date: 2026-05-13 12:37:47 CST
export default {
  name: 'RagMariadb',
  data() {
    return {
      loading: false,
      rows: [],
      queryParams: {
        fileName: undefined,
        securityLevel: undefined,
        scopeCode: undefined
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      listMariadbFiles(this.queryParams).then(response => {
        this.rows = response.data || []
      }).finally(() => {
        this.loading = false
      })
    },
    handleQuery() {
      this.getList()
    },
    resetQuery() {
      this.resetForm('queryForm')
      this.handleQuery()
    }
  }
}
</script>
