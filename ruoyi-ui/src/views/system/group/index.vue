<template>
  <div class="app-container group-page">
    <div class="page-header">
      <div>
        <div class="page-title">用户组管理</div>
        <div class="page-desc">
          统一管理用户组、成员、知悉范围与用户组密级，成员关系不再单独维护。
        </div>
      </div>
    </div>

    <el-alert
      title="用户加入用户组后，将继承该用户组对应的基础访问范围。"
      type="info"
      :closable="false"
      show-icon
      class="mb20"
    />

    <el-form
      ref="queryForm"
      :model="queryParams"
      size="small"
      :inline="true"
      v-show="showSearch"
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
          placeholder="请输入用户组编码"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>

      <el-form-item label="知悉范围" prop="scopeCode">
        <el-input
          v-model="queryParams.scopeCode"
          placeholder="例如 PUBLIC"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>

      <el-form-item label="用户组密级" prop="groupSecretLevel">
        <el-select
          v-model="queryParams.groupSecretLevel"
          placeholder="全部"
          clearable
        >
          <el-option label="公开" value="PUBLIC" />
          <el-option label="内部" value="INTERNAL" />
          <el-option label="秘密" value="SECRET" />
          <el-option label="机密" value="CONFIDENTIAL" />
        </el-select>
      </el-form-item>

      <el-form-item>
        <el-button
          type="primary"
          icon="el-icon-search"
          size="mini"
          @click="handleQuery"
        >搜索</el-button>

        <el-button
          icon="el-icon-refresh"
          size="mini"
          @click="resetQuery"
        >重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="el-icon-plus"
          size="mini"
          @click="handleAdd"
          v-hasPermi="['system:group:add']"
        >新建用户组</el-button>
      </el-col>

      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['system:group:export']"
        >导出</el-button>
      </el-col>

      <right-toolbar
        :showSearch.sync="showSearch"
        @queryTable="getList"
      />
    </el-row>

    <el-table
      v-loading="loading"
      :data="groupList"
      border
      stripe
    >
      <el-table-column
        label="用户组名称"
        prop="groupName"
        min-width="150"
      />

      <el-table-column
        label="用户组编码"
        prop="groupCode"
        min-width="150"
      />

      <el-table-column
        label="知悉范围"
        prop="scopeCode"
        min-width="120"
        align="center"
      >
        <template slot-scope="scope">
          <el-tag size="mini" effect="plain">
            {{ scope.row.scopeCode || '未配置' }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column
        label="用户组密级"
        width="110"
        align="center"
      >
        <template slot-scope="scope">
          <el-tag
            size="mini"
            :type="secretTagType(scope.row.groupSecretLevel)"
          >
            {{ secretText(scope.row.groupSecretLevel) }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column
        label="成员数"
        width="90"
        align="center"
      >
        <template slot-scope="scope">
          <el-tag type="info" size="mini">
            {{ memberCount(scope.row.id) }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column
        label="状态"
        width="90"
        align="center"
      >
        <template slot-scope="scope">
          <el-tag
            size="mini"
            :type="scope.row.status === '0' ? 'success' : 'info'"
          >
            {{ scope.row.status === '0' ? '正常' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column
        label="备注"
        prop="remark"
        min-width="150"
        show-overflow-tooltip
      />

      <el-table-column
        label="操作"
        width="230"
        align="center"
        fixed="right"
      >
        <template slot-scope="scope">
          <el-button
            type="text"
            icon="el-icon-user"
            @click="openMembers(scope.row)"
            v-hasPermi="['system:group:list']"
          >成员管理</el-button>

          <el-button
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['system:group:edit']"
          >编辑</el-button>

          <el-button
            type="text"
            icon="el-icon-delete"
            class="danger-text"
            @click="handleDelete(scope.row)"
            v-hasPermi="['system:group:remove']"
          >删除</el-button>
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

    <!-- 用户组新增/编辑 -->
    <el-dialog
      :title="title"
      :visible.sync="open"
      width="560px"
      append-to-body
    >
      <el-form
        ref="form"
        :model="form"
        :rules="rules"
        label-width="110px"
      >
        <el-form-item label="用户组名称" prop="groupName">
          <el-input
            v-model="form.groupName"
            placeholder="例如：公开文档组"
          />
        </el-form-item>

        <el-form-item label="用户组编码" prop="groupCode">
          <el-input
            v-model="form.groupCode"
            placeholder="例如：GROUP_PUBLIC"
          />
        </el-form-item>

        <el-form-item label="知悉范围" prop="scopeCode">
          <el-input
            v-model="form.scopeCode"
            placeholder="例如：PUBLIC"
          />
        </el-form-item>

        <el-form-item label="用户组密级" prop="groupSecretLevel">
          <el-select
            v-model="form.groupSecretLevel"
            style="width:100%"
          >
            <el-option label="公开" value="PUBLIC" />
            <el-option label="内部" value="INTERNAL" />
            <el-option label="秘密" value="SECRET" />
            <el-option label="机密" value="CONFIDENTIAL" />
          </el-select>
        </el-form-item>

        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio label="0">正常</el-radio>
            <el-radio label="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="备注">
          <el-input
            v-model="form.remark"
            type="textarea"
            :rows="3"
          />
        </el-form-item>
      </el-form>

      <div slot="footer">
        <el-button type="primary" @click="submitForm">保存</el-button>
        <el-button @click="cancel">取消</el-button>
      </div>
    </el-dialog>

    <!-- 用户组成员管理 -->
    <el-dialog
      :title="memberDialogTitle"
      :visible.sync="memberOpen"
      width="760px"
      append-to-body
    >
      <div v-if="currentGroup" class="member-summary">
        <div>
          <strong>{{ currentGroup.groupName }}</strong>
          <span class="summary-code">
            {{ currentGroup.groupCode }}
          </span>
        </div>

        <div>
          知悉范围：
          <el-tag size="mini">
            {{ currentGroup.scopeCode || '-' }}
          </el-tag>
        </div>
      </div>

      <el-divider content-position="left">
        添加成员
      </el-divider>

      <div class="add-member-row">
        <el-select
          v-model="candidateUserId"
          filterable
          clearable
          placeholder="选择需要加入的用户"
          style="width:430px"
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
          :disabled="!candidateUserId"
          @click="addMember"
        >
          加入用户组
        </el-button>
      </div>

      <el-divider content-position="left">
        当前成员
      </el-divider>

      <el-table
        v-loading="memberLoading"
        :data="memberList"
        border
      >
        <el-table-column
          label="用户名"
          prop="userName"
          min-width="130"
        />

        <el-table-column
          label="姓名"
          prop="nickName"
          min-width="130"
        />

        <el-table-column
          label="状态"
          width="90"
          align="center"
        >
          <template slot-scope="scope">
            <el-tag
              size="mini"
              :type="scope.row.status === '0' ? 'success' : 'info'"
            >
              {{ scope.row.status === '0' ? '正常' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column
          label="加入时间"
          width="170"
        >
          <template slot-scope="scope">
            {{ parseTime(scope.row.joinTime, '{y}-{m}-{d} {h}:{i}:{s}') }}
          </template>
        </el-table-column>

        <el-table-column
          label="操作"
          width="100"
          align="center"
        >
          <template slot-scope="scope">
            <el-button
              type="text"
              class="danger-text"
              @click="removeMember(scope.row)"
            >移出</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div slot="footer">
        <el-button @click="memberOpen = false">关闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
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
        scopeCode: null,
        groupSecretLevel: null,
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
        ],
        scopeCode: [
          {
            required: true,
            message: '知悉范围不能为空',
            trigger: 'blur'
          }
        ],
        groupSecretLevel: [
          {
            required: true,
            message: '请选择用户组密级',
            trigger: 'change'
          }
        ]
      }
    }
  },

  computed: {
    memberDialogTitle() {
      if (!this.currentGroup) {
        return '用户组成员管理'
      }

      return this.currentGroup.groupName + ' - 成员管理'
    }
  },

  created() {
    this.getList()
  },

  methods: {
    getList() {
      this.loading = true

      listGroup(this.queryParams).then(response => {
        this.groupList = response.rows || []
        this.total = response.total || 0
        this.loading = false
        this.loadMemberCounts()
      })
    },

    loadMemberCounts() {
      getGroupMemberCounts().then(response => {
        const map = {}

        ;(response.data || []).forEach(item => {
          map[String(item.groupId)] =
            Number(item.memberCount) || 0
        })

        this.memberCountMap = map
      })
    },

    memberCount(groupId) {
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
      this.form = {
        id: null,
        groupCode: null,
        groupName: null,
        scopeCode: null,
        groupSecretLevel: 'PUBLIC',
        status: '0',
        remark: null
      }

      this.resetForm('form')
    },

    handleAdd() {
      this.reset()
      this.title = '新建用户组'
      this.open = true
    },

    handleUpdate(row) {
      this.reset()

      getGroup(row.id).then(response => {
        this.form = response.data
        this.title = '编辑用户组'
        this.open = true
      })
    },

    cancel() {
      this.open = false
      this.reset()
    },

    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) {
          return
        }

        const request = this.form.id
          ? updateGroup(this.form)
          : addGroup(this.form)

        request.then(() => {
          this.$modal.msgSuccess(
            this.form.id ? '修改成功' : '新增成功'
          )

          this.open = false
          this.getList()
        })
      })
    },

    handleDelete(row) {
      this.$modal
        .confirm(
          '确认删除用户组“' +
          row.groupName +
          '”吗？'
        )
        .then(() => delGroup(row.id))
        .then(() => {
          this.$modal.msgSuccess('删除成功')
          this.getList()
        })
        .catch(() => {})
    },

    handleExport() {
      this.download(
        'system/group/export',
        {
          ...this.queryParams
        },
        'group_' + new Date().getTime() + '.xlsx'
      )
    },

    openMembers(row) {
      this.currentGroup = row
      this.memberOpen = true
      this.candidateUserId = null
      this.loadMembers()
    },

    loadMembers() {
      if (!this.currentGroup) {
        return
      }

      this.memberLoading = true

      Promise.all([
        listGroupMembers(this.currentGroup.id),
        listGroupCandidates(this.currentGroup.id)
      ]).then(([members, candidates]) => {
        this.memberList = members.data || []
        this.candidateList = candidates.data || []
        this.memberLoading = false
        this.loadMemberCounts()
      }).catch(() => {
        this.memberLoading = false
      })
    },

    addMember() {
      if (!this.candidateUserId) {
        return
      }

      addGroupMember({
        groupId: this.currentGroup.id,
        userId: this.candidateUserId
      }).then(() => {
        this.$modal.msgSuccess('成员添加成功')
        this.candidateUserId = null
        this.loadMembers()
      })
    },

    removeMember(user) {
      this.$modal
        .confirm(
          '确认将用户“' +
          user.userName +
          '”移出该用户组吗？'
        )
        .then(() => {
          return removeGroupMember(
            this.currentGroup.id,
            user.userId
          )
        })
        .then(() => {
          this.$modal.msgSuccess('成员已移出')
          this.loadMembers()
        })
        .catch(() => {})
    },

    userLabel(user) {
      if (
        user.nickName &&
        user.nickName !== user.userName
      ) {
        return (
          user.nickName +
          '（' +
          user.userName +
          '）'
        )
      }

      return user.userName
    },

    secretText(value) {
      const map = {
        PUBLIC: '公开',
        INTERNAL: '内部',
        SECRET: '秘密',
        CONFIDENTIAL: '机密'
      }

      return map[value] || value || '未配置'
    },

    secretTagType(value) {
      const map = {
        PUBLIC: 'success',
        INTERNAL: 'info',
        SECRET: 'warning',
        CONFIDENTIAL: 'danger'
      }

      return map[value] || 'info'
    }
  }
}
</script>

<style scoped>
.group-page {
  padding: 20px;
}

.page-header {
  margin-bottom: 18px;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}

.page-desc {
  margin-top: 7px;
  color: #909399;
  font-size: 13px;
}

.mb20 {
  margin-bottom: 20px;
}

.member-summary {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #f5f7fa;
  padding: 14px 16px;
  border-radius: 4px;
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
