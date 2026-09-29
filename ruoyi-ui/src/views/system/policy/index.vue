<template>
  <div class="app-container policy-page">

    <div class="page-header">
      <div class="page-title">访问策略管理</div>
      <div class="page-desc">
        统一管理访问规则及其适用用户或用户组，策略绑定不再单独维护。
      </div>
    </div>

    <el-alert
      title="策略规则定义“允许访问什么”，适用对象决定“该策略对谁生效”。"
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
      <el-form-item label="策略编码" prop="policyCode">
        <el-input
          v-model="queryParams.policyCode"
          placeholder="请输入策略编码"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>

      <el-form-item label="策略名称" prop="policyName">
        <el-input
          v-model="queryParams.policyName"
          placeholder="请输入策略名称"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>

      <el-form-item label="优先级" prop="priority">
        <el-input
          v-model="queryParams.priority"
          placeholder="请输入优先级"
          clearable
          @keyup.enter.native="handleQuery"
        />
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
          v-hasPermi="['system:policy:add']"
        >新建策略</el-button>
      </el-col>

      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['system:policy:export']"
        >导出</el-button>
      </el-col>

      <right-toolbar
        :showSearch.sync="showSearch"
        @queryTable="getList"
      />
    </el-row>

    <el-table
      v-loading="loading"
      :data="policyList"
      border
      stripe
    >
      <el-table-column
        label="策略名称"
        prop="policyName"
        min-width="150"
      />

      <el-table-column
        label="策略编码"
        prop="policyCode"
        min-width="160"
      />

      <el-table-column
        label="效果"
        width="90"
        align="center"
      >
        <template slot-scope="scope">
          <el-tag
            size="mini"
            :type="isAllow(scope.row.effect) ? 'success' : 'danger'"
          >
            {{ isAllow(scope.row.effect) ? '允许' : '拒绝' }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column
        label="适用对象"
        min-width="230"
      >
        <template slot-scope="scope">
          <div v-if="bindingsOf(scope.row.id).length">
            <el-tag
              v-for="binding in bindingsOf(scope.row.id)"
              :key="binding.id"
              size="mini"
              style="margin:2px"
            >
              {{ bindingTargetName(binding) }}
            </el-tag>
          </div>

          <span v-else class="empty-text">
            未配置
          </span>
        </template>
      </el-table-column>

      <el-table-column
        label="资源范围"
        prop="resourceExpr"
        min-width="170"
        show-overflow-tooltip
      >
        <template slot-scope="scope">
          {{ scope.row.resourceExpr || '未限制' }}
        </template>
      </el-table-column>

      <el-table-column
        label="环境限制"
        prop="envExpr"
        min-width="150"
        show-overflow-tooltip
      >
        <template slot-scope="scope">
          {{ scope.row.envExpr || '无限制' }}
        </template>
      </el-table-column>

      <el-table-column
        label="优先级"
        prop="priority"
        width="90"
        align="center"
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
        label="操作"
        width="290"
        fixed="right"
        align="center"
      >
        <template slot-scope="scope">

          <el-button
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['system:policy:edit']"
          >编辑</el-button>

          <el-button
            type="text"
            icon="el-icon-user"
            @click="openBindings(scope.row)"
          >适用对象</el-button>

          <el-button
            type="text"
            icon="el-icon-time"
            @click="handleVersions(scope.row)"
            v-hasPermi="['system:policy:query']"
          >版本</el-button>

          <el-button
            type="text"
            icon="el-icon-delete"
            class="danger-text"
            @click="handleDelete(scope.row)"
            v-hasPermi="['system:policy:remove']"
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

    <!-- 策略新增/编辑 -->
    <el-dialog
      :title="title"
      :visible.sync="open"
      width="680px"
      append-to-body
    >
      <el-form
        ref="form"
        :model="form"
        :rules="rules"
        label-width="110px"
      >
        <el-row :gutter="18">

          <el-col :span="12">
            <el-form-item label="策略名称" prop="policyName">
              <el-input
                v-model="form.policyName"
                placeholder="例如：公开文档读取策略"
              />
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="策略编码" prop="policyCode">
              <el-input
                v-model="form.policyCode"
                placeholder="例如：RAG_PUBLIC_READ"
              />
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="访问效果" prop="effect">
              <el-radio-group v-model="form.effect">
                <el-radio label="0">允许</el-radio>
                <el-radio label="1">拒绝</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="优先级" prop="priority">
              <el-input-number
                v-model="form.priority"
                :min="1"
                :max="9999"
                controls-position="right"
                style="width:100%"
              />
            </el-form-item>
          </el-col>

          <el-col :span="24">
            <el-form-item label="资源范围">
              <el-input
                v-model="form.resourceExpr"
                placeholder="例如：scope_code=PUBLIC"
              />
              <div class="field-help">
                用于描述该策略可以访问的资源范围。
              </div>
            </el-form-item>
          </el-col>

          <el-col :span="24">
            <el-form-item label="环境限制">
              <el-input
                v-model="form.envExpr"
                placeholder="不填写表示无限制，例如：time in [09:00-18:00]"
              />
            </el-form-item>
          </el-col>

          <el-col :span="24">
            <el-collapse>
              <el-collapse-item title="高级兼容配置（一般无需修改）">

                <el-form-item label="主体类型">
                  <el-select
                    v-model="form.subjectType"
                    style="width:100%"
                  >
                    <el-option label="用户组" value="GROUP" />
                    <el-option label="用户" value="USER" />
                  </el-select>
                </el-form-item>

                <el-form-item label="主体表达式">
                  <el-input
                    v-model="form.subjectExpr"
                    placeholder="保留原策略表达式兼容能力"
                  />
                </el-form-item>

              </el-collapse-item>
            </el-collapse>
          </el-col>

          <el-col :span="24">
            <el-form-item label="状态" prop="status">
              <el-radio-group v-model="form.status">
                <el-radio label="0">正常</el-radio>
                <el-radio label="1">停用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>

          <el-col :span="24">
            <el-form-item label="备注">
              <el-input
                v-model="form.remark"
                type="textarea"
                :rows="3"
              />
            </el-form-item>
          </el-col>

        </el-row>
      </el-form>

      <div slot="footer">
        <el-button type="primary" @click="submitForm">
          保存
        </el-button>
        <el-button @click="cancel">
          取消
        </el-button>
      </div>
    </el-dialog>

    <!-- 策略适用对象 -->
    <el-dialog
      :title="bindingDialogTitle"
      :visible.sync="bindingOpen"
      width="720px"
      append-to-body
    >
      <el-alert
        title="策略绑定到用户或用户组后，才会进入对应用户的权限上下文。"
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom:18px"
      />

      <div class="binding-add-row">

        <el-select
          v-model="newBinding.bindType"
          style="width:130px"
          @change="newBinding.bindTargetId = null"
        >
          <el-option label="用户组" value="GROUP" />
          <el-option label="用户" value="USER" />
        </el-select>

        <el-select
          v-if="newBinding.bindType === 'GROUP'"
          v-model="newBinding.bindTargetId"
          filterable
          clearable
          placeholder="选择用户组"
          style="flex:1"
        >
          <el-option
            v-for="group in groupOptions"
            :key="group.id"
            :label="group.groupName + '（' + group.groupCode + '）'"
            :value="group.id"
          />
        </el-select>

        <el-select
          v-else
          v-model="newBinding.bindTargetId"
          filterable
          clearable
          placeholder="选择用户"
          style="flex:1"
        >
          <el-option
            v-for="user in userOptions"
            :key="user.userId"
            :label="userLabel(user)"
            :value="user.userId"
          />
        </el-select>

        <el-button
          type="primary"
          icon="el-icon-plus"
          :disabled="!newBinding.bindTargetId"
          @click="addBinding"
        >添加</el-button>

      </div>

      <el-table
        v-if="currentPolicy"
        :data="bindingsOf(currentPolicy.id)"
        border
      >
        <el-table-column
          label="对象类型"
          width="110"
          align="center"
        >
          <template slot-scope="scope">
            {{ bindingTypeText(scope.row.bindType) }}
          </template>
        </el-table-column>

        <el-table-column
          label="适用对象"
          min-width="280"
        >
          <template slot-scope="scope">
            {{ bindingTargetName(scope.row) }}
          </template>
        </el-table-column>

        <el-table-column
          label="状态"
          width="90"
          align="center"
        >
          <template>
            <el-tag type="success" size="mini">
              生效
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column
          label="操作"
          width="110"
          align="center"
        >
          <template slot-scope="scope">
            <el-button
              type="text"
              class="danger-text"
              @click="removeBinding(scope.row)"
            >取消绑定</el-button>
          </template>
        </el-table-column>

      </el-table>

      <div slot="footer">
        <el-button @click="bindingOpen = false">
          关闭
        </el-button>
      </div>
    </el-dialog>

    <!-- 策略版本历史 -->
    <el-dialog
      :title="versionTitle"
      :visible.sync="versionOpen"
      width="900px"
      append-to-body
    >
      <el-table
        :data="versionList"
        border
        stripe
      >
        <el-table-column
          label="版本"
          width="80"
          align="center"
        >
          <template slot-scope="scope">
            V{{ scope.row.versionNo }}
          </template>
        </el-table-column>

        <el-table-column
          label="变更类型"
          prop="changeType"
          width="110"
          align="center"
        >
          <template slot-scope="scope">
            <el-tag
              size="mini"
              :type="scope.row.changeType === 'UPDATE' ? 'warning' : 'success'"
            >
              {{ changeTypeText(scope.row.changeType) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column
          label="策略名称"
          prop="policyName"
          min-width="150"
        />

        <el-table-column
          label="效果"
          width="90"
          align="center"
        >
          <template slot-scope="scope">
            {{ isAllow(scope.row.effect) ? '允许' : '拒绝' }}
          </template>
        </el-table-column>

        <el-table-column
          label="资源范围"
          prop="resourceExpr"
          min-width="160"
          show-overflow-tooltip
        />

        <el-table-column
          label="优先级"
          prop="priority"
          width="80"
          align="center"
        />

        <el-table-column
          label="变更人"
          prop="changeBy"
          width="100"
        />

        <el-table-column
          label="变更时间"
          prop="changeTime"
          width="170"
        >
          <template slot-scope="scope">
            {{
              parseTime(
                scope.row.changeTime,
                '{y}-{m}-{d} {h}:{i}:{s}'
              )
            }}
          </template>
        </el-table-column>

      </el-table>

      <div slot="footer">
        <el-button @click="versionOpen = false">
          关闭
        </el-button>
      </div>
    </el-dialog>

  </div>
</template>

<script>
import {
  listPolicy,
  getPolicy,
  delPolicy,
  addPolicy,
  updatePolicy,
  getPolicyVersions
} from '@/api/system/policy'

import {
  listPolicyBind,
  addPolicyBind,
  delPolicyBind
} from '@/api/system/policyBind'

import {
  listGroup
} from '@/api/system/group'

import {
  listUser
} from '@/api/system/user'

export default {
  name: 'Policy',

  data() {
    return {
      loading: true,
      showSearch: true,

      total: 0,
      policyList: [],

      open: false,
      title: '',

      ids: [],

      allBindings: [],
      groupOptions: [],
      userOptions: [],

      bindingOpen: false,
      currentPolicy: null,

      newBinding: {
        bindType: 'GROUP',
        bindTargetId: null
      },

      versionOpen: false,
      versionTitle: '策略版本历史',
      versionList: [],

      queryParams: {
        pageNum: 1,
        pageSize: 10,
        policyCode: null,
        policyName: null,
        effect: null,
        subjectType: null,
        priority: null,
        status: null
      },

      form: {},

      rules: {
        policyCode: [
          {
            required: true,
            message: '策略编码不能为空',
            trigger: 'blur'
          }
        ],
        policyName: [
          {
            required: true,
            message: '策略名称不能为空',
            trigger: 'blur'
          }
        ],
        effect: [
          {
            required: true,
            message: '请选择访问效果',
            trigger: 'change'
          }
        ],
        priority: [
          {
            required: true,
            message: '优先级不能为空',
            trigger: 'blur'
          }
        ]
      }
    }
  },

  computed: {
    bindingDialogTitle() {
      if (!this.currentPolicy) {
        return '策略适用对象'
      }

      return this.currentPolicy.policyName + ' - 适用对象'
    }
  },

  created() {
    this.loadReferenceData()
  },

  methods: {
    loadReferenceData() {
      Promise.all([
        listGroup({
          pageNum: 1,
          pageSize: 1000
        }),
        listUser({
          pageNum: 1,
          pageSize: 1000
        })
      ]).then(([groups, users]) => {
        this.groupOptions = groups.rows || []
        this.userOptions = users.rows || []
        this.getList()
      }).catch(() => {
        this.getList()
      })
    },

    getList() {
      this.loading = true

      Promise.all([
        listPolicy(this.queryParams),
        this.refreshBindings()
      ]).then(([response]) => {
        this.policyList = response.rows || []
        this.total = response.total || 0
        this.loading = false
      }).catch(() => {
        this.loading = false
      })
    },

    refreshBindings() {
      return listPolicyBind({
        pageNum: 1,
        pageSize: 1000
      }).then(response => {
        this.allBindings = response.rows || []
        return response
      })
    },

    bindingsOf(policyId) {
      return this.allBindings.filter(item => {
        return (
          String(item.policyId) === String(policyId) &&
          item.status === '0'
        )
      })
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
        policyCode: null,
        policyName: null,
        effect: '0',
        subjectType: 'GROUP',
        subjectExpr: null,
        resourceExpr: null,
        envExpr: null,
        priority: 100,
        status: '0',
        remark: null
      }

      this.resetForm('form')
    },

    handleAdd() {
      this.reset()
      this.title = '新建访问策略'
      this.open = true
    },

    handleUpdate(row) {
      this.reset()

      getPolicy(row.id).then(response => {
        this.form = response.data
        this.title = '编辑访问策略'
        this.open = true
      })
    },

    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) {
          return
        }

        const request = this.form.id
          ? updatePolicy(this.form)
          : addPolicy(this.form)

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
          '确认删除访问策略“' +
          row.policyName +
          '”吗？'
        )
        .then(() => delPolicy(row.id))
        .then(() => {
          this.$modal.msgSuccess('删除成功')
          this.getList()
        })
        .catch(() => {})
    },

    handleExport() {
      this.download(
        'system/policy/export',
        {
          ...this.queryParams
        },
        'policy_' + new Date().getTime() + '.xlsx'
      )
    },

    openBindings(row) {
      this.currentPolicy = row

      this.newBinding = {
        bindType: 'GROUP',
        bindTargetId: null
      }

      this.bindingOpen = true
      this.refreshBindings()
    },

    addBinding() {
      if (!this.currentPolicy || !this.newBinding.bindTargetId) {
        return
      }

      const exists =
        this.bindingsOf(this.currentPolicy.id).some(item => {
          return (
            item.bindType === this.newBinding.bindType &&
            String(item.bindTargetId) ===
              String(this.newBinding.bindTargetId)
          )
        })

      if (exists) {
        this.$message.warning('该对象已经绑定当前策略')
        return
      }

      addPolicyBind({
        policyId: this.currentPolicy.id,
        bindType: this.newBinding.bindType,
        bindTargetId: Number(this.newBinding.bindTargetId),
        status: '0',
        remark: '通过访问策略管理页面配置'
      }).then(() => {
        this.$modal.msgSuccess('适用对象添加成功')
        this.newBinding.bindTargetId = null
        this.refreshBindings()
      })
    },

    removeBinding(binding) {
      this.$modal
        .confirm('确认取消该对象的策略绑定吗？')
        .then(() => delPolicyBind(binding.id))
        .then(() => {
          this.$modal.msgSuccess('绑定已取消')
          this.refreshBindings()
        })
        .catch(() => {})
    },

    bindingTypeText(type) {
      if (type === 'GROUP') {
        return '用户组'
      }

      if (type === 'USER') {
        return '用户'
      }

      if (type === 'DOC') {
        return '文档'
      }

      if (type === 'DIRECTORY') {
        return '目录'
      }

      return type || '-'
    },

    bindingTargetName(binding) {
      const id = String(binding.bindTargetId)

      if (binding.bindType === 'GROUP') {
        const group = this.groupOptions.find(
          item => String(item.id) === id
        )

        return group
          ? group.groupName
          : '用户组 #' + id
      }

      if (binding.bindType === 'USER') {
        const user = this.userOptions.find(
          item => String(item.userId) === id
        )

        return user
          ? (user.nickName || user.userName)
          : '用户 #' + id
      }

      return '#' + id
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

    handleVersions(row) {
      getPolicyVersions(row.id).then(response => {
        this.versionList = response.data || []
        this.versionTitle =
          row.policyName + ' - 版本历史'
        this.versionOpen = true
      })
    },

    isAllow(effect) {
      return effect === '0' || effect === 'ALLOW'
    },

    changeTypeText(type) {
      if (type === 'BASELINE') {
        return '初始基线'
      }

      if (type === 'CREATE') {
        return '新建'
      }

      if (type === 'UPDATE') {
        return '更新'
      }

      return type || '-'
    }
  }
}
</script>

<style scoped>
.policy-page {
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

.empty-text {
  color: #909399;
}

.binding-add-row {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
}

.field-help {
  margin-top: 5px;
  color: #909399;
  font-size: 12px;
}

.danger-text {
  color: #f56c6c !important;
}
</style>
