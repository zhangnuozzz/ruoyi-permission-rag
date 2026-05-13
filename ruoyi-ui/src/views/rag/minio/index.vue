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
      <el-table-column label="对象名称" align="center" prop="objectName" min-width="320" show-overflow-tooltip />
      <el-table-column label="大小" align="center" prop="size" width="120" />
      <el-table-column label="ETag" align="center" prop="etag" min-width="220" show-overflow-tooltip />
      <el-table-column label="最后修改时间" align="center" prop="lastModified" width="220" />
    </el-table>
  </div>
</template>

<script>
import { listMinioObjects } from '@/api/rag/file'

// MinIO存储内容界面
// author: fufu
// date: 2026-05-13 12:37:47 CST
export default {
  name: 'RagMinio',
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
      listMinioObjects(this.queryParams).then(response => {
        this.rows = response.data || []
      }).finally(() => {
        this.loading = false
      })
    }
  }
}
</script>
