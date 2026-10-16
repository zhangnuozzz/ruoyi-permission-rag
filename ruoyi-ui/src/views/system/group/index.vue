<template>
  <div class="app-container group-page identity-access-page">
    <header class="identity-header">
      <div>
        <h1>用户组管理</h1>
        <p>一个人可加入多个用户组；秘密、机密文档各绑定一个知悉组。</p>
      </div>
      <span class="identity-count">当前筛选：{{ total }} 组</span>
    </header>

    <div class="identity-rule-note" role="note">
      <strong>秘密、机密文档：</strong>
      人员密级足够，且文档唯一绑定组属于人员所属组集合。用户组不提高人员密级。
    </div>

    <el-form
      v-show="showSearch"
      ref="queryForm"
      class="identity-filter"
      :model="queryParams"
      size="small"
      :inline="true"
    >
      <el-form-item label="用户组名称" prop="groupName">
        <el-input
          v-model="queryParams.groupName"
          placeholder="请输入用户组名称"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>

      <el-form-item label="用户组编码" prop="groupCode">
        <el-input
          v-model="queryParams.groupCode"
          placeholder="例如 GROUP_PROJECT_A"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>

      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="全部" clearable>
          <el-option label="正常" value="0" />
          <el-option label="停用" value="1" />
        </el-select>
      </el-form-item>

      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">
          搜索
        </el-button>

        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8 identity-toolbar">
      <el-col :span="1.5">
        <el-button
          v-hasPermi="['system:group:add']"
          type="primary"
          plain
          icon="el-icon-plus"
          size="mini"
          @click="handleAdd"
        >
          新建用户组
        </el-button>
      </el-col>

      <right-toolbar :show-search.sync="showSearch" @queryTable="getList" />
    </el-row>

    <div v-if="listError || deleteError" class="identity-feedback" role="alert">
      <i class="el-icon-warning-outline" aria-hidden="true" />
      <span>{{ listError || deleteError }}</span>
      <el-button v-if="listError" type="text" :disabled="loading" @click="getList">重新加载</el-button>
      <el-button v-else type="text" @click="deleteError = ''">知道了</el-button>
    </div>

    <el-table
      v-loading="loading"
      :data="groupList"
      size="small"
      :empty-text="listError ? '用户组数据未能加载' : '未找到符合条件的用户组'"
    >
      <el-table-column label="用户组名称" prop="groupName" min-width="160" />

      <el-table-column label="用户组编码" prop="groupCode" min-width="180" />

      <el-table-column label="分组用途" width="150">
        <template slot-scope="scope">
          <el-tag
            size="mini"
            :type="scope.row.groupCode === 'GROUP_PUBLIC' ? 'info' : ''"
            effect="plain"
          >
            {{ scope.row.groupCode === 'GROUP_PUBLIC' ? '公开兼容分组' : '涉密知悉范围' }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column label="成员数" width="90" align="center">
        <template slot-scope="scope">
          <el-tag type="info" size="mini">
            {{ memberCount(scope.row.id) }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column label="状态" width="90" align="center">
        <template slot-scope="scope">
          <el-tag size="mini" :type="scope.row.status === '0' ? 'success' : 'info'">
            {{ scope.row.status === '0' ? '正常' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column label="说明" prop="remark" min-width="180" show-overflow-tooltip />

      <el-table-column
        label="操作"
        width="230"
        class-name="identity-actions"
        fixed="right"
        align="center"
      >
        <template slot-scope="scope">
          <el-button
            v-if="can('system:group:list')"
            type="text"
            size="mini"
            icon="el-icon-user"
            @click="openMembers(scope.row)"
          >
            成员管理
          </el-button>

          <el-button
            v-if="can('system:group:edit') && can('system:group:query')"
            type="text"
            size="mini"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
          >
            编辑
          </el-button>

          <el-button
            v-if="can('system:group:remove')"
            type="text"
            size="mini"
            icon="el-icon-delete"
            class="danger-text"
            :disabled="deletingId !== null || scope.row.groupCode === 'GROUP_PUBLIC'"
            :loading="deletingId === scope.row.id"
            :title="scope.row.groupCode === 'GROUP_PUBLIC' ? '公开兼容分组保留历史归属，不能删除' : '仅可删除无引用的用户组'"
            @click="handleDelete(scope.row)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 新增编辑 -->
    <el-dialog
      :title="title"
      :visible.sync="open"
      width="560px"
      :before-close="closeGroupDialog"
      :close-on-press-escape="!saving"
      append-to-body
      custom-class="identity-access-dialog"
      :close-on-click-modal="false"
    >
      <el-alert
        :title="
          form.groupCode === 'GROUP_PUBLIC'
            ? '此分组仅用于旧版公开数据兼容，不参与涉密知悉授权。'
            : '用户组表示知悉范围，不设置密级，也不授予后台操作权限。'
        "
        type="info"
        :closable="false"
        show-icon
        class="dialog-alert"
      />

      <div v-if="saveError" class="identity-form-feedback" role="alert">{{ saveError }}</div>
      <el-form ref="form" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="用户组名称" prop="groupName">
          <el-input v-model.trim="form.groupName" :maxlength="128" placeholder="例如：A项目组" />
        </el-form-item>

        <el-form-item label="用户组编码" prop="groupCode">
          <el-input
            v-model.trim="form.groupCode"
            :disabled="!!form.id"
            :maxlength="64"
            placeholder="大写字母、数字或下划线，以字母开头"
          />
          <span v-if="form.id" class="identity-help">编码用于文档归属，创建后固定。</span>
        </el-form-item>

        <el-form-item label="业务含义">
          <el-input
            :value="form.groupCode === 'GROUP_PUBLIC' ? '公开兼容分组' : '涉密知悉范围'"
            disabled
          />
        </el-form-item>

        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio label="0">正常</el-radio>
            <el-radio label="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="说明">
          <el-input
            v-model="form.remark"
            type="textarea"
            :rows="3"
            :maxlength="500"
            placeholder="例如：A项目秘密、机密资料知悉人员范围"
          />
        </el-form-item>
      </el-form>

      <div slot="footer">
        <el-button type="primary" :loading="saving" @click="submitForm">保存</el-button>

        <el-button :disabled="saving" @click="cancel">取消</el-button>
      </div>
    </el-dialog>

    <!-- 成员管理 -->
    <el-dialog
      :title="memberDialogTitle"
      :visible.sync="memberOpen"
      width="760px"
      :before-close="closeMemberDialog"
      :close-on-press-escape="!memberBusy"
      append-to-body
      custom-class="identity-access-dialog"
      :close-on-click-modal="false"
    >
      <el-alert
        :title="
          currentGroup && currentGroup.groupCode === 'GROUP_PUBLIC'
            ? '此处维护旧版兼容成员关系；公开文档访问不以加入该组为前提。'
            : '加入该组只满足对应知悉范围，访问秘密、机密文档仍须人员密级足够。'
        "
        type="warning"
        :closable="false"
        show-icon
        class="dialog-alert"
      />

      <div v-if="currentGroup" class="member-summary">
        <div>
          <strong>
            {{ currentGroup.groupName }}
          </strong>

          <span class="summary-code">
            {{ currentGroup.groupCode }}
          </span>
        </div>

        <div>
          当前成员：
          <strong>
            {{ memberLoading || memberError ? '待确认' : memberList.length }}
          </strong>
        </div>
      </div>

      <el-divider content-position="left">添加成员</el-divider>

      <el-alert
        v-if="currentGroup && currentGroup.status !== '0'"
        title="此用户组已停用，不能新增成员；可查看或移出现有成员。"
        type="info"
        :closable="false"
        show-icon
        class="mb20"
      />

      <div v-if="memberError" class="identity-form-feedback" role="alert">
        {{ memberError }}
        <el-button type="text" :disabled="memberLoading" @click="loadMembers">重试</el-button>
      </div>
      <div v-if="can('system:group:edit')" class="add-member-row">
        <el-select
          v-model="candidateUserId"
          :disabled="!canAddMembers || memberLoading || memberBusy || !!memberError"
          filterable
          clearable
          placeholder="选择需要加入该知悉范围的人员"
          style="width: 470px"
        >
          <el-option
            v-for="user in candidateList"
            :key="user.userId"
            :label="userLabel(user)"
            :value="user.userId"
          />
        </el-select>

        <el-button
          type="primary"
          icon="el-icon-plus"
          :disabled="!canAddMembers || !candidateUserId || memberLoading || !!memberError || memberBusy"
          :loading="memberSaving"
          @click="addMember"
        >
          加入用户组
        </el-button>
      </div>

      <el-divider content-position="left">当前成员</el-divider>

      <el-table v-loading="memberLoading" :data="memberList" border>
        <el-table-column label="用户名" prop="userName" min-width="130" />

        <el-table-column label="姓名" prop="nickName" min-width="130" />

        <el-table-column label="账号状态" width="100" align="center">
          <template slot-scope="scope">
            <el-tag size="mini" :type="scope.row.status === '0' ? 'success' : 'info'">
              {{ scope.row.status === '0' ? '正常' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="加入时间" width="170">
          <template slot-scope="scope">
            {{ formatJoinTime(scope.row.joinTime) }}
          </template>
        </el-table-column>

        <el-table-column label="操作" width="100" align="center">
          <template slot-scope="scope">
            <el-button
              v-if="can('system:group:edit')"
              type="text"
              class="danger-text"
              :disabled="memberBusy || memberLoading || !!memberError"
              :loading="memberRemovingId === scope.row.userId"
              @click="removeMember(scope.row)"
            >
              移出
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div slot="footer">
        <el-button :disabled="memberBusy" @click="memberOpen = false">关闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import '@/assets/styles/identity-access.scss'
import { canUse } from '@/utils/identityAccess'
import {
  listGroup,
  getGroup,
  delGroup,
  addGroup,
  updateGroup,
  getGroupMemberCounts,
  listGroupMembers,
  listGroupCandidates,
  addGroupMember,
  removeGroupMember
} from '@/api/system/group'

export default {
  name: 'Group',

  data() {
    return {
      loading: true,
      saving: false,
      listError: '',
      deleteError: '',
      deletingId: null,
      saveError: '',
      countsLoaded: false,
      countsError: '',
      memberError: '',
      memberSaving: false,
      memberRemovingId: null,
      showSearch: true,

      total: 0,
      groupList: [],

      open: false,
      title: '',
      form: {},

      memberCountMap: {},

      memberOpen: false,
      memberLoading: false,

      currentGroup: null,

      memberList: [],
      candidateList: [],
      candidateUserId: null,

      queryParams: {
        pageNum: 1,
        pageSize: 10,

        groupCode: null,
        groupName: null,

        status: null
      },

      rules: {
        groupName: [
          {
            required: true,
            message: '用户组名称不能为空',
            trigger: 'blur'
          }
        ],

        groupCode: [
          {
            required: true,
            message: '用户组编码不能为空',
            trigger: 'blur'
          }
        ]
      }
    }
  },

  computed: {
    canAddMembers() {
      return !!this.currentGroup && this.currentGroup.status === '0' && this.can('system:group:edit')
    },
    memberBusy() {
      return this.memberSaving || this.memberRemovingId !== null
    },
    memberDialogTitle() {
      if (!this.currentGroup) {
        return '用户组成员管理'
      }

      return this.currentGroup.groupName + ' - 知悉人员管理'
    }
  },

  created() {
    this.getList()
  },

  methods: {
    can(permission) {
      return canUse(this.$store.getters.permissions, permission)
    },
    closeGroupDialog(done) {
      if (!this.saving) {
        done()
      }
    },
    closeMemberDialog(done) {
      if (!this.memberBusy) {
        done()
      }
    },

    async getList() {
      this.loading = true
      this.listError = ''
      this.deleteError = ''
      const requestId = (this.groupListRequestId || 0) + 1
      this.groupListRequestId = requestId
      try {
        const response = await listGroup(this.queryParams)
        if (requestId !== this.groupListRequestId) {
          return
        }
        this.groupList = Array.isArray(response.rows) ? response.rows : []
        this.total = Number(response.total) || 0
        this.loadMemberCounts()
      } catch (error) {
        if (requestId !== this.groupListRequestId) {
          return
        }
        this.groupList = []
        this.total = 0
        this.listError = '用户组列表加载失败，请重试。'
      } finally {
        if (requestId === this.groupListRequestId) {
          this.loading = false
        }
      }
    },

    async loadMemberCounts() {
      this.countsLoaded = false
      this.countsError = ''
      try {
        const response = await getGroupMemberCounts()
        const map = {}
        ;(response.data || []).forEach((item) => {
          map[String(item.groupId)] = Number(item.memberCount) || 0
        })
        this.memberCountMap = map
        this.countsLoaded = true
      } catch (error) {
        this.countsError = '成员数加载失败'
      }
    },

    memberCount(groupId) {
      if (!this.countsLoaded) {
        return this.countsError ? '未取得' : '加载中'
      }
      return this.memberCountMap[String(groupId)] || 0
    },

    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },

    resetQuery() {
      this.resetForm('queryForm')
      this.handleQuery()
    },

    reset() {
      this.saveError = ''
      this.form = {
        id: null,

        groupCode: null,
        groupName: null,

        // 旧兼容字段不再参与业务
        scopeCode: null,
        groupSecretLevel: null,

        status: '0',
        remark: null
      }

      if (this.$refs.form) {
        this.resetForm('form')
      }
    },

    handleAdd() {
      this.reset()

      this.title = '新建知悉用户组'

      this.open = true
    },

    handleUpdate(row) {
      this.reset()

      getGroup(row.id)
        .then((response) => {
          this.form = response.data || {}

          // 不再编辑旧 scope / groupSecretLevel
          this.form.scopeCode = null
          this.form.groupSecretLevel = null

          this.title = '编辑知悉用户组'

          this.open = true
        })
        .catch(() => {
          this.$message.error('用户组详情加载失败，请重试。')
        })
    },

    submitForm() {
      if (this.saving) {
        return
      }
      const permission = this.form.id ? 'system:group:edit' : 'system:group:add'
      if (!this.can(permission)) {
        return
      }
      this.$refs.form.validate(async(valid) => {
        if (!valid || this.saving) {
          return
        }
        this.saving = true
        this.saveError = ''
        const payload = {
          id: this.form.id,
          groupCode: this.form.groupCode,
          groupName: this.form.groupName,
          scopeCode: null,
          groupSecretLevel: null,
          status: this.form.status,
          remark: this.form.remark
        }
        try {
          await (payload.id ? updateGroup(payload) : addGroup(payload))
          this.msgSuccess('用户组已保存')
          this.open = false
          this.getList()
        } catch (error) {
          this.saveError = '用户组保存失败，请核对后重试。'
        } finally {
          this.saving = false
        }
      })
    },

    cancel() {
      if (this.saving) {
        return
      }
      this.open = false
      this.reset()
    },

    async handleDelete(row) {
      if (this.deletingId !== null || !this.can('system:group:remove') || row.groupCode === 'GROUP_PUBLIC') {
        return
      }
      this.deletingId = row.id
      this.deleteError = ''
      try {
        await this.$confirm('确认删除用户组“' + row.groupName + '”吗？有成员、文档或策略引用时将拒绝删除。', '警告', {
          type: 'warning'
        })
        await delGroup(row.id)
        this.msgSuccess('删除成功')
        await this.getList()
      } catch (error) {
        if (error !== 'cancel' && error !== 'close') {
          this.deleteError = error && error.message
            ? error.message
            : '删除未完成，请刷新核对；仍有引用的用户组须先解除关联。'
        }
      } finally {
        this.deletingId = null
      }
    },

    openMembers(row) {
      this.currentGroup = row
      this.memberList = []
      this.candidateList = []
      this.memberError = ''
      this.memberOpen = true
      this.candidateUserId = null
      this.loadMembers()
    },

    async loadMembers() {
      if (!this.currentGroup) {
        return
      }
      const groupId = this.currentGroup.id
      this.memberLoading = true
      this.memberError = ''
      try {
        const results = await Promise.all([
          listGroupMembers(groupId),
          this.canAddMembers
            ? listGroupCandidates(groupId)
            : Promise.resolve({ data: [] })
        ])
        if (!this.currentGroup || this.currentGroup.id !== groupId) {
          return
        }
        this.memberList = Array.isArray(results[0].data) ? results[0].data : []
        this.candidateList = Array.isArray(results[1].data) ? results[1].data : []
        this.loadMemberCounts()
      } catch (error) {
        if (!this.currentGroup || this.currentGroup.id !== groupId) {
          return
        }
        this.memberList = []
        this.candidateList = []
        this.memberError = '成员资料加载失败，暂不能变更成员关系。'
      } finally {
        if (this.currentGroup && this.currentGroup.id === groupId) {
          this.memberLoading = false
        }
      }
    },

    async addMember() {
      if (
        !this.currentGroup ||
        !this.candidateUserId ||
        this.memberBusy ||
        this.memberLoading ||
        this.memberError ||
        !this.canAddMembers
      ) {
        return
      }
      const groupId = this.currentGroup.id
      const userId = this.candidateUserId
      this.memberSaving = true
      try {
        await addGroupMember({ groupId, userId })
        this.msgSuccess('成员已加入用户组；人员密级未改变')
        this.candidateUserId = null
        await this.loadMembers()
      } catch (error) {
        this.$message.error('成员添加失败，请核对后重试。')
      } finally {
        this.memberSaving = false
      }
    },

    async removeMember(user) {
      if (
        !this.currentGroup ||
        this.memberBusy ||
        this.memberLoading ||
        this.memberError ||
        !this.can('system:group:edit')
      ) {
        return
      }
      const groupId = this.currentGroup.id
      this.memberRemovingId = user.userId
      try {
        await this.$confirm(
          '确认将“' +
            user.userName +
            '”移出“' +
            this.currentGroup.groupName +
            '”吗？该成员将失去此组对应的知悉范围。',
          '移出用户组'
        )
        await removeGroupMember(groupId, user.userId)
        this.msgSuccess('成员关系已移除')
        await this.loadMembers()
      } catch (error) {
        if (error !== 'cancel' && error !== 'close') {
          this.$message.error('成员移除失败，请刷新后核实。')
        }
      } finally {
        this.memberRemovingId = null
      }
    },

    formatJoinTime(value) {
      if (value === null || value === undefined || String(value).trim() === '') {
        return '未记录'
      }
      let source = typeof value === 'string' ? value.trim() : value
      if (typeof source === 'string' && /^\d+$/.test(source)) {
        source = Number(source)
      }
      if (typeof source === 'number' && String(source).length === 10) {
        source *= 1000
      }
      // ISO 日期保留时区信息；无时区的数据库日期沿用本地时间解释。
      if (typeof source === 'string' && /^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}(\.\d+)?$/.test(source)) {
        source = source.replace(/-/g, '/')
      }
      if (typeof source !== 'string' && typeof source !== 'number' && !(source instanceof Date)) {
        return '时间格式异常'
      }
      const date = source instanceof Date ? source : new Date(source)
      return Number.isNaN(date.getTime())
        ? '时间格式异常'
        : this.parseTime(date, '{y}-{m}-{d} {h}:{i}:{s}')
    },

    userLabel(user) {
      if (user.nickName && user.nickName !== user.userName) {
        return user.nickName + '（' + user.userName + '）'
      }

      return user.userName
    }
  }
}
</script>

<style scoped>
.group-page {
  padding: 20px;
}

.mb20 {
  margin-bottom: 20px;
}

.dialog-alert {
  margin-bottom: 18px;
}

.member-summary {
  display: flex;
  justify-content: space-between;
  align-items: center;

  margin-bottom: 10px;
  padding: 14px 16px;

  border-radius: 4px;

  background: #f5f7fa;
}

.summary-code {
  margin-left: 12px;
  color: #909399;
}

.add-member-row {
  display: flex;
  gap: 12px;
  align-items: center;
}

.danger-text {
  color: #f56c6c !important;
}
</style>
