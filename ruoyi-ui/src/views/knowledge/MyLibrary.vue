<template>
  <div class="library-page">
    <div class="library-heading">
      <div>
        <div class="eyebrow">MY KNOWLEDGE BASE</div>
        <h1>我的知识库</h1>
        <p>上传知识文件并查看入库状态。公开文档可直接分享，内部文档按人员密级访问，国家秘密还需知悉用户组授权。</p>
      </div>
      <el-button type="primary" icon="el-icon-upload2" @click="openUpload">上传知识文件</el-button>
    </div>

    <div class="library-summary">
      <div class="summary-icon"><i class="el-icon-folder-opened" /></div>
      <div><span>我的文档</span><strong>{{ loadError ? '--' : total }}</strong></div>
      <div class="summary-caption">仅统计当前账号上传的文件</div>
    </div>

    <div class="library-panel">
      <div class="panel-heading">
        <div><h2>已上传文档</h2><p>查看解析与向量化结果，按需停用或重新启用自己的文档</p></div>
        <el-button icon="el-icon-refresh" size="small" :loading="loading" @click="loadFiles">刷新</el-button>
      </div>

      <div v-if="loadError" class="state-message error-state">
        <i class="el-icon-warning-outline" /> {{ loadError }}
        <el-button type="text" @click="loadFiles">重试</el-button>
      </div>
      <div v-else-if="!loading && !files.length" class="state-message empty-state">
        <i class="el-icon-folder-add" />
        <strong>还没有上传知识文件</strong>
        <span>上传后，系统将保存原文件、解析内容并写入向量库。</span>
        <el-button type="primary" plain size="small" @click="openUpload">上传第一份文档</el-button>
      </div>
      <div v-else class="table-scroll">
        <el-table v-loading="loading" :data="files" class="file-table" empty-text="暂无文档">
          <el-table-column label="文档" min-width="190">
            <template slot-scope="scope">
              <div class="file-name"><i class="el-icon-document" /><span :title="scope.row.fileName">{{ scope.row.fileName }}</span></div>
              <div class="file-meta">{{ scope.row.fileType || '文件' }} · {{ scope.row.createTime || '--' }}</div>
            </template>
          </el-table-column>
          <el-table-column label="访问范围" min-width="150">
            <template slot-scope="scope">
              <el-tag size="mini" effect="plain" :type="levelTag(scope.row.securityLevel)">{{ levelName(scope.row.securityLevel) }}</el-tag>
              <div class="file-meta">{{ accessDescription(scope.row) }}</div>
            </template>
          </el-table-column>
          <el-table-column label="向量切块" width="95" align="center">
            <template slot-scope="scope">{{ scope.row.chunkCount == null ? '--' : scope.row.chunkCount }}</template>
          </el-table-column>
          <el-table-column label="入库状态" width="105">
            <template slot-scope="scope"><span class="index-status" :class="isVectorReady(scope.row) ? 'ready' : 'pending'">{{ vectorStatus(scope.row) }}</span></template>
          </el-table-column>
          <el-table-column label="使用状态" width="100">
            <template slot-scope="scope"><span class="index-status" :class="scope.row.metadataStatus === 'ACTIVE' ? 'ready' : 'paused'">{{ scope.row.metadataStatus === 'ACTIVE' ? '已启用' : scope.row.metadataStatus === 'UNAVAILABLE' ? '待修复' : '已停用' }}</span></template>
          </el-table-column>
          <el-table-column label="操作" width="90" align="right">
            <template slot-scope="scope">
              <el-button type="text" :disabled="!!changingId || scope.row.metadataStatus === 'UNAVAILABLE'" :loading="changingId === scope.row.fileId" @click="toggleStatus(scope.row)">{{ scope.row.metadataStatus === 'ACTIVE' ? '停用' : '启用' }}</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <div v-if="total > pageSize" class="pagination-wrap">
        <el-pagination background layout="prev, pager, next" :current-page="pageNum" :page-size="pageSize" :total="total" @current-change="changePage" />
      </div>
    </div>

    <p v-if="!groupsLoading && (groupsError || (userLevelRank >= 3 && !groups.length))" class="group-tip">
      {{ groupsError ? '暂时无法读取人员密级和知悉用户组，请稍后重试。' : '当前账号没有可用的知悉用户组；公开和内部文档仍可按人员密级上传。' }}
      <el-button v-if="groupsError" type="text" @click="loadGroups">重试</el-button>
    </p>

    <el-dialog title="上传知识文件" :visible.sync="uploadVisible" width="520px" :close-on-click-modal="false" :close-on-press-escape="!uploading" :show-close="!uploading" :before-close="closeUpload" append-to-body>
      <div class="upload-intro">文件将进入知识库向量化流程；检索范围由文档密级和国家秘密的唯一知悉用户组决定。</div>
      <el-form label-position="top" class="upload-form" :disabled="uploading">
        <el-form-item label="选择文件" required>
          <el-upload ref="upload" action="#" :auto-upload="false" :disabled="uploading" :file-list="fileList" :on-change="selectFile" :on-remove="removeFile">
            <el-button size="small" icon="el-icon-document-add">选择文件</el-button>
            <div slot="tip" class="upload-tip">支持 TXT、MD、CSV、JSON、LOG、PDF、DOC、DOCX、OFD 及常见图片，单文件不超过 10 MB。</div>
          </el-upload>
        </el-form-item>
        <el-form-item label="文档密级" required>
          <el-select v-model="form.securityLevel" placeholder="选择文档密级" class="form-control" @change="onLevelChange">
            <el-option v-for="level in availableLevels" :key="level.value" :label="level.label" :value="level.value" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="requiresKnowledgeGroup" label="唯一知悉用户组" required>
          <el-select v-model="form.groupCode" placeholder="选择本人所属的知悉用户组" class="form-control">
            <el-option v-for="group in groups" :key="group.groupCode" :label="group.groupName + '（' + group.groupCode + '）'" :value="group.groupCode" />
          </el-select>
        </el-form-item>
        <div class="scope-note">{{ uploadRuleText }}</div>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button :disabled="uploading" @click="closeUpload()">取消</el-button>
        <el-button type="primary" :loading="uploading" :disabled="uploading || !selectedFile || !form.securityLevel || (requiresKnowledgeGroup && !form.groupCode)" @click="submitUpload">上传并入库</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { listMyKnowledge, listMyUploadGroups, uploadMyKnowledge, changeMyKnowledgeStatus } from '@/api/rag/myKnowledge'

const LEVELS = [
  { value: 'PUBLIC', label: '公开', rank: 1 },
  { value: 'INTERNAL', label: '内部', rank: 2 },
  { value: 'SECRET', label: '秘密', rank: 3 },
  { value: 'CONFIDENTIAL', label: '机密', rank: 4 }
]

export default {
  name: 'MyLibrary',
  data() {
    return {
      files: [], total: 0, pageNum: 1, pageSize: 10, loading: false, loadError: '', fileRequestId: 0, groupRequestId: 0,
      groups: [], groupsLoading: false, groupsError: false, userSecretLevel: 'PUBLIC', uploadVisible: false, uploading: false, changingId: '',
      fileList: [], selectedFile: null, form: { groupCode: '', securityLevel: '' }
    }
  },
  computed: {
    userLevelRank() { return (LEVELS.find(item => item.value === this.userSecretLevel) || LEVELS[0]).rank },
    availableLevels() {
      return LEVELS.filter(item => item.rank <= this.userLevelRank)
    },
    requiresKnowledgeGroup() { return this.form.securityLevel === 'SECRET' || this.form.securityLevel === 'CONFIDENTIAL' },
    uploadRuleText() {
      if (this.form.securityLevel === 'PUBLIC') return '公开非密文档无需知悉用户组，登录用户均可访问。'
      if (this.form.securityLevel === 'INTERNAL') return '内部工作秘密仅按人员密级校验，无需知悉用户组。'
      return '国家秘密须同时满足人员密级和本人所属的唯一知悉用户组。'
    }
  },
  created() {
    this.loadFiles()
    this.loadGroups()
  },
  beforeDestroy() {
    this.fileRequestId++
    this.groupRequestId++
  },
  methods: {
    async loadGroups() {
      const requestId = ++this.groupRequestId
      this.groupsLoading = true
      this.groupsError = false
      try {
        const response = await listMyUploadGroups()
        if (requestId !== this.groupRequestId) return
        const data = response.data || {}
        this.groups = data.groups || []
        this.userSecretLevel = data.userSecretLevel || 'PUBLIC'
      } catch (error) {
        if (requestId !== this.groupRequestId) return
        this.groups = []
        this.userSecretLevel = 'PUBLIC'
        this.groupsError = true
      } finally {
        if (requestId === this.groupRequestId) this.groupsLoading = false
      }
    },
    async loadFiles() {
      const requestId = ++this.fileRequestId
      this.loading = true
      this.loadError = ''
      try {
        const response = await listMyKnowledge({ pageNum: this.pageNum, pageSize: this.pageSize })
        if (requestId !== this.fileRequestId) return
        this.files = response.data.rows || []
        this.total = response.data.total || 0
      } catch (error) {
        if (requestId !== this.fileRequestId) return
        this.files = []
        this.total = 0
        this.loadError = '暂时无法读取您的知识库。'
      } finally {
        if (requestId === this.fileRequestId) this.loading = false
      }
    },
    changePage(page) { this.pageNum = page; this.loadFiles() },
    openUpload() {
      if (this.uploading) return
      this.selectedFile = null
      this.fileList = []
      this.form.groupCode = ''
      this.form.securityLevel = this.userLevelRank >= 2 ? 'INTERNAL' : 'PUBLIC'
      this.uploadVisible = true
      this.$nextTick(() => {
        if (this.$refs.upload) this.$refs.upload.clearFiles()
      })
    },
    closeUpload(done) {
      if (this.uploading) return
      if (typeof done === 'function') done()
      else this.uploadVisible = false
    },
    onLevelChange() { if (!this.uploading) this.form.groupCode = '' },
    selectFile(file, fileList) {
      if (this.uploading) return
      this.selectedFile = file.raw
      this.fileList = fileList.slice(-1)
    },
    removeFile() { if (this.uploading) return; this.selectedFile = null; this.fileList = [] },
    async submitUpload() {
      if (this.uploading || !this.selectedFile || !this.form.securityLevel || (this.requiresKnowledgeGroup && !this.form.groupCode)) return
      if (this.selectedFile.size > 10 * 1024 * 1024) {
        this.$message.warning('单个文件不能超过 10 MB')
        return
      }
      const data = new FormData()
      data.append('file', this.selectedFile)
      data.append('groupCode', this.requiresKnowledgeGroup ? this.form.groupCode : '')
      data.append('securityLevel', this.form.securityLevel)
      this.uploading = true
      try {
        const response = await uploadMyKnowledge(data)
        const uploadResult = response.data
        const chunkCount = uploadResult && Number(uploadResult.chunkCount)
        if (uploadResult && !Number.isNaN(chunkCount) && chunkCount === 0) {
          this.$message.warning('文件已保存，但未生成向量切块，请检查文件内容')
        } else {
          this.$message.success('文档已上传并写入知识库')
        }
        this.selectedFile = null
        this.fileList = []
        if (this.$refs.upload) this.$refs.upload.clearFiles()
        this.uploadVisible = false
        this.pageNum = 1
        await this.loadFiles()
      } catch (error) {
        // request 拦截器已展示服务端的具体错误。
      } finally {
        this.uploading = false
      }
    },
    async toggleStatus(file) {
      if (this.changingId || !file.fileId || file.metadataStatus === 'UNAVAILABLE') return
      const enabled = file.metadataStatus !== 'ACTIVE'
      this.changingId = file.fileId
      try {
        try {
          await this.$confirm(enabled ? '启用后，符合权限策略的用户可检索此文档。' : '停用后，此文档将不再返回给检索用户。', enabled ? '启用文档' : '停用文档', {
            confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning'
          })
        } catch (error) { return }
        await changeMyKnowledgeStatus(file.fileId, enabled)
        this.$message.success(enabled ? '文档已启用' : '文档已停用')
        await this.loadFiles()
      } catch (error) {
        // request 拦截器已展示服务端的具体错误。
      } finally {
        this.changingId = ''
      }
    },
    isVectorReady(file) { return file.embeddingStatus === 'SUCCESS' && Number(file.chunkCount) > 0 },
    vectorStatus(file) {
      if (this.isVectorReady(file)) return '已向量化'
      if (file.chunkCount != null && Number(file.chunkCount) === 0) return '无向量切块'
      return '待确认'
    },
    accessDescription(file) {
      if (file.securityLevel === 'PUBLIC') return '公开 · 登录用户可访问'
      if (file.securityLevel === 'INTERNAL') return '内部 · 按人员密级'
      return file.groupName || file.scopeCode || '知悉用户组未配置'
    },
    levelName(value) { return (LEVELS.find(item => item.value === value) || {}).label || value || '未标记' },
    levelTag(value) { return value === 'PUBLIC' ? 'success' : value === 'INTERNAL' ? 'info' : 'warning' }
  }
}
</script>

<style scoped>
.library-page { height: 100%; overflow-y: auto; padding: 36px 48px 48px; box-sizing: border-box; }
.library-heading { display: flex; align-items: center; justify-content: space-between; gap: 24px; }
.eyebrow { color: #6f9bc9; font-size: 11px; font-weight: 700; letter-spacing: 1.8px; }
.library-heading h1 { margin: 10px 0 9px; color: #213b5c; font-size: 30px; }
.library-heading p { margin: 0; color: #8396aa; font-size: 13px; line-height: 1.6; }
.library-summary { display: flex; align-items: center; gap: 17px; margin: 30px 0 18px; padding: 22px 26px; border: 1px solid #e5edf6; border-radius: 13px; background: linear-gradient(100deg, #fff, #f5faff); }
.summary-icon { display: flex; align-items: center; justify-content: center; width: 52px; height: 52px; border-radius: 13px; background: #eaf3ff; color: #4385d5; font-size: 25px; }
.library-summary span { display: block; color: #7488a0; font-size: 12px; }
.library-summary strong { display: block; margin-top: 2px; color: #24456b; font-size: 27px; }
.summary-caption { margin-left: auto; color: #9aacc0; font-size: 12px; }
.library-panel { overflow: hidden; border: 1px solid #e4ebf3; border-radius: 13px; background: #fff; box-shadow: 0 5px 18px rgba(35, 67, 105, .04); }
.panel-heading { display: flex; align-items: center; justify-content: space-between; gap: 18px; padding: 22px 26px; border-bottom: 1px solid #edf1f6; }
.panel-heading h2 { margin: 0; color: #294564; font-size: 17px; }.panel-heading p { margin: 6px 0 0; color: #9aabba; font-size: 12px; }
.table-scroll { overflow-x: auto; }.file-table { min-width: 780px; }.file-name { display: flex; align-items: center; gap: 9px; color: #294768; font-weight: 600; }.file-name i { color: #5e99da; font-size: 18px; }.file-name span { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }.file-meta { margin-top: 5px; color: #a0afbf; font-size: 11px; }
.index-status { display: inline-flex; align-items: center; gap: 6px; color: #8795a6; font-size: 12px; }.index-status::before { content: ''; width: 6px; height: 6px; border-radius: 50%; background: #aeb9c5; }.index-status.ready { color: #2b9f79; }.index-status.ready::before { background: #36b68b; }.index-status.pending { color: #b98735; }.index-status.pending::before { background: #e4a846; }
.pagination-wrap { display: flex; justify-content: flex-end; padding: 16px 23px; }.state-message { min-height: 260px; display: flex; align-items: center; justify-content: center; flex-direction: column; gap: 12px; color: #8fa1b4; font-size: 13px; }.state-message i { color: #acc5de; font-size: 34px; }.state-message strong { color: #47647f; font-size: 15px; }.error-state i { color: #dd9d61; }.group-tip { margin: 18px 2px; color: #a57e48; font-size: 12px; }
.upload-intro { margin-bottom: 16px; padding: 12px 14px; border-radius: 7px; background: #f1f6fc; color: #64819d; font-size: 12px; line-height: 1.6; }.form-control { width: 100%; }.upload-tip { color: #96a7b9; font-size: 11px; line-height: 1.5; }.scope-note { padding: 12px; border-radius: 6px; background: #f7f9fc; color: #8498ac; font-size: 12px; }
@media (max-width: 1100px) { .library-page { padding: 25px; } .library-heading { align-items: flex-start; flex-direction: column; } }
</style>
