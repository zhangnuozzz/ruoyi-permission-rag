<template>
  <div class="app-container">
    <el-form ref="form" :model="form" :rules="rules" label-width="96px" size="small">
      <el-form-item label="文件" prop="file">
        <el-upload
          ref="upload"
          drag
          action=""
          :auto-upload="false"
          :limit="1"
          :file-list="fileList"
          :on-change="handleFileChange"
          :on-remove="handleFileRemove"
          :on-exceed="handleExceed"
        >
          <i class="el-icon-upload"></i>
          <div class="el-upload__text">将文件拖到此处，或<em>点击选择</em></div>
        </el-upload>
      </el-form-item>
      <el-form-item label="文件密级" prop="securityLevel">
        <el-select v-model="form.securityLevel" placeholder="请选择文件密级" clearable>
          <el-option label="公开" value="PUBLIC" />
          <el-option label="内部" value="INTERNAL" />
          <el-option label="秘密" value="SECRET" />
        </el-select>
      </el-form-item>
      <el-form-item label="权限标签" prop="scopeCode">
        <el-input v-model="form.scopeCode" placeholder="请输入文档级权限标签" clearable />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-upload2" :loading="uploading" @click="submitUpload">上传</el-button>
        <el-button icon="el-icon-refresh" @click="resetForm">重置</el-button>
      </el-form-item>
    </el-form>

    <el-descriptions v-if="result" class="mt20" title="上传结果" :column="2" border>
      <el-descriptions-item label="文件ID">{{ result.fileId }}</el-descriptions-item>
      <el-descriptions-item label="文件名">{{ result.fileName }}</el-descriptions-item>
      <el-descriptions-item label="切块数量">{{ result.chunkCount }}</el-descriptions-item>
      <el-descriptions-item label="权限标签">{{ result.scopeCode }}</el-descriptions-item>
      <el-descriptions-item label="MinIO对象">{{ result.minioObjectName }}</el-descriptions-item>
    </el-descriptions>
  </div>
</template>

<script>
import { uploadRagFile } from '@/api/rag/file'

// RAG上传文件界面
// author: fufu
// date: 2026-05-13 12:37:47 CST
export default {
  name: 'RagUpload',
  data() {
    return {
      uploading: false,
      fileList: [],
      result: null,
      form: {
        file: null,
        securityLevel: 'INTERNAL',
        scopeCode: 'INTERNAL'
      },
      rules: {
        file: [{ required: true, message: '请选择上传文件', trigger: 'change' }],
        securityLevel: [{ required: true, message: '请选择文件密级', trigger: 'change' }],
        scopeCode: [{ required: true, message: '请输入权限标签', trigger: 'blur' }]
      }
    }
  },
  methods: {
    handleFileChange(file, fileList) {
      this.fileList = fileList.slice(-1)
      this.form.file = file.raw
      this.$refs.form.validateField('file')
    },
    handleFileRemove() {
      this.fileList = []
      this.form.file = null
    },
    handleExceed() {
      this.$modal.msgWarning('一次只能上传一个文件')
    },
    submitUpload() {
      this.$refs.form.validate(valid => {
        if (!valid) {
          return
        }
        const data = new FormData()
        data.append('file', this.form.file)
        data.append('securityLevel', this.form.securityLevel)
        data.append('scopeCode', this.form.scopeCode)
        this.uploading = true
        uploadRagFile(data).then(response => {
          this.result = response.data
          this.$modal.msgSuccess('上传成功')
        }).finally(() => {
          this.uploading = false
        })
      })
    },
    resetForm() {
      this.fileList = []
      this.result = null
      this.form = {
        file: null,
        securityLevel: 'INTERNAL',
        scopeCode: 'INTERNAL'
      }
      this.$nextTick(() => this.$refs.form.clearValidate())
    }
  }
}
</script>

<style scoped>
.mt20 {
  margin-top: 20px;
}
</style>
