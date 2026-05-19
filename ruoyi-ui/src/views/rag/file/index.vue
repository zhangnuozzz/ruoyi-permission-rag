<template>
  <div class="app-container rag-file-page">
    <el-card shadow="never" class="box-card">
      <div slot="header">
        <div class="page-title">RAG 文件入库</div>
        <div class="page-subtitle">
          通过若依平台上传带权限标签的文件，由后端代理转发至 fufu RAG Server，完成 MariaDB / MinIO / Milvus 三段式存储。
        </div>
      </div>

      <el-alert
        title="对接链路：若依前端 → 若依后端 8080 → fufu RAG Server 8081 → MariaDB / MinIO / Milvus"
        type="info"
        :closable="false"
        show-icon
        class="tips-alert"
      />

      <el-form ref="form" :model="form" label-width="110px" size="small">
        <el-form-item label="选择文件">
          <el-upload
            ref="upload"
            action="#"
            :auto-upload="false"
            :limit="1"
            :on-change="handleFileChange"
            :on-remove="handleFileRemove"
            :file-list="fileList"
          >
            <el-button size="mini" type="primary">选择文件</el-button>
            <div slot="tip" class="el-upload__tip">请选择需要入库的文档文件，当前第一版由 RAG Server 负责切分与存储。</div>
          </el-upload>
        </el-form-item>

        <el-form-item label="文件密级">
          <el-select v-model="form.securityLevel" placeholder="请选择文件密级" style="width: 240px">
            <el-option label="公开 PUBLIC" value="PUBLIC" />
            <el-option label="内部 INTERNAL" value="INTERNAL" />
            <el-option label="秘密 SECRET" value="SECRET" />
          </el-select>
        </el-form-item>

        <el-form-item label="权限标签">
          <el-select v-model="form.scopeCode" placeholder="请选择权限标签" style="width: 240px">
            <el-option label="PUBLIC" value="PUBLIC" />
            <el-option label="INTERNAL" value="INTERNAL" />
            <el-option label="DOC_ADMIN" value="DOC_ADMIN" />
            <el-option label="RD" value="RD" />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="uploading" @click="handleUpload">上传入库</el-button>
          <el-button @click="resetUpload">重置</el-button>
          <el-button type="success" plain @click="refreshAll">刷新三端列表</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card v-if="uploadResult" shadow="never" class="result-card">
      <div slot="header">
        <span>最近一次上传结果</span>
      </div>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="状态">
          <el-tag type="success">上传成功</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="返回说明">
          {{ uploadResult.msg || uploadResult.message || '操作成功' }}
        </el-descriptions-item>
        <el-descriptions-item label="结果数据" :span="2">
          <pre class="json-box">{{ formatJson(uploadResult.data || uploadResult) }}</pre>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card shadow="never" class="result-card">
      <div slot="header" class="table-header">
        <span>MariaDB 文件元数据</span>
        <el-button size="mini" type="primary" plain @click="loadMariadb">刷新</el-button>
      </div>

      <el-table v-loading="loadingMariadb" :data="mariadbList" border stripe>
        <el-table-column label="文件ID" prop="fileId" min-width="180" show-overflow-tooltip />
        <el-table-column label="文件名" prop="fileName" min-width="180" show-overflow-tooltip />
        <el-table-column label="上传用户" prop="uploadUserName" width="120" />
        <el-table-column label="密级" prop="securityLevel" width="120">
          <template slot-scope="scope">
            <el-tag effect="plain">{{ scope.row.securityLevel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="权限标签" prop="scopeCode" width="120">
          <template slot-scope="scope">
            <el-tag type="success" effect="plain">{{ scope.row.scopeCode }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="用户组" prop="groupName" width="140" />
        <el-table-column label="MinIO对象" prop="minioObjectName" min-width="220" show-overflow-tooltip />
        <el-table-column label="创建时间" prop="createTime" width="170">
          <template slot-scope="scope">
            <span>{{ parseTime(scope.row.createTime) }}</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card shadow="never" class="result-card">
      <div slot="header" class="table-header">
        <span>Milvus 切块内容</span>
        <el-button size="mini" type="primary" plain @click="loadMilvus">刷新</el-button>
      </div>

      <el-table v-loading="loadingMilvus" :data="milvusList" border stripe>
        <el-table-column label="序号" type="index" width="60" align="center" />
        <el-table-column label="内容" min-width="360" show-overflow-tooltip>
          <template slot-scope="scope">
            {{ stringify(scope.row) }}
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card shadow="never" class="result-card">
      <div slot="header" class="table-header">
        <span>MinIO 原始文件对象</span>
        <el-button size="mini" type="primary" plain @click="loadMinio">刷新</el-button>
      </div>

      <el-table v-loading="loadingMinio" :data="minioList" border stripe>
        <el-table-column label="序号" type="index" width="60" align="center" />
        <el-table-column label="对象信息" min-width="360" show-overflow-tooltip>
          <template slot-scope="scope">
            {{ stringify(scope.row) }}
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script>
import { uploadRagFile, listRagFileMariadb, listRagFileMilvus, listRagFileMinio } from '@/api/rag/file'

export default {
  name: 'RagFile',
  data() {
    return {
      uploading: false,
      fileList: [],
      selectedFile: null,
      form: {
        securityLevel: 'INTERNAL',
        scopeCode: 'INTERNAL'
      },
      uploadResult: null,
      mariadbList: [],
      milvusList: [],
      minioList: [],
      loadingMariadb: false,
      loadingMilvus: false,
      loadingMinio: false
    }
  },
  created() {
    this.refreshAll()
  },
  methods: {
    handleFileChange(file, fileList) {
      this.fileList = fileList.slice(-1)
      this.selectedFile = file.raw
    },
    handleFileRemove() {
      this.fileList = []
      this.selectedFile = null
    },
    resetUpload() {
      this.fileList = []
      this.selectedFile = null
      this.form.securityLevel = 'INTERNAL'
      this.form.scopeCode = 'INTERNAL'
      this.uploadResult = null
      if (this.$refs.upload) {
        this.$refs.upload.clearFiles()
      }
    },
    handleUpload() {
      if (!this.selectedFile) {
        this.$modal.msgWarning('请先选择文件')
        return
      }
      if (!this.form.securityLevel || !this.form.scopeCode) {
        this.$modal.msgWarning('请选择文件密级和权限标签')
        return
      }

      const data = new FormData()
      data.append('file', this.selectedFile)
      data.append('securityLevel', this.form.securityLevel)
      data.append('scopeCode', this.form.scopeCode)

      this.uploading = true
      uploadRagFile(data).then(response => {
        this.uploadResult = response.data || response
        this.$modal.msgSuccess('文件上传入库成功')
        this.refreshAll()
      }).finally(() => {
        this.uploading = false
      })
    },
    refreshAll() {
      this.loadMariadb()
      this.loadMilvus()
      this.loadMinio()
    },
    loadMariadb() {
      this.loadingMariadb = true
      listRagFileMariadb().then(response => {
        const result = response.data || response
        this.mariadbList = result.data || []
      }).finally(() => {
        this.loadingMariadb = false
      })
    },
    loadMilvus() {
      this.loadingMilvus = true
      listRagFileMilvus(100).then(response => {
        const result = response.data || response
        this.milvusList = result.data || []
      }).catch(() => {
        this.milvusList = []
      }).finally(() => {
        this.loadingMilvus = false
      })
    },
    loadMinio() {
      this.loadingMinio = true
      listRagFileMinio(100).then(response => {
        const result = response.data || response
        this.minioList = result.data || []
      }).catch(() => {
        this.minioList = []
      }).finally(() => {
        this.loadingMinio = false
      })
    },
    formatJson(value) {
      try {
        return JSON.stringify(value, null, 2)
      } catch (e) {
        return String(value)
      }
    },
    stringify(value) {
      try {
        return JSON.stringify(value)
      } catch (e) {
        return String(value)
      }
    }
  }
}
</script>

<style scoped>
.rag-file-page {
  padding: 20px;
}

.box-card {
  border-radius: 6px;
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

.result-card {
  margin-top: 20px;
  border-radius: 6px;
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.json-box {
  margin: 0;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 4px;
  color: #606266;
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
