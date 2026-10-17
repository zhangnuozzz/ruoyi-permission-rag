<template>
  <div class="app-container policy-page identity-access-page">
    <header class="identity-header">
      <div>
        <h1>访问策略管理</h1>
        <p>为指定人员或用户组配置附加访问限制。</p>
      </div>
      <span class="identity-count">当前筛选：{{ total }} 条</span>
    </header>

    <div class="identity-rule-note" role="note">
      当前支持普通人员的工作时间限制。策略不提高人员密级，也不扩大知悉范围。
    </div>

    <el-form
      v-show="showSearch"
      ref="queryForm"
      class="identity-filter"
      :model="queryParams"
      size="small"
      :inline="true"
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
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">
          搜索
        </el-button>

        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8 identity-toolbar">
      <el-col :span="1.5">
        <el-button
          v-hasPermi="['system:policy:add']"
          type="primary"
          plain
          icon="el-icon-plus"
          size="mini"
          @click="handleAdd"
        >
          新建策略
        </el-button>
      </el-col>

      <el-col :span="1.5">
        <el-button
          v-hasPermi="['system:policy:export']"
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          :loading="exporting"
          @click="handleExport"
        >
          导出
        </el-button>
      </el-col>

      <right-toolbar :show-search.sync="showSearch" @queryTable="getList" />
    </el-row>

    <div v-if="listError" class="identity-feedback" role="alert">
      <span>{{ listError }}</span>
      <el-button type="text" :disabled="loading" @click="getList">重试</el-button>
    </div>
    <div v-if="bindingsError" class="identity-feedback" role="status">
      <span>{{ bindingsError }}，适用对象暂不能确认。</span>
      <el-button
        v-if="can('system:policyBind:list')"
        type="text"
        :disabled="bindingsLoading"
        @click="refreshBindings"
      >
        重试
      </el-button>
    </div>
    <div v-if="actionError" class="identity-feedback" role="alert">
      {{ actionError }}
    </div>
    <el-table
      v-loading="loading"
      :data="policyList"
      size="small"
      :empty-text="listError ? '策略数据未能加载' : '未找到符合条件的策略'"
    >
      <el-table-column label="策略名称 / 编码" min-width="210">
        <template slot-scope="scope">
          <div class="identity-person-name">{{ scope.row.policyName }}</div>
          <div class="identity-secondary identity-code">{{ scope.row.policyCode }}</div>
        </template>
      </el-table-column>
      <el-table-column label="规则说明" min-width="250">
        <template slot-scope="scope">
          <div>{{ ruleSummary(scope.row) }}</div>
          <div class="identity-expression-note">{{ ruleSupport(scope.row) }}</div>
        </template>
      </el-table-column>
      <el-table-column label="适用对象" min-width="200">
        <template slot-scope="scope">
          <span v-if="bindingsLoading" class="identity-muted">加载中</span>
          <span v-else-if="bindingsError" class="identity-muted">待确认</span>
          <div v-else-if="bindingsOf(scope.row.id).length">
            <el-tag
              v-for="binding in bindingsOf(scope.row.id).slice(0, 2)"
              :key="binding.id"
              size="mini"
              style="margin: 2px"
            >
              {{ bindingTargetName(binding) }}
            </el-tag>
            <span v-if="bindingsOf(scope.row.id).length > 2" class="identity-muted">
              另 {{ bindingsOf(scope.row.id).length - 2 }} 项
            </span>
          </div>
          <span v-else class="identity-muted">未绑定对象</span>
        </template>
      </el-table-column>
      <el-table-column label="配置状态" width="100">
        <template slot-scope="scope">
          <el-tag size="mini" :type="scope.row.status === '0' ? 'success' : 'info'">
            {{ scope.row.status === '0' ? '已启用' : '已停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="优先级" prop="priority" width="85" align="center" />
      <el-table-column label="操作" width="190" fixed="right" align="center">
        <template slot-scope="scope">
          <el-button
            v-if="can('system:policy:edit') && can('system:policy:query')"
            type="text"
            size="mini"
            @click="handleUpdate(scope.row)"
          >
            编辑
          </el-button>
          <el-button
            v-if="can('system:policyBind:list')"
            type="text"
            size="mini"
            @click="openBindings(scope.row)"
          >
            适用对象
          </el-button>
          <el-dropdown
            v-if="can('system:policy:query') || can('system:policy:remove')"
            trigger="click"
            @command="handlePolicyAction($event, scope.row)"
          >
            <button
              type="button"
              class="el-button el-button--text el-button--mini"
              :aria-label="'更多操作：' + scope.row.policyName"
            >
              更多
              <i class="el-icon-arrow-down el-icon--right" />
            </button>
            <el-dropdown-menu slot="dropdown">
              <el-dropdown-item v-if="can('system:policy:query')" command="versions">
                版本历史
              </el-dropdown-item>
              <el-dropdown-item v-if="can('system:policy:remove')" :disabled="deletingId !== null" command="delete">
                删除策略
              </el-dropdown-item>
            </el-dropdown-menu>
          </el-dropdown>
        </template>
      </el-table-column>
    </el-table>
    <p class="identity-muted">其他规则暂仅保存配置，不参与访问判断；当前时间判断不按优先级排序。</p>

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
      :before-close="closePolicyDialog"
      :close-on-press-escape="!saving"
      append-to-body
      custom-class="identity-access-dialog"
      :close-on-click-modal="false"
    >
      <div v-if="saveError" class="identity-form-feedback" role="alert">{{ saveError }}</div>
      <div class="identity-pending-note">
        目前支持普通人员的固定工作时间限制，管理员不受此项限制；其他表达式暂仅保存配置。
      </div>
      <el-button size="small" plain @click="applyWorkingHoursTemplate">
        套用工作时间限制（09:00—18:00）
      </el-button>
      <p class="identity-help">
        保存后请配置“适用对象”。绑定用户组表示限制该组成员，并非仅限制该组文档。
      </p>
      <el-form ref="form" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="18">
          <el-col :span="12">
            <el-form-item label="策略名称" prop="policyName">
              <el-input v-model="form.policyName" placeholder="例如：研发人员工作时间访问限制" />
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="策略编码" prop="policyCode">
              <el-input v-model="form.policyCode" placeholder="例如：RAG_PUBLIC_READ" />
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="访问效果" prop="effect">
              <el-radio-group v-model="form.effect">
                <el-radio label="0">允许配置（不扩大基础资格）</el-radio>
                <el-radio label="1">拒绝配置</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="优先级" prop="priority">
              <el-input-number
                v-model="form.priority"
                :min="0"
                :max="9999"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>

          <el-col :span="24">
            <el-form-item label="资源范围">
              <el-input
                v-model="form.resourceExpr"
                placeholder="兼容配置；当前固定时间判断不解析此字段"
              />
              <div class="field-help">
                此字段保留历史资源表达式。当前固定时间判断不执行资源范围匹配，不可据此推断文档已授权。
              </div>
            </el-form-item>
          </el-col>

          <el-col :span="24">
            <el-form-item label="环境限制">
              <el-input v-model="form.envExpr" placeholder="当前识别：time not in [09:00-18:00]" />
            </el-form-item>
          </el-col>

          <el-col :span="24">
            <el-collapse>
              <el-collapse-item title="高级兼容配置（一般无需修改）">
                <el-form-item label="主体类型">
                  <el-select v-model="form.subjectType" style="width: 100%">
                    <el-option label="用户组" value="GROUP" />
                    <el-option label="用户" value="USER" />
                  </el-select>
                </el-form-item>

                <el-form-item label="主体表达式">
                  <el-input v-model="form.subjectExpr" placeholder="保留原策略表达式兼容能力" />
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
              <el-input v-model="form.remark" type="textarea" :rows="3" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <div slot="footer">
        <el-button type="primary" :loading="saving" @click="submitForm">保存</el-button>
        <el-button :disabled="saving" @click="cancel">取消</el-button>
      </div>
    </el-dialog>

    <!-- 策略适用对象 -->
    <el-dialog
      :title="bindingDialogTitle"
      :visible.sync="bindingOpen"
      width="720px"
      :before-close="closeBindingDialog"
      :close-on-press-escape="!bindingBusy"
      append-to-body
      custom-class="identity-access-dialog"
      :close-on-click-modal="false"
    >
      <el-alert
        title="启用的策略通过有效绑定进入对应人员的权限上下文；停用用户组不传递组策略。"
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom: 18px"
      />

      <div v-if="bindingError || referenceError" class="identity-form-feedback" role="alert">
        {{ bindingError || referenceError }}
        <el-button v-if="referenceError" type="text" @click="loadReferenceData">
          重新加载选项
        </el-button>
      </div>
      <p class="identity-help">用户组绑定会把本策略应用于该组成员，不代表只限制绑定组的文档。</p>
      <p v-if="currentPolicy && currentPolicy.status !== '0'" class="identity-help">
        当前策略已停用，可查看或取消已有绑定，不能新增绑定。
      </p>
      <div v-if="can('system:policyBind:add')" class="binding-add-row">
        <el-select
          v-model="newBinding.bindType"
          style="width: 130px"
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
          style="flex: 1"
        >
          <el-option
            v-for="group in groupOptions"
            :key="group.id"
            :label="group.groupName + '（' + group.groupCode + '）' + (isBindingTargetAvailable(group) ? '' : ' · 不可新增')"
            :value="group.id"
            :disabled="!isBindingTargetAvailable(group)"
          />
        </el-select>

        <el-select
          v-else
          v-model="newBinding.bindTargetId"
          filterable
          clearable
          placeholder="选择用户"
          style="flex: 1"
        >
          <el-option
            v-for="user in userOptions"
            :key="user.userId"
            :label="userLabel(user) + (isBindingTargetAvailable(user) ? '' : ' · 不可新增')"
            :value="user.userId"
            :disabled="!isBindingTargetAvailable(user)"
          />
        </el-select>

        <el-button
          type="primary"
          icon="el-icon-plus"
          :disabled="
            !newBinding.bindTargetId ||
              !currentPolicy ||
              currentPolicy.status !== '0' ||
              referenceLoading ||
              bindingBusy ||
              bindingsLoading ||
              !!bindingsError ||
              !!referenceError
          "
          :loading="bindingSaving"
          @click="addBinding"
        >
          添加
        </el-button>
      </div>

      <el-table
        v-if="currentPolicy"
        v-loading="bindingsLoading"
        :data="policyBindings(currentPolicy.id)"
        :empty-text="bindingsError ? '适用对象未能加载' : '尚未绑定对象'"
        border
      >
        <el-table-column label="对象类型" width="110" align="center">
          <template slot-scope="scope">
            {{ bindingTypeText(scope.row.bindType) }}
          </template>
        </el-table-column>

        <el-table-column label="适用对象" min-width="280">
          <template slot-scope="scope">
            {{ bindingTargetName(scope.row) }}
          </template>
        </el-table-column>

        <el-table-column label="状态" width="90" align="center">
          <template slot-scope="scope">
            <el-tag :type="scope.row.status === '0' ? 'success' : 'info'" size="mini">
              {{ scope.row.status === '0' ? '绑定启用' : '绑定停用' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="110" align="center">
          <template slot-scope="scope">
            <el-button
              v-if="can('system:policyBind:remove')"
              type="text"
              class="danger-text"
              :disabled="bindingBusy || bindingsLoading || !!bindingsError"
              :loading="bindingRemovingId === scope.row.id"
              @click="removeBinding(scope.row)"
            >
              取消绑定
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div slot="footer">
        <el-button :disabled="bindingBusy" @click="bindingOpen = false">关闭</el-button>
      </div>
    </el-dialog>

    <!-- 策略版本历史 -->
    <el-dialog
      :title="versionTitle"
      :visible.sync="versionOpen"
      width="900px"
      append-to-body
      custom-class="identity-access-dialog"
      :close-on-click-modal="false"
    >
      <el-table :data="versionList" border stripe>
        <el-table-column label="版本" width="80" align="center">
          <template slot-scope="scope">V{{ scope.row.versionNo }}</template>
        </el-table-column>

        <el-table-column label="变更类型" prop="changeType" width="110" align="center">
          <template slot-scope="scope">
            <el-tag size="mini" :type="scope.row.changeType === 'UPDATE' ? 'warning' : 'success'">
              {{ changeTypeText(scope.row.changeType) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="策略名称" prop="policyName" min-width="150" />

        <el-table-column label="效果" width="90" align="center">
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

        <el-table-column label="优先级" prop="priority" width="80" align="center" />

        <el-table-column label="变更人" prop="changeBy" width="100" />

        <el-table-column label="变更时间" prop="changeTime" width="170">
          <template slot-scope="scope">
            {{ parseTime(scope.row.changeTime, '{y}-{m}-{d} {h}:{i}:{s}') }}
          </template>
        </el-table-column>
      </el-table>

      <div slot="footer">
        <el-button @click="versionOpen = false">关闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { exportTable } from '@/utils/tableExport'
import '@/assets/styles/identity-access.scss'
import { canUse, displayExpression, hasSupportedTimeCondition } from '@/utils/identityAccess'
import {
  listPolicy,
  getPolicy,
  delPolicy,
  addPolicy,
  updatePolicy,
  getPolicyVersions
} from '@/api/system/policy'

import { listPolicyBind, addPolicyBind, delPolicyBind } from '@/api/system/policyBind'

import { listGroup } from '@/api/system/group'

import { listUser } from '@/api/system/user'

export default {
  name: 'Policy',

  data() {
    return {
      loading: true,
      exporting: false,
      saving: false,
      deletingId: null,
      actionError: '',
      listError: '',
      saveError: '',
      bindingsError: '',
      bindingsLoading: false,
      bindingError: '',
      referenceError: '',
      referenceLoading: false,
      bindingSaving: false,
      bindingRemovingId: null,
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
    bindingBusy() {
      return this.bindingSaving || this.bindingRemovingId !== null
    },
    bindingDialogTitle() {
      if (!this.currentPolicy) {
        return '策略适用对象'
      }

      return this.currentPolicy.policyName + ' - 适用对象'
    }
  },

  created() {
    this.getList()
    this.loadReferenceData()
  },

  methods: {
    cancel() {
      if (!this.saving) {
        this.open = false
      }
    },

    can(permission) {
      return canUse(this.$store.getters.permissions, permission)
    },
    ruleSummary(policy) {
      if (hasSupportedTimeCondition(policy)) {
        return '工作时间外拒绝访问（09:00—18:00）'
      }
      if (policy.effect === '0' || policy.effect === 'ALLOW') {
        return '历史允许配置；不扩大人员密级或知悉范围'
      }
      return displayExpression(policy.envExpr) || '尚无可识别的附加条件'
    },
    ruleSupport(policy) {
      return hasSupportedTimeCondition(policy)
        ? '固定时间条件可识别，仍须绑定对象且启用'
        : '仅保存配置，暂未执行'
    },
    applyWorkingHoursTemplate() {
      this.form.effect = '1'
      this.form.envExpr = 'time not in [09:00-18:00]'
      this.form.resourceExpr = null
    },
    closePolicyDialog(done) {
      if (!this.saving) {
        done()
      }
    },
    closeBindingDialog(done) {
      if (!this.bindingBusy) {
        done()
      }
    },
    handlePolicyAction(command, row) {
      if (command === 'versions' && this.can('system:policy:query')) {
        this.handleVersions(row)
      }
      if (command === 'delete' && this.can('system:policy:remove')) {
        this.handleDelete(row)
      }
    },

    async loadReferenceData() {
      const requestId = (this.referenceRequestId || 0) + 1
      this.referenceRequestId = requestId
      this.referenceLoading = true
      this.referenceError = ''
      const groupRequest = this.can('system:group:list')
        ? listGroup({ pageNum: 1, pageSize: 1000 })
        : Promise.resolve({ rows: [] })
      const userRequest = this.can('system:user:list')
        ? listUser({ pageNum: 1, pageSize: 1000 })
        : Promise.resolve({ rows: [] })
      try {
        const results = await Promise.all([groupRequest, userRequest])
        if (requestId !== this.referenceRequestId) {
          return
        }
        this.groupOptions = Array.isArray(results[0].rows) ? results[0].rows : []
        this.userOptions = Array.isArray(results[1].rows) ? results[1].rows : []
        if (
          Number(results[0].total) > this.groupOptions.length ||
          Number(results[1].total) > this.userOptions.length
        ) {
          this.referenceError = '对象选项尚未完整加载，请联系管理员核对。'
        }
      } catch (error) {
        if (requestId === this.referenceRequestId) {
          this.referenceError = '人员或用户组选项加载失败，暂不能新增绑定。'
        }
      } finally {
        if (requestId === this.referenceRequestId) {
          this.referenceLoading = false
        }
      }
    },

    async getList() {
      this.loading = true
      this.listError = ''
      const requestId = (this.policyListRequestId || 0) + 1
      this.policyListRequestId = requestId
      this.refreshBindings()
      try {
        const response = await listPolicy(this.queryParams)
        if (requestId !== this.policyListRequestId) {
          return
        }
        this.policyList = Array.isArray(response.rows) ? response.rows : []
        this.total = Number(response.total) || 0
      } catch (error) {
        if (requestId !== this.policyListRequestId) {
          return
        }
        this.policyList = []
        this.total = 0
        this.listError = '策略列表加载失败，请重试。'
      } finally {
        if (requestId === this.policyListRequestId) {
          this.loading = false
        }
      }
    },

    async refreshBindings() {
      if (!this.can('system:policyBind:list')) {
        this.bindingsError = '当前账号没有读取策略绑定的权限'
        this.allBindings = []
        return
      }
      this.bindingsLoading = true
      this.bindingsError = ''
      const requestId = (this.bindingsRequestId || 0) + 1
      this.bindingsRequestId = requestId
      try {
        const response = await listPolicyBind({ pageNum: 1, pageSize: 1000 })
        if (requestId !== this.bindingsRequestId) {
          return
        }
        const rows = Array.isArray(response.rows) ? response.rows : []
        if (Number(response.total) > rows.length) {
          throw new Error('Incomplete bindings')
        }
        this.allBindings = rows
      } catch (error) {
        if (requestId !== this.bindingsRequestId) {
          return
        }
        this.allBindings = []
        this.bindingsError = '策略绑定未完整加载'
      } finally {
        if (requestId === this.bindingsRequestId) {
          this.bindingsLoading = false
        }
      }
    },

    policyBindings(policyId) {
      return this.allBindings.filter((item) => String(item.policyId) === String(policyId))
    },

    bindingsOf(policyId) {
      return this.allBindings.filter((item) => {
        return String(item.policyId) === String(policyId) && item.status === '0'
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
      this.saveError = ''
      this.form.effect = '1'
      this.form.envExpr = 'time not in [09:00-18:00]'
      this.title = '新建附加访问策略'
      this.open = true
    },

    async handleUpdate(row) {
      this.reset()
      this.saveError = ''
      try {
        const response = await getPolicy(row.id)
        this.form = response.data
        this.title = '编辑附加访问策略'
        this.open = true
      } catch (error) {
        this.$message.error('策略详情加载失败，请重试。')
      }
    },

    submitForm() {
      if (this.saving) {
        return
      }
      if (!this.can(this.form.id ? 'system:policy:edit' : 'system:policy:add')) {
        return
      }
      this.$refs.form.validate(async(valid) => {
        if (!valid || this.saving) {
          return
        }
        this.saving = true
        this.saveError = ''
        try {
          await (this.form.id ? updatePolicy(this.form) : addPolicy(this.form))
          this.$modal.msgSuccess('策略配置已保存；请核对适用对象与后端执行范围')
          this.open = false
          this.getList()
        } catch (error) {
          this.saveError = '策略保存失败，请核对后重试。'
        } finally {
          this.saving = false
        }
      })
    },

    async handleDelete(row) {
      if (this.deletingId !== null || !this.can('system:policy:remove')) {
        return
      }
      if (!row || !/^[1-9]\d*$/.test(String(row.id))) {
        this.actionError = '策略标识无效，请刷新后重试。'
        return
      }
      this.deletingId = row.id
      this.actionError = ''
      let confirmed = false
      try {
        await this.$modal.confirm('确认删除访问策略“' + row.policyName + '”吗？存在适用对象时须先取消绑定；停用绑定也需处理。')
        confirmed = true
        const response = await delPolicy(row.id)
        if (!response || response.code !== 200) {
          throw new Error((response && response.msg) || '未收到明确的删除成功结果，请刷新核对。')
        }
        this.$modal.msgSuccess('删除成功')
        if (this.policyList.length === 1 && this.queryParams.pageNum > 1) {
          this.queryParams.pageNum -= 1
        }
        await this.getList()
      } catch (error) {
        if (confirmed || (error !== 'cancel' && error !== 'close')) {
          this.actionError = (error && error.message) || '策略删除失败，请刷新核对。'
        }
      } finally {
        this.deletingId = null
      }
    },

    async handleExport() {
      if (this.exporting || !this.can('system:policy:export')) return
      this.exporting = true
      this.actionError = ''
      try {
        await exportTable('/system/policy/export', this.queryParams)
      } catch (error) {
        this.actionError = (error && error.message) || '导出失败，请稍后重试。'
      } finally {
        this.exporting = false
      }
    },

    openBindings(row) {
      this.currentPolicy = row
      this.newBinding = { bindType: 'GROUP', bindTargetId: null }
      this.bindingError = ''
      this.bindingOpen = true
      this.loadReferenceData()
      this.refreshBindings()
    },

    async addBinding() {
      if (
        !this.currentPolicy ||
        this.currentPolicy.status !== '0' ||
        this.referenceLoading ||
        !this.newBinding.bindTargetId ||
        this.bindingBusy ||
        this.bindingsLoading ||
        this.bindingsError ||
        this.referenceError ||
        !this.can('system:policyBind:add')
      ) {
        return
      }
      const options = this.newBinding.bindType === 'GROUP' ? this.groupOptions : this.userOptions
      const key = this.newBinding.bindType === 'GROUP' ? 'id' : 'userId'
      if (!options.some((item) =>
        String(item[key]) === String(this.newBinding.bindTargetId) && this.isBindingTargetAvailable(item)
      )) {
        this.$message.warning('请选择当前可用的人员或用户组。')
        return
      }
      const policyId = this.currentPolicy.id
      const type = this.newBinding.bindType
      const targetId = this.newBinding.bindTargetId
      if (
        this.policyBindings(policyId).some(
          (item) => item.bindType === type && String(item.bindTargetId) === String(targetId)
        )
      ) {
        this.$message.warning('该对象已有绑定，请先核对现有绑定状态。')
        return
      }
      this.bindingSaving = true
      this.bindingError = ''
      try {
        await addPolicyBind({
          policyId,
          bindType: type,
          bindTargetId: Number(targetId),
          status: '0',
          remark: '通过访问策略管理页面配置'
        })
        this.$modal.msgSuccess('适用对象已关联')
        this.newBinding.bindTargetId = null
        await this.refreshBindings()
      } catch (error) {
        this.bindingError = '绑定添加失败，请刷新后核实，避免重复添加。'
      } finally {
        this.bindingSaving = false
      }
    },

    async removeBinding(binding) {
      if (
        this.bindingBusy ||
        this.bindingsLoading ||
        this.bindingsError ||
        !this.can('system:policyBind:remove')
      ) {
        return
      }
      this.bindingRemovingId = binding.id
      this.bindingError = ''
      try {
        await this.$modal.confirm(
          '确认取消“' + this.bindingTargetName(binding) + '”与当前策略的关联吗？'
        )
        await delPolicyBind(binding.id)
        this.$modal.msgSuccess('策略关联已取消')
        await this.refreshBindings()
      } catch (error) {
        if (error !== 'cancel' && error !== 'close') {
          this.bindingError = '取消绑定失败，请刷新后核实。'
        }
      } finally {
        this.bindingRemovingId = null
      }
    },

    isBindingTargetAvailable(target) {
      return !!target && target.status === '0' && (!target.delFlag || target.delFlag === '0')
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
        const group = this.groupOptions.find((item) => String(item.id) === id)

        return group ? group.groupName : '用户组 #' + id
      }

      if (binding.bindType === 'USER') {
        const user = this.userOptions.find((item) => String(item.userId) === id)

        return user ? user.nickName || user.userName : '用户 #' + id
      }

      return '#' + id
    },

    userLabel(user) {
      if (user.nickName && user.nickName !== user.userName) {
        return user.nickName + '（' + user.userName + '）'
      }

      return user.userName
    },

    handleVersions(row) {
      getPolicyVersions(row.id).then((response) => {
        this.versionList = response.data || []
        this.versionTitle = row.policyName + ' - 版本历史'
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
