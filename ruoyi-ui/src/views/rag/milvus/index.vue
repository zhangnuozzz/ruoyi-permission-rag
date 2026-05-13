<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" label-width="68px">
      <el-form-item label="数量" prop="limit">
        <el-input-number v-model="queryParams.limit" :min="1" :max="500" controls-position="right" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="getList">刷新</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="rows">
      <el-table-column label="切块ID" align="center" prop="chunk_id" min-width="220" show-overflow-tooltip />
      <el-table-column label="文件ID" align="center" prop="file_id" min-width="220" show-overflow-tooltip />
      <el-table-column label="文件密级" align="center" prop="security_level" width="120" />
      <el-table-column label="权限标签" align="center" prop="scope_code" width="120" />
      <el-table-column label="用户组" align="center" prop="group_name" width="140" show-overflow-tooltip />
      <el-table-column label="切块内容" align="left" prop="content" min-width="360" show-overflow-tooltip />
    </el-table>
  </div>
</template>

<script>
import { listMilvusChunks } from '@/api/rag/file'

// Milvus存储内容界面
// author: fufu
// date: 2026-05-13 12:37:47 CST
export default {
  name: 'RagMilvus',
  data() {
    return {
      loading: false,
      rows: [],
      queryParams: {
        limit: 100
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      listMilvusChunks(this.queryParams).then(response => {
        this.rows = response.data || []
      }).finally(() => {
        this.loading = false
      })
    }
  }
}
</script>
