<template>
  <div class="app-container">
    <div class="rag-doc-header">
      <div>
        <div class="rag-doc-title">文档权限标签</div>
        <div class="rag-doc-desc">维护文档密级、唯一知悉用户组和访问状态。原文件通过“RAG 文件入库”登记。</div>
        <div class="rag-doc-desc">公开不设密级与组门槛；内部仅要求人员密级；秘密、机密还须匹配知悉组。实际访问仍需通过状态、时间和策略校验。</div>
      </div>
      <span class="rag-doc-count">{{ loading || listError ? '数量待确认' : '共 ' + total + ' 条' }}</span>
    </div>
    <el-form v-show="showSearch" ref="queryForm" :model="queryParams" size="small" :inline="true" label-width="84px">
      <el-form-item label="文档ID" prop="docId">
        <el-input
          v-model="queryParams.docId"
          placeholder="请输入文档ID"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="文档名称" prop="docName">
        <el-input
          v-model="queryParams.docName"
          placeholder="请输入文档名称"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="文档密级" prop="securityLevel">
        <el-select v-model="queryParams.securityLevel" placeholder="全部" clearable>
          <el-option label="公开" value="PUBLIC" />
          <el-option label="内部" value="INTERNAL" />
          <el-option label="秘密" value="SECRET" />
          <el-option label="机密" value="CONFIDENTIAL" />
        </el-select>
      </el-form-item>
      <el-form-item label="知悉组编码" prop="ownerGroupCode">
        <el-input
          v-model="queryParams.ownerGroupCode"
          placeholder="请输入知悉组编码"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="文档状态" prop="metadataStatus">
        <el-select v-model="queryParams.metadataStatus" placeholder="全部" clearable>
          <el-option label="启用" value="ACTIVE" />
          <el-option label="禁用" value="DISABLED" />
          <el-option label="归档" value="ARCHIVED" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          v-hasPermi="['system:ragDoc:add']"
          type="primary"
          plain
          icon="el-icon-plus"
          size="mini"
          @click="handleAdd"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          v-hasPermi="['system:ragDoc:edit']"
          type="success"
          plain
          icon="el-icon-edit"
          size="mini"
          :disabled="single || loading || detailLoading || deleting"
          @click="handleUpdate"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          v-hasPermi="['system:ragDoc:remove']"
          type="danger"
          plain
          icon="el-icon-delete"
          size="mini"
          :disabled="multiple || loading || deleting"
          :loading="deleting"
          @click="handleDelete"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          v-hasPermi="['system:ragDoc:export']"
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
        >导出</el-button>
      </el-col>
      <right-toolbar :show-search.sync="showSearch" @queryTable="getList" />
    </el-row>

    <div v-if="listError || actionError" class="rag-doc-feedback" role="alert">
      <span>{{ listError || actionError }}</span>
      <el-button v-if="listError" type="text" :disabled="loading" @click="getList">重新加载</el-button>
      <el-button v-else type="text" @click="actionError = ''">知道了</el-button>
    </div>

    <el-table
      v-loading="loading"
      :data="ragDocList"
      :empty-text="listError ? '标签列表未能加载' : '未找到符合条件的文档标签'"
      border
      stripe
      class="rag-doc-table"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="50" align="center" fixed="left" />

      <el-table-column label="文档ID" align="center" prop="docId" width="230" show-overflow-tooltip>
        <template slot-scope="scope">
          <span class="mono-text">{{ scope.row.docId }}</span>
        </template>
      </el-table-column>

      <el-table-column label="文档名称" align="center" prop="docName" min-width="180" show-overflow-tooltip />
      <el-table-column label="上传人" align="center" prop="uploadUserName" width="110" show-overflow-tooltip />

      <el-table-column label="文档密级" align="center" prop="securityLevel" width="120">
        <template slot-scope="scope">
          <el-tag
            size="mini"
            :type="levelType(scope.row.securityLevel)"
          >
            {{ levelText(scope.row.securityLevel) }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column label="知悉用户组" align="center" min-width="190" show-overflow-tooltip>
        <template slot-scope="scope">
          <span v-if="groupExempt(scope.row.securityLevel)" class="sub-text">不要求知悉组</span>
          <template v-else>
            <div>{{ scope.row.ownerGroupName || '待核对归属' }}</div>
            <div class="sub-text mono-text">{{ scope.row.ownerGroupCode || '未配置' }}</div>
          </template>
        </template>
      </el-table-column>

      <el-table-column label="文档状态" align="center" prop="metadataStatus" width="100">
        <template slot-scope="scope">
          <el-tag size="mini" :type="stateType(scope.row)">{{ stateText(scope.row) }}</el-tag>
        </template>
      </el-table-column>

      <el-table-column label="备注 / 来源说明" align="left" prop="remark" min-width="220" show-overflow-tooltip />

      <el-table-column label="创建时间" align="center" prop="createTime" width="120">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.createTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>

      <el-table-column label="更新时间" align="center" prop="updateTime" width="120">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.updateTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>

      <el-table-column label="操作" align="center" width="140" fixed="right" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            v-hasPermi="['system:ragDoc:edit']"
            size="mini"
            type="text"
            icon="el-icon-edit"
            :disabled="loading || detailLoading || deleting"
            @click="handleUpdate(scope.row)"
          >修改</el-button>
          <el-button
            v-hasPermi="['system:ragDoc:remove']"
            size="mini"
            type="text"
            icon="el-icon-delete"
            :disabled="loading || deleting"
            @click="handleDelete(scope.row)"
          >删除</el-button>
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

    <el-dialog
      :title="title"
      :visible.sync="open"
      width="620px"
      :before-close="closeDialog"
      :close-on-click-modal="false"
      :close-on-press-escape="!saving"
      append-to-body
    >
      <div v-if="saveError" class="rag-doc-feedback" role="alert">{{ saveError }}</div>
      <el-form ref="form" :model="form" :rules="rules" :disabled="saving" label-width="100px">
        <el-form-item label="文档ID" prop="docId">
          <el-input v-model.trim="form.docId" :disabled="!!form.id" :maxlength="64" placeholder="请输入唯一文档ID" />
        </el-form-item>
        <el-form-item label="文档名称" prop="docName">
          <el-input v-model.trim="form.docName" :maxlength="255" placeholder="请输入文档名称" />
        </el-form-item>
        <el-form-item label="上传人">
          <span>{{ form.uploadUserName || '未记录' }}</span>
          <span v-if="!form.id" class="sub-text">（仅新增标签，不上传文件）</span>
        </el-form-item>
        <el-form-item label="文档密级" prop="securityLevel">
          <el-select v-model="form.securityLevel" style="width: 100%" @change="handleLevelChange">
            <el-option v-if="form.securityLevel && !knownLevel(form.securityLevel)" :label="levelText(form.securityLevel)" :value="form.securityLevel" disabled />
            <el-option label="公开" value="PUBLIC" />
            <el-option label="内部" value="INTERNAL" />
            <el-option label="秘密" value="SECRET" />
            <el-option label="机密" value="CONFIDENTIAL" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="requiresGroup(form.securityLevel)" label="知悉用户组" prop="ownerGroupCode">
          <el-select
            v-model="form.ownerGroupCode"
            filterable
            clearable
            :loading="groupLoading"
            :disabled="groupLoading || !!groupError"
            placeholder="选择一个有效知悉用户组"
            style="width: 100%"
          >
            <el-option
              v-if="unavailableCurrentGroup"
              :label="(form.ownerGroupName || form.ownerGroupCode) + '（当前归属待核对）'"
              :value="form.ownerGroupCode"
              disabled
            />
            <el-option v-for="group in groupOptions" :key="group.id" :label="group.groupName + '（' + group.groupCode + '）'" :value="group.groupCode" />
          </el-select>
          <div v-if="groupError" class="form-help error-text">
            {{ groupError }}
            <el-button type="text" :disabled="groupLoading" @click="loadGroupOptions">重新加载</el-button>
          </div>
        </el-form-item>
        <div class="form-help rule-help">{{ documentRule(form.securityLevel) }}</div>
        <el-form-item label="文档状态" prop="metadataStatus">
          <el-select v-model="form.metadataStatus" style="width: 100%">
            <el-option v-if="form.metadataStatus && !knownState(form.metadataStatus)" :label="'待核对（' + form.metadataStatus + '）'" :value="form.metadataStatus" disabled />
            <el-option label="启用" value="ACTIVE" />
            <el-option label="禁用" value="DISABLED" />
            <el-option label="归档" value="ARCHIVED" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :maxlength="500" :rows="3" placeholder="请输入说明" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" :loading="saving" :disabled="requiresGroup(form.securityLevel) && (groupLoading || !!groupError)" @click="submitForm">保存标签</el-button>
        <el-button :disabled="saving" @click="cancel">取消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listRagDoc, getRagDoc, delRagDoc, addRagDoc, updateRagDoc } from '@/api/system/ragDoc'
import { listGroup } from '@/api/system/group'
import { canUse } from '@/utils/identityAccess'

const LEVEL_NAMES = { PUBLIC: '公开', INTERNAL: '内部', SECRET: '秘密', CONFIDENTIAL: '机密' }
const DOCUMENT_STATES = ['ACTIVE', 'DISABLED', 'ARCHIVED']

export default {
  name: 'RagDoc',
  data() {
    return {
      loading: true,
      detailLoading: false,
      saving: false,
      deleting: false,
      listError: '',
      actionError: '',
      saveError: '',
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      ragDocList: [],
      title: '',
      open: false,
      form: {},
      groupOptions: [],
      groupLoading: false,
      groupError: '',
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        docId: null,
        docName: null,
        securityLevel: null,
        ownerGroupCode: null,
        metadataStatus: null
      },
      rules: {
        docId: [{ required: true, whitespace: true, max: 64, message: '文档ID不能为空且不能超过64个字符', trigger: 'blur' }],
        docName: [{ required: true, whitespace: true, max: 255, message: '文档名称不能为空且不能超过255个字符', trigger: 'blur' }],
        securityLevel: [{
          validator: (rule, value, callback) => {
            callback(this.knownLevel(value) ? undefined : new Error('请选择支持的文档密级；历史值须核对后处理'))
          },
          trigger: 'change'
        }],
        ownerGroupCode: [{
          validator: (rule, value, callback) => {
            const valid = !this.requiresGroup(this.form.securityLevel) || this.groupOptions.some(group => this.availableGroup(group) && group.groupCode === value)
            callback(valid ? undefined : new Error('请选择一个有效知悉用户组'))
          },
          trigger: 'change'
        }],
        metadataStatus: [{
          validator: (rule, value, callback) => {
            callback(DOCUMENT_STATES.includes(value) ? undefined : new Error('请选择文档访问状态'))
          },
          trigger: 'change'
        }]
      }
    }
  },
  computed: {
    unavailableCurrentGroup() {
      return this.requiresGroup(this.form.securityLevel) && !this.groupLoading && !!this.form.ownerGroupCode &&
        !this.groupOptions.some(group => group.groupCode === this.form.ownerGroupCode)
    }
  },
  created() {
    this.getList()
  },
  methods: {
    can(permission) {
      return canUse(this.$store.getters.permissions, permission)
    },
    knownLevel(level) {
      return Object.prototype.hasOwnProperty.call(LEVEL_NAMES, level)
    },
    knownState(state) {
      return DOCUMENT_STATES.includes(state)
    },
    availableGroup(group) {
      return !!group && group.status === '0' && (!group.delFlag || group.delFlag === '0') &&
        typeof group.groupCode === 'string' && !!group.groupCode && group.groupCode.trim().toUpperCase() !== 'GROUP_PUBLIC'
    },
    levelText(level) {
      return this.knownLevel(level) ? LEVEL_NAMES[level] : '待核对（' + (level || '未记录') + '）'
    },
    levelType(level) {
      return this.knownLevel(level) ? { PUBLIC: 'success', INTERNAL: 'info', SECRET: 'warning', CONFIDENTIAL: 'danger' }[level] : 'info'
    },
    effectiveState(document) {
      return document.metadataStatus || (document.status === '0' ? 'ACTIVE' : document.status === '1' ? 'DISABLED' : '')
    },
    stateText(document) {
      const state = this.effectiveState(document)
      return this.knownState(state) ? { ACTIVE: '启用', DISABLED: '禁用', ARCHIVED: '归档' }[state] : '待核对（' + (state || '未记录') + '）'
    },
    stateType(document) {
      const state = this.effectiveState(document)
      return this.knownState(state) ? { ACTIVE: 'success', DISABLED: 'danger', ARCHIVED: 'info' }[state] : 'info'
    },
    requiresGroup(level) {
      return level === 'SECRET' || level === 'CONFIDENTIAL'
    },
    groupExempt(level) {
      return level === 'PUBLIC' || level === 'INTERNAL'
    },
    documentRule(level) {
      if (level === 'PUBLIC') return '公开文档不设人员密级和知悉组门槛；仍须通过请求准入检查。'
      if (level === 'INTERNAL') return '内部文档只要求人员密级达到内部，不要求知悉用户组。'
      if (this.requiresGroup(level)) return '人员密级须达到文档密级，且属于文档绑定的唯一知悉组；用户组不具有密级。'
      return '历史密级尚未识别，请核对；页面不会自动转换为公开或内部。'
    },
    async getList() {
      const requestId = (this.listRequestId || 0) + 1
      this.listRequestId = requestId
      this.loading = true
      this.listError = ''
      this.actionError = ''
      this.ids = []
      this.single = true
      this.multiple = true
      try {
        const response = await listRagDoc(this.queryParams)
        if (requestId !== this.listRequestId) return
        if (!response || !Array.isArray(response.rows) || !Number.isFinite(Number(response.total))) {
          throw new Error('标签列表返回不完整')
        }
        this.ragDocList = response.rows
        this.total = Number(response.total)
      } catch (error) {
        if (requestId !== this.listRequestId) return
        this.ragDocList = []
        this.total = 0
        this.listError = '文档标签加载失败，请重新加载。'
      } finally {
        if (requestId === this.listRequestId) this.loading = false
      }
    },
    async loadGroupOptions() {
      const requestId = (this.groupRequestId || 0) + 1
      this.groupRequestId = requestId
      this.groupLoading = true
      this.groupError = ''
      this.groupOptions = []
      try {
        if (!this.can('system:group:list')) throw new Error('当前账号不能读取用户组选项，请联系管理员配置查询权限。')
        const response = await listGroup({ pageNum: 1, pageSize: 1000, status: '0' })
        if (requestId !== this.groupRequestId) return
        if (!response || !Array.isArray(response.rows) || !Number.isFinite(Number(response.total)) || Number(response.total) > response.rows.length) {
          throw new Error('知悉用户组选项未完整加载，请联系管理员核对。')
        }
        this.groupOptions = response.rows.filter(group => this.availableGroup(group))
      } catch (error) {
        if (requestId === this.groupRequestId) {
          this.groupError = error && error.message ? error.message : '知悉用户组加载失败，请重试。'
        }
      } finally {
        if (requestId === this.groupRequestId) this.groupLoading = false
      }
    },
    handleLevelChange(level) {
      this.saveError = ''
      if (this.requiresGroup(level)) {
        this.loadGroupOptions()
      } else {
        this.form.ownerGroupCode = ''
        this.form.ownerGroupName = ''
      }
      this.$nextTick(() => {
        if (this.$refs.form) this.$refs.form.clearValidate(['securityLevel', 'ownerGroupCode'])
      })
    },
    reset() {
      this.detailRequestId = (this.detailRequestId || 0) + 1
      this.groupRequestId = (this.groupRequestId || 0) + 1
      this.detailLoading = false
      this.groupLoading = false
      this.groupOptions = []
      this.groupError = ''
      this.saveError = ''
      this.form = {
        id: null, docId: '', docName: '', securityLevel: 'PUBLIC',
        ownerGroupCode: '', ownerGroupName: '', uploadUserName: '',
        metadataStatus: 'ACTIVE', remark: ''
      }
      this.$nextTick(() => {
        if (this.$refs.form) this.$refs.form.clearValidate()
      })
    },
    closeDialog(done) {
      if (!this.saving) {
        this.reset()
        done()
      }
    },
    cancel() {
      if (this.saving) return
      this.open = false
      this.reset()
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.resetForm('queryForm')
      this.handleQuery()
    },
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.id)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    handleAdd() {
      if (this.deleting || !this.can('system:ragDoc:add')) return
      this.reset()
      this.title = '新增文档标签'
      this.open = true
    },
    async handleUpdate(row) {
      if (this.detailLoading || this.deleting || !this.can('system:ragDoc:edit')) return
      const id = row && row.id ? row.id : this.ids[0]
      if (!id) return
      this.reset()
      const requestId = this.detailRequestId
      this.detailLoading = true
      this.actionError = ''
      try {
        const response = await getRagDoc(id)
        if (requestId !== this.detailRequestId) return
        const data = response && response.data
        if (!data || String(data.id) !== String(id)) throw new Error('文档标签不存在或返回不完整')
        this.form = {
          id: data.id, docId: data.docId, docName: data.docName,
          securityLevel: data.securityLevel,
          ownerGroupCode: data.ownerGroupCode || '', ownerGroupName: data.ownerGroupName || '',
          uploadUserName: data.uploadUserName || '',
          metadataStatus: this.effectiveState(data),
          remark: data.remark || ''
        }
        this.title = '修改文档标签'
        this.open = true
        if (this.requiresGroup(this.form.securityLevel)) this.loadGroupOptions()
      } catch (error) {
        if (requestId === this.detailRequestId) this.actionError = '文档标签详情加载失败，请刷新后重试。'
      } finally {
        if (requestId === this.detailRequestId) this.detailLoading = false
      }
    },
    buildPayload() {
      const level = this.form.securityLevel
      if (!this.knownLevel(level)) throw new Error('请选择支持的文档密级，历史值不会自动转换。')
      if (!DOCUMENT_STATES.includes(this.form.metadataStatus)) throw new Error('请选择有效的文档状态。')
      const group = this.groupOptions.find(item => this.availableGroup(item) && item.groupCode === this.form.ownerGroupCode)
      if (this.requiresGroup(level) && (this.groupLoading || this.groupError || !group)) {
        throw new Error('请先加载并选择一个有效知悉用户组。')
      }
      const code = level === 'PUBLIC' ? 'GROUP_PUBLIC' : level === 'INTERNAL' ? '' : group.groupCode
      return {
        id: this.form.id,
        docId: this.form.docId.trim(),
        docName: this.form.docName.trim(),
        securityLevel: level,
        ownerGroupCode: code,
        ownerGroupName: level === 'PUBLIC' ? '公开文档组' : level === 'INTERNAL' ? '' : group.groupName,
        scopeCode: this.requiresGroup(level) ? code : level,
        metadataStatus: this.form.metadataStatus,
        status: this.form.metadataStatus === 'ACTIVE' ? '0' : '1',
        remark: this.form.remark || ''
      }
    },
    submitForm() {
      if (this.saving || !this.can(this.form.id ? 'system:ragDoc:edit' : 'system:ragDoc:add')) return
      this.$refs.form.validate(async valid => {
        if (!valid || this.saving) return
        this.saving = true
        this.saveError = ''
        try {
          const payload = this.buildPayload()
          const response = await (payload.id ? updateRagDoc(payload) : addRagDoc(payload))
          if (!response || response.code !== 200) throw new Error('未取得保存成功响应，请刷新列表核对。')
          this.msgSuccess('文档标签已保存')
          this.open = false
          this.getList()
        } catch (error) {
          this.saveError = error && error.message ? error.message : '保存未完成，请刷新列表核对后重试。'
        } finally {
          this.saving = false
        }
      })
    },
    async handleDelete(row) {
      if (this.deleting || this.loading || !this.can('system:ragDoc:remove')) return
      const ids = row && row.id ? [row.id] : this.ids.slice()
      if (!ids.length) return
      this.deleting = true
      this.actionError = ''
      try {
        const label = row && row.id ? '“' + row.docName + '”' : '所选的 ' + ids.length + ' 个标签'
        await this.$modal.confirm('确认删除文档权限标签' + label + '吗？此操作不删除原文件。')
        const response = await delRagDoc(ids.join(','))
        if (!response || response.code !== 200) throw new Error('未取得删除成功响应，请刷新列表核对。')
        if (ids.length >= this.ragDocList.length && this.queryParams.pageNum > 1) this.queryParams.pageNum--
        this.msgSuccess('文档标签已删除')
        await this.getList()
      } catch (error) {
        if (error !== 'cancel' && error !== 'close') this.actionError = '删除未完成，请刷新列表核对后重试。'
      } finally {
        this.deleting = false
      }
    },
    handleExport() {
      this.download('system/ragDoc/export', { ...this.queryParams }, 'ragDoc_' + Date.now() + '.xlsx')
    }
  }
}
</script>

<style scoped>
.rag-doc-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 16px;
  margin-bottom: 16px;
  background: #f8fafc;
  border: 1px solid #ebeef5;
  border-radius: 4px;
}
.rag-doc-title { font-size: 16px; font-weight: 600; color: #303133; margin-bottom: 6px; }
.rag-doc-desc, .sub-text, .rag-doc-count { font-size: 13px; color: #909399; line-height: 1.6; }
.rag-doc-count { flex-shrink: 0; }
.rag-doc-table { width: 100%; }
.mono-text { font-family: Menlo, Monaco, Consolas, "Courier New", monospace; font-size: 12px; }
.rag-doc-feedback { display: flex; align-items: center; gap: 12px; padding: 10px 14px; margin-bottom: 14px; background: #fef0f0; color: #c45656; border-radius: 4px; }
.form-help { font-size: 13px; line-height: 1.6; color: #909399; }
.rule-help { margin: -2px 0 18px 100px; }
.error-text { color: #c45656; }
@media (max-width: 768px) {
  .rag-doc-header { align-items: flex-start; flex-direction: column; }
}
</style>
