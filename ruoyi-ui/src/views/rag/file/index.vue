<template>
  <div class="app-container rag-file-page">
    <el-card shadow="never" class="box-card">
      <div slot="header">
        <div class="page-title">RAG 文件入库</div>
        <div class="page-subtitle">
          通过若依平台上传带权限标签的文件，由后端代理转发至 fufu RAG Server，完成 MariaDB / MinIO / Milvus 三段式存储，并自动回写文档权限标签。
        </div>
      </div>

      <el-alert
        title="对接链路：若依前端 → 若依后端 8080 → fufu RAG Server 8081 → MariaDB / MinIO / Milvus → sys_rag_doc 权限标签回写"
        type="info"
        :closable="false"
        show-icon
        class="tips-alert"
      />

      <el-row :gutter="12" class="status-row">
        <el-col :span="6">
          <div class="status-card">
            <div class="status-label">平台入口</div>
            <div class="status-value">若依前端 1024</div>
            <el-tag size="mini" type="success">已接入</el-tag>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="status-card">
            <div class="status-label">代理服务</div>
            <div class="status-value">若依后端 8080</div>
            <el-tag size="mini" type="success">转发中</el-tag>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="status-card">
            <div class="status-label">RAG 服务</div>
            <div class="status-value">fufu Server 8081</div>
            <el-tag size="mini" type="success">已对接</el-tag>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="status-card">
            <div class="status-label">三段式存储</div>
            <div class="status-value">MariaDB / MinIO / Milvus</div>
            <el-tag size="mini" type="success">可验证</el-tag>
          </div>
        </el-col>
      </el-row>

      <el-form ref="form" :model="form" label-width="110px" size="small" class="upload-form">
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
            <div slot="tip" class="el-upload__tip">
              请选择需要入库的文档文件。当前版本由 fufu RAG Server 完成原文件备份、文本切分与向量写入。
            </div>
          </el-upload>
        </el-form-item>

        <el-form-item label="文件密级">
          <el-select v-model="form.securityLevel" placeholder="请选择文件密级" style="width: 260px">
            <el-option label="公开 PUBLIC" value="PUBLIC" />
            <el-option label="内部 INTERNAL" value="INTERNAL" />
            <el-option label="秘密 SECRET" value="SECRET" />
          </el-select>
        </el-form-item>

        <el-form-item label="权限标签">
          <el-select v-model="form.scopeCode" placeholder="请选择权限标签" style="width: 260px">
            <el-option label="PUBLIC" value="PUBLIC" />
            <el-option label="INTERNAL" value="INTERNAL" />
            <el-option label="DOC_ADMIN" value="DOC_ADMIN" />
            <el-option label="PROJECT_A" value="PROJECT_A" />
            <el-option label="RD" value="RD" />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="uploading" @click="handleUpload">上传入库</el-button>
          <el-button @click="resetUpload">重置</el-button>
          <el-button type="success" plain :loading="refreshing" @click="refreshAll">刷新三端列表</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card v-if="uploadResult" shadow="never" class="result-card">
      <div slot="header" class="table-header">
        <span>最近一次上传结果</span>
        <el-tag type="success" size="mini">上传成功</el-tag>
      </div>

      <el-descriptions :column="3" border>
        <el-descriptions-item label="文件ID">
          <span class="mono-text">{{ latestUpload.fileId || '-' }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="文件名">
          {{ latestUpload.fileName || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="切块数">
          <el-tag type="success" size="mini">{{ latestUpload.chunkCount || 0 }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="文件密级">
          <el-tag :type="tagType(latestUpload.securityLevel)" size="mini">{{ latestUpload.securityLevel || '-' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="权限标签">
          <el-tag type="info" size="mini">{{ latestUpload.scopeCode || '-' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="用户组">
          {{ latestUpload.groupName || latestUpload.groupId || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="MinIO 对象" :span="3">
          <span class="mono-text">{{ latestUpload.minioObjectName || '-' }}</span>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card shadow="never" class="result-card">
      <div slot="header" class="table-header">
        <span>MariaDB 文件元数据</span>
        <div>
          <el-tag size="mini" type="info">sys_rag_file</el-tag>
          <el-button size="mini" type="primary" plain @click="loadMariadb">刷新</el-button>
        </div>
      </div>

      <el-table v-loading="loadingMariadb" :data="mariadbList" border stripe>
        <el-table-column label="文件ID" prop="fileId" width="230" show-overflow-tooltip>
          <template slot-scope="scope">
            <span class="mono-text">{{ scope.row.fileId }}</span>
          </template>
        </el-table-column>
        <el-table-column label="文件名" prop="fileName" min-width="160" show-overflow-tooltip />
        <el-table-column label="上传用户" prop="uploadUserName" width="110" />
        <el-table-column label="密级" prop="securityLevel" width="110">
          <template slot-scope="scope">
            <el-tag size="mini" :type="tagType(scope.row.securityLevel)">{{ scope.row.securityLevel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="权限标签" prop="scopeCode" width="120">
          <template slot-scope="scope">
            <el-tag size="mini" type="info">{{ scope.row.scopeCode }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="用户组" prop="groupName" width="120" show-overflow-tooltip />
        <el-table-column label="MinIO对象" prop="minioObjectName" min-width="260" show-overflow-tooltip>
          <template slot-scope="scope">
            <span class="mono-text">{{ scope.row.minioObjectName }}</span>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" prop="createTime" width="170">
          <template slot-scope="scope">
            <span>{{ formatTime(scope.row.createTime) }}</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card shadow="never" class="result-card">
      <div slot="header" class="table-header">
        <span>Milvus 切块内容</span>
        <div>
          <el-tag size="mini" type="info">rag_file_chunks</el-tag>
          <el-button size="mini" type="primary" plain @click="loadMilvus">刷新</el-button>
        </div>
      </div>

      <el-table v-loading="loadingMilvus" :data="milvusList" border stripe>
        <el-table-column label="序号" type="index" width="60" align="center" />
        <el-table-column label="切块ID" prop="chunk_id" width="260" show-overflow-tooltip>
          <template slot-scope="scope">
            <span class="mono-text">{{ scope.row.chunk_id || scope.row.chunkId || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="文件ID" prop="file_id" width="230" show-overflow-tooltip>
          <template slot-scope="scope">
            <span class="mono-text">{{ scope.row.file_id || scope.row.fileId || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="密级" width="110">
          <template slot-scope="scope">
            <el-tag size="mini" :type="tagType(scope.row.security_level || scope.row.securityLevel)">
              {{ scope.row.security_level || scope.row.securityLevel || '-' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="权限标签" width="120">
          <template slot-scope="scope">
            <el-tag size="mini" type="info">
              {{ scope.row.scope_code || scope.row.scopeCode || '-' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="内容摘要" min-width="360" show-overflow-tooltip>
          <template slot-scope="scope">
            {{ contentPreview(scope.row.content || stringify(scope.row)) }}
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card shadow="never" class="result-card">
      <div slot="header" class="table-header">
        <span>MinIO 原始文件对象</span>
        <div>
          <el-tag size="mini" type="info">rag-files bucket</el-tag>
          <el-button size="mini" type="primary" plain @click="loadMinio">刷新</el-button>
        </div>
      </div>

      <el-table v-loading="loadingMinio" :data="minioList" border stripe>
        <el-table-column label="序号" type="index" width="60" align="center" />
        <el-table-column label="对象名" prop="objectName" min-width="360" show-overflow-tooltip>
          <template slot-scope="scope">
            <span class="mono-text">{{ scope.row.objectName || scope.row.name || stringify(scope.row) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="大小" prop="size" width="120">
          <template slot-scope="scope">
            {{ formatSize(scope.row.size) }}
          </template>
        </el-table-column>
        <el-table-column label="ETag" prop="etag" width="220" show-overflow-tooltip>
          <template slot-scope="scope">
            <span class="mono-text">{{ scope.row.etag || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="最后修改时间" prop="lastModified" width="190">
          <template slot-scope="scope">
            {{ formatTime(scope.row.lastModified) }}
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
      refreshing: false,
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
  computed: {
    latestUpload() {
      if (!this.uploadResult) {
        return {}
      }
      return this.uploadResult.data || this.uploadResult || {}
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
        this.$modal.msgSuccess('文件上传入库成功，三端列表已自动刷新')
        this.refreshAll()
      }).finally(() => {
        this.uploading = false
      })
    },
    refreshAll() {
      this.refreshing = true
      Promise.all([
        this.loadMariadb(),
        this.loadMilvus(),
        this.loadMinio()
      ]).finally(() => {
        this.refreshing = false
      })
    },
    loadMariadb() {
      this.loadingMariadb = true
      return listRagFileMariadb().then(response => {
        const result = response.data || response
        this.mariadbList = result.data || []
      }).catch(() => {
        this.mariadbList = []
      }).finally(() => {
        this.loadingMariadb = false
      })
    },
    loadMilvus() {
      this.loadingMilvus = true
      return listRagFileMilvus(100).then(response => {
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
      return listRagFileMinio(100).then(response => {
        const result = response.data || response
        this.minioList = result.data || []
      }).catch(() => {
        this.minioList = []
      }).finally(() => {
        this.loadingMinio = false
      })
    },
    tagType(value) {
      if (value === 'SECRET') {
        return 'danger'
      }
      if (value === 'INTERNAL') {
        return 'warning'
      }
      if (value === 'PUBLIC') {
        return 'success'
      }
      return 'info'
    },
    contentPreview(value) {
      if (!value) {
        return '-'
      }
      const text = String(value).replace(/\s+/g, ' ')
      return text.length > 120 ? text.slice(0, 120) + '...' : text
    },
    formatSize(size) {
      const num = Number(size)
      if (!num && num !== 0) {
        return '-'
      }
      if (num < 1024) {
        return num + ' B'
      }
      if (num < 1024 * 1024) {
        return (num / 1024).toFixed(2) + ' KB'
      }
      return (num / 1024 / 1024).toFixed(2) + ' MB'
    },
    formatTime(value) {
      if (!value) {
        return '-'
      }
      const date = new Date(value)
      if (isNaN(date.getTime())) {
        return String(value)
      }
      const pad = n => n < 10 ? '0' + n : '' + n
      return date.getFullYear() + '-' +
        pad(date.getMonth() + 1) + '-' +
        pad(date.getDate()) + ' ' +
        pad(date.getHours()) + ':' +
        pad(date.getMinutes()) + ':' +
        pad(date.getSeconds())
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

.status-row {
  margin-bottom: 18px;
}

.status-card {
  padding: 12px 14px;
  border: 1px solid #ebeef5;
  background: #f8fafc;
  border-radius: 6px;
}

.status-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 6px;
}

.status-value {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}

.upload-form {
  margin-top: 4px;
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

.mono-text {
  font-family: Menlo, Monaco, Consolas, "Courier New", monospace;
  font-size: 12px;
}

::v-deep .el-table .cell {
  white-space: nowrap;
}
</style>
