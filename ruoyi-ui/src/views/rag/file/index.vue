<template>
  <div class="app-container rag-file-page">
    <el-card shadow="never" class="box-card">
      <div slot="header">
        <div class="page-title">RAG 文件入库</div>
        <div class="page-subtitle">
          上传带权限标签的知识文件，系统完成文件解析、原文件保存、向量切块写入及文档权限标签同步。
        </div>
      </div>

      <el-alert
        title="入库链路：文件提交 → 内容解析与切块 → 原文件保存 → 向量写入 → 文档权限标签同步"
        type="info"
        :closable="false"
        show-icon
        class="tips-alert"
      />

      <el-row :gutter="12" class="status-row">
        <el-col :span="6">
          <div class="status-card">
            <div class="status-label">管理入口</div>
            <div class="status-value">向量库访问控制系统</div>
            <el-tag size="mini" type="success">已接入</el-tag>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="status-card">
            <div class="status-label">入库服务</div>
            <div class="status-value">文件处理服务</div>
            <el-tag size="mini" type="success">转发中</el-tag>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="status-card">
            <div class="status-label">检索服务</div>
            <div class="status-value">RAG 检索服务</div>
            <el-tag size="mini" type="success">已对接</el-tag>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="status-card">
            <div class="status-label">知识存储</div>
            <div class="status-value">MariaDB / MinIO / Milvus</div>
            <el-tag size="mini" type="success">可验证</el-tag>
          </div>
        </el-col>
      </el-row>

      <el-form ref="form" :model="form" :disabled="uploading" label-width="110px" size="small" class="upload-form">
        <el-form-item label="选择文件">
          <el-upload
            ref="upload"
            action="#"
            :auto-upload="false"
            :disabled="uploading"
            :on-change="handleFileChange"
            :on-remove="handleFileRemove"
            :file-list="fileList"
          >
            <el-button size="mini" type="primary">选择文件</el-button>
            <div slot="tip" class="el-upload__tip">
              支持 TXT / PDF / Word / OFD / PNG / JPG / JPEG / BMP / GIF / TIF / TIFF / WEBP；图片会进行 OCR 后入库。
            </div>
          </el-upload>
        </el-form-item>

        <el-form-item label="文件密级">
          <el-select
            v-model="form.securityLevel"
            placeholder="请选择文件密级"
            style="width: 320px"
            @change="handleSecurityLevelChange"
          >
            <el-option
              label="公开 · 非密数据 PUBLIC"
              value="PUBLIC"
            />
            <el-option
              label="内部 · 工作秘密 INTERNAL"
              value="INTERNAL"
            />
            <el-option
              label="秘密 · 国家秘密 SECRET"
              value="SECRET"
            />
            <el-option
              label="机密 · 国家秘密 CONFIDENTIAL"
              value="CONFIDENTIAL"
            />
          </el-select>
        </el-form-item>

        <el-form-item
          v-if="requiresKnowledgeGroup"
          label="知悉用户组"
          required
        >
          <el-select
            v-model="form.groupCode"
            placeholder="请选择唯一知悉用户组"
            style="width: 320px"
            filterable
            clearable
          >
            <el-option
              v-for="group in groupOptions"
              :key="group.groupCode"
              :label="group.groupName + '（' + group.groupCode + '）'"
              :value="group.groupCode"
            />
          </el-select>

          <div class="permission-help">
            秘密级、机密级国家秘密必须绑定一个唯一知悉用户组。
            只有人员密级满足且属于该用户组时才可访问。
          </div>
        </el-form-item>

        <el-form-item
          v-else
          label="知悉用户组"
        >
          <el-input
            :value="nonSecretGroupText"
            disabled
            style="width: 320px"
          />

          <div class="permission-help">
            {{
              form.securityLevel === 'PUBLIC'
                ? '公开非密数据不受人员密级和用户组限制。'
                : '内部工作秘密只校验人员密级，不校验知悉用户组。'
            }}
          </div>
        </el-form-item>

        <el-form-item label="权限模型">
          <div class="access-model-box">
            <template v-if="form.securityLevel === 'PUBLIC'">
              <el-tag type="success" size="small">
                PUBLIC
              </el-tag>
              <span>所有用户均可访问</span>
            </template>

            <template v-else-if="form.securityLevel === 'INTERNAL'">
              <el-tag type="info" size="small">
                INTERNAL
              </el-tag>
              <span>人员密级 ≥ INTERNAL 即可访问</span>
            </template>

            <template v-else>
              <el-tag
                :type="
                  form.securityLevel === 'CONFIDENTIAL'
                    ? 'danger'
                    : 'warning'
                "
                size="small"
              >
                {{ form.securityLevel }}
              </el-tag>

              <span>
                人员密级 ≥ 文档密级
                AND
                文档用户组 ∈ 人员用户组集合
              </span>
            </template>
          </div>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="uploading" @click="handleUpload">上传入库</el-button>
          <el-button @click="resetUpload">重置</el-button>
          <el-button type="success" plain :loading="refreshing" @click="refreshAll">刷新存储状态</el-button>
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
        <el-descriptions-item label="文件类型">
          <el-tag type="info" size="mini">{{ latestUpload.fileType || '-' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="解析方式">
          <el-tag type="info" size="mini">{{ latestUpload.parseMethod || 'UTF8_TEXT' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="文件密级">
          <el-tag :type="tagType(latestUpload.securityLevel)" size="mini">{{ latestUpload.securityLevel || '-' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="兼容标签">
          <el-tag type="info" size="mini">{{ latestUpload.scopeCode || '-' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="知悉用户组">
          {{ latestUpload.groupName || latestUpload.groupId || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="MinIO 对象" :span="3">
          <span class="mono-text">{{ latestUpload.minioObjectName || '-' }}</span>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card shadow="never" class="result-card">
      <div slot="header" class="table-header">
        <span>文件元数据</span>
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
        <el-table-column label="类型" prop="fileType" width="90">
          <template slot-scope="scope">
            <el-tag size="mini" type="info">{{ scope.row.fileType || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="解析方式" prop="parseMethod" width="120" />
        <el-table-column label="上传用户" prop="uploadUserName" width="110" />
        <el-table-column label="密级" prop="securityLevel" width="110">
          <template slot-scope="scope">
            <el-tag size="mini" :type="tagType(scope.row.securityLevel)">{{ scope.row.securityLevel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="兼容标签" prop="scopeCode" width="130">
          <template slot-scope="scope">
            <el-tag size="mini" type="info">{{ scope.row.scopeCode }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="知悉用户组" prop="groupName" width="140" show-overflow-tooltip />
        <el-table-column label="切块数" prop="chunkCount" width="90" />
        <el-table-column label="向量化状态" prop="embeddingStatus" width="120">
          <template slot-scope="scope">
            <el-tag size="mini" :type="scope.row.embeddingStatus === 'SUCCESS' ? 'success' : 'warning'">
              {{ scope.row.embeddingStatus || '-' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="向量索引" prop="vectorIndexType" width="100" />
        <el-table-column label="元数据索引" prop="metadataIndexStatus" width="120" />
        <el-table-column label="文档密级" prop="docLevel" width="110" />
        <el-table-column label="唯一知悉用户组" prop="docGroup" width="160" />
        <el-table-column label="文档状态" prop="docStatus" width="110" />
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
        <span>向量切块数据</span>
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
        <el-table-column label="兼容标签" width="130">
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
        <span>原始文件对象</span>
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
import { uploadRagFile, listRagFileMariadb, listRagFileMilvus, listRagFileMinio, listRagUploadGroups } from '@/api/rag/file'

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

        // scopeCode 仅用于兼容 RAG Server 旧接口，
        // 不再由管理员手工配置。
        scopeCode: 'INTERNAL',

        // PUBLIC / INTERNAL 不需要知悉用户组。
        groupCode: ''
      },
      groupOptions: [],
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
    },

    /**
     * SECRET / CONFIDENTIAL 属于国家秘密，
     * 必须配置唯一知悉用户组。
     */
    requiresKnowledgeGroup() {
      return (
        this.form.securityLevel === 'SECRET' ||
        this.form.securityLevel === 'CONFIDENTIAL'
      )
    },

    nonSecretGroupText() {
      if (this.form.securityLevel === 'PUBLIC') {
        return '无需配置（公开非密数据）'
      }

      return '无需配置（内部工作秘密仅校验人员密级）'
    }
  },
  created() {
    this.loadGroups()
    this.refreshAll()
  },
  methods: {
    handleFileChange(file, fileList) {
      if (this.uploading) return
      this.uploadResult = null
      this.fileList = fileList.slice(-1)
      this.selectedFile = file.raw
    },
    handleFileRemove() {
      if (this.uploading) return
      this.fileList = []
      this.selectedFile = null
    },
    resetUpload() {
      if (this.uploading) return
      this.fileList = []
      this.selectedFile = null
      this.form.securityLevel = 'INTERNAL'

      // scopeCode 只保留给 fufu RAG Server 兼容使用
      this.form.scopeCode = 'INTERNAL'

      // INTERNAL 不绑定知悉用户组
      this.form.groupCode = ''

      this.uploadResult = null
      if (this.$refs.upload) {
        this.$refs.upload.clearFiles()
      }
    },
    /**
     * 根据文件密级自动维护兼容 scopeCode。
     *
     * PUBLIC：
     * scopeCode = PUBLIC
     *
     * INTERNAL：
     * scopeCode = INTERNAL
     *
     * SECRET / CONFIDENTIAL：
     * scopeCode = 唯一用户组编码
     *
     * scopeCode 不再承担独立权限含义。
     */
    handleSecurityLevelChange(level) {
      if (level === 'PUBLIC') {
        this.form.groupCode = ''
        this.form.scopeCode = 'PUBLIC'
        return
      }

      if (level === 'INTERNAL') {
        this.form.groupCode = ''
        this.form.scopeCode = 'INTERNAL'
        return
      }

      // 国家秘密切换密级时重新选择唯一知悉用户组
      this.form.groupCode = ''
      this.form.scopeCode = ''
    },

    handleUpload() {
      if (this.uploading) return
      if (!this.selectedFile) {
        this.$modal.msgWarning('请先选择文件')
        return
      }
      if (!this.form.securityLevel) {
        this.$modal.msgWarning('请选择文件密级')
        return
      }

      /*
       * SECRET / CONFIDENTIAL：
       * 必须选择唯一知悉用户组。
       */
      if (
        this.requiresKnowledgeGroup &&
        !this.form.groupCode
      ) {
        this.$modal.msgWarning(
          '秘密级、机密级国家秘密必须选择唯一知悉用户组'
        )
        return
      }

      /*
       * scopeCode 仅为兼容 fufu 当前接口自动生成：
       *
       * PUBLIC       -> PUBLIC
       * INTERNAL     -> INTERNAL
       * SECRET       -> groupCode
       * CONFIDENTIAL -> groupCode
       */
      if (this.form.securityLevel === 'PUBLIC') {
        this.form.scopeCode = 'PUBLIC'
      } else if (this.form.securityLevel === 'INTERNAL') {
        this.form.scopeCode = 'INTERNAL'
      } else {
        this.form.scopeCode = this.form.groupCode
      }

      const data = new FormData()

      data.append(
        'file',
        this.selectedFile
      )

      data.append(
        'securityLevel',
        this.form.securityLevel
      )

      data.append(
        'scopeCode',
        this.form.scopeCode
      )

      data.append(
        'groupCode',
        this.form.groupCode || ''
      )

      this.uploading = true
      return uploadRagFile(data).then(response => {
        this.uploadResult = response.data || response
        this.fileList = []
        this.selectedFile = null
        if (this.$refs.upload) this.$refs.upload.clearFiles()
        this.$modal.msgSuccess('文件上传入库成功')
        this.refreshAll()
      }).catch(error => {
        this.uploadResult = null
        const message = error && error.message ? error.message : '文件上传入库失败，请查看后端日志'
        this.$modal.msgError(message)
      }).finally(() => {
        this.uploading = false
      })
    },
    loadGroups() {
      return listRagUploadGroups().then(response => {
        this.groupOptions = (response.data || []).filter(group => group.groupCode !== 'GROUP_PUBLIC')
      }).catch(() => {
        this.groupOptions = []
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
        if (Array.isArray(result)) {
          this.mariadbList = result
        } else if (result && Array.isArray(result.data)) {
          this.mariadbList = result.data
        } else {
          this.mariadbList = []
        }
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
      if (value === 'CONFIDENTIAL') {
        return 'danger'
      }

      if (value === 'SECRET') {
        return 'warning'
      }

      if (value === 'INTERNAL') {
        return 'info'
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

.permission-help {
  margin-top: 6px;
  max-width: 650px;
  color: #909399;
  font-size: 12px;
  line-height: 20px;
}

.access-model-box {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 32px;
  padding: 8px 12px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  background: #fafafa;
  color: #606266;
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
