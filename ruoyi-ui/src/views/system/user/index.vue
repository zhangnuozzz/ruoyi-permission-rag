<template>
  <div class="app-container personnel-page identity-access-page">
    <header class="identity-header">
      <div>
        <h1>人员权限管理</h1>
        <p>设置人员密级与所属用户组，明确可接触的数据等级和知悉范围。</p>
      </div>
      <span class="identity-count">当前筛选：{{ total }} 人</span>
    </header>

    <el-row :gutter="20">
      <!-- 左侧组织机构 -->
      <el-col :span="4" :xs="24">
        <div class="org-panel">
          <div class="panel-title">
            <i class="el-icon-office-building" />
            组织机构
          </div>

          <el-input
            v-model="deptName"
            placeholder="搜索部门"
            clearable
            size="small"
            prefix-icon="el-icon-search"
            class="dept-search"
          />

          <el-tree
            ref="tree"
            :data="deptOptions"
            :props="defaultProps"
            :expand-on-click-node="false"
            :filter-node-method="filterNode"
            default-expand-all
            highlight-current
            @node-click="handleNodeClick"
          />
        </div>
      </el-col>

      <!-- 右侧人员 -->
      <el-col :span="20" :xs="24">
        <div class="personnel-workspace">
          <el-form
            v-show="showSearch"
            ref="queryForm"
            :model="queryParams"
            class="identity-filter"
            :inline="true"
            size="small"
          >
            <el-form-item label="人员账号" prop="userName">
              <el-input
                v-model="queryParams.userName"
                placeholder="请输入登录账号"
                clearable
                @keyup.enter.native="handleQuery"
              />
            </el-form-item>

            <el-form-item label="手机号码" prop="phonenumber">
              <el-input
                v-model="queryParams.phonenumber"
                placeholder="请输入手机号"
                clearable
                @keyup.enter.native="handleQuery"
              />
            </el-form-item>

            <el-form-item label="人员密级" prop="secretLevel">
              <el-select
                v-model="queryParams.secretLevel"
                placeholder="全部"
                clearable
                style="width: 170px"
              >
                <el-option label="公开 · 非密" value="PUBLIC" />
                <el-option label="内部 · 工作秘密" value="INTERNAL" />
                <el-option label="秘密 · 国家秘密" value="SECRET" />
                <el-option label="机密 · 国家秘密" value="CONFIDENTIAL" />
              </el-select>
            </el-form-item>

            <el-form-item label="账号状态" prop="status">
              <el-select
                v-model="queryParams.status"
                placeholder="全部"
                clearable
                style="width: 130px"
              >
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
                v-hasPermi="['system:user:add']"
                type="primary"
                plain
                icon="el-icon-plus"
                size="mini"
                @click="handleAdd"
              >
                新增人员
              </el-button>
            </el-col>

            <el-col :span="1.5">
              <el-button
                v-hasPermi="['system:user:edit']"
                type="success"
                plain
                icon="el-icon-edit"
                size="mini"
                :disabled="single || !can('system:user:query')"
                @click="handleUpdate"
              >
                修改
              </el-button>
            </el-col>

            <el-col :span="1.5">
              <el-button
                v-hasPermi="['system:user:remove']"
                type="danger"
                plain
                icon="el-icon-delete"
                size="mini"
                :disabled="multiple"
                @click="handleDelete"
              >
                删除
              </el-button>
            </el-col>

            <right-toolbar :show-search.sync="showSearch" @queryTable="getList" />
          </el-row>

          <div v-if="listError" class="identity-feedback" role="alert">
            <span>{{ listError }}</span>
            <el-button type="text" :disabled="loading" @click="getList">重试</el-button>
          </div>
          <el-table
            v-loading="loading"
            :data="userList"
            size="small"
            :empty-text="listError ? '人员数据未能加载' : '未找到符合条件的人员'"
            @selection-change="handleSelectionChange"
          >
            <el-table-column type="selection" width="44" align="center" />
            <el-table-column label="人员 / 账号" min-width="150">
              <template slot-scope="scope">
                <div class="identity-person-name">{{ scope.row.nickName || '未提供姓名' }}</div>
                <div class="identity-secondary">{{ scope.row.userName }}</div>
              </template>
            </el-table-column>
            <el-table-column label="部门 / 工号" min-width="130">
              <template slot-scope="scope">
                <div>{{ (scope.row.dept && scope.row.dept.deptName) || '未配置部门' }}</div>
                <div class="identity-secondary">
                  {{ scope.row.employeeNo || '未设置工号' }}
                </div>
              </template>
            </el-table-column>
            <el-table-column label="人员密级" width="110">
              <template slot-scope="scope">
                <el-tag size="mini" :type="secretType(scope.row.secretLevel)">
                  {{ secretText(scope.row.secretLevel) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="知悉用户组" min-width="210" show-overflow-tooltip>
              <template slot-scope="scope">
                <span v-if="scope.row.groupNames">{{ scope.row.groupNames }}</span>
                <span v-else class="identity-muted">未配置知悉组</span>
              </template>
            </el-table-column>
            <el-table-column label="登录账号状态" width="120" align="center">
              <template slot-scope="scope">
                <el-switch
                  v-if="can('system:user:edit')"
                  v-model="scope.row.status"
                  active-value="0"
                  inactive-value="1"
                  :disabled="statusBusyId !== null"
                  :aria-label="'切换账号 ' + scope.row.userName + ' 的状态'"
                  @change="handleStatusChange(scope.row)"
                />
                <el-tag v-else size="mini" :type="scope.row.status === '0' ? 'success' : 'info'">
                  {{ scope.row.status === '0' ? '启用' : '停用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="145" fixed="right" align="center">
              <template slot-scope="scope">
                <div class="identity-operation">
                  <el-button
                    v-if="can('system:user:edit') && can('system:user:query')"
                    type="text"
                    size="mini"
                    @click="handleUpdate(scope.row)"
                  >
                    配置权限
                  </el-button>
                  <el-dropdown
                    v-if="
                      can('system:user:resetPwd') ||
                        (can('system:user:remove') && String(scope.row.userId) !== '1')
                    "
                    trigger="click"
                    @command="handleRowAction($event, scope.row)"
                  >
                    <button
                      type="button"
                      class="el-button el-button--text el-button--mini"
                      :aria-label="'更多操作：' + scope.row.userName"
                    >
                      更多
                      <i class="el-icon-arrow-down el-icon--right" />
                    </button>
                    <el-dropdown-menu slot="dropdown">
                      <el-dropdown-item v-if="can('system:user:resetPwd')" command="password">
                        重置密码
                      </el-dropdown-item>
                      <el-dropdown-item
                        v-if="can('system:user:remove') && String(scope.row.userId) !== '1'"
                        command="delete"
                      >
                        删除人员
                      </el-dropdown-item>
                    </el-dropdown-menu>
                  </el-dropdown>
                </div>
              </template>
            </el-table-column>
          </el-table>
          <p class="identity-muted">实际访问还须通过状态、时间及附加策略校验。</p>

          <pagination
            v-show="total > 0"
            :total="total"
            :page.sync="queryParams.pageNum"
            :limit.sync="queryParams.pageSize"
            @pagination="getList"
          />
        </div>
      </el-col>
    </el-row>

    <!-- 人员新增/修改 -->
    <el-dialog
      :title="title"
      :visible.sync="open"
      width="960px"
      custom-class="identity-access-dialog"
      :close-on-click-modal="false"
      :close-on-press-escape="!saving"
      :before-close="closePersonnelDialog"
      append-to-body
      top="4vh"
    >
      <div v-if="saveError" class="identity-form-feedback" role="alert">{{ saveError }}</div>
      <div v-if="groupError" class="identity-form-feedback" role="alert">
        {{ groupError }}
        <el-button type="text" :loading="groupLoading" @click="getGroupOptions">
          重新加载用户组
        </el-button>
      </div>
      <el-alert
        title="人员密级决定可接触的最高数据密级；所属用户组集合决定秘密级、机密级国家秘密的知悉范围。"
        type="info"
        :closable="false"
        show-icon
        class="dialog-alert"
      />

      <el-form ref="form" :model="form" :rules="rules" label-width="105px">
        <div class="section-title">
          <i class="el-icon-user" />
          人员基本信息
        </div>

        <el-row :gutter="18">
          <el-col :span="8">
            <el-form-item label="姓名" prop="nickName">
              <el-input v-model="form.nickName" placeholder="请输入姓名" />
            </el-form-item>
          </el-col>

          <el-col :span="8">
            <el-form-item label="人员账号" prop="userName">
              <el-input v-model="form.userName" placeholder="登录账号" :disabled="!!form.userId" />
            </el-form-item>
          </el-col>

          <el-col :span="8">
            <el-form-item v-if="!form.userId" label="初始密码" prop="password">
              <el-input v-model="form.password" type="password" show-password />
            </el-form-item>
          </el-col>

          <el-col :span="8">
            <el-form-item label="工号">
              <el-input v-model="form.employeeNo" placeholder="人员工号" />
            </el-form-item>
          </el-col>

          <el-col :span="8">
            <el-form-item label="归属部门" prop="deptId">
              <treeselect
                v-model="form.deptId"
                :options="deptOptions"
                :show-count="true"
                placeholder="请选择组织部门"
              />
            </el-form-item>
          </el-col>

          <el-col :span="8">
            <el-form-item label="职务">
              <el-input v-model="form.jobTitle" placeholder="例如：高级工程师" />
            </el-form-item>
          </el-col>

          <el-col :span="8">
            <el-form-item label="性别">
              <el-select v-model="form.sex" style="width: 100%" placeholder="请选择">
                <el-option
                  v-for="item in sexOptions"
                  :key="item.dictValue"
                  :label="item.dictLabel"
                  :value="item.dictValue"
                />
              </el-select>
            </el-form-item>
          </el-col>

          <el-col :span="8">
            <el-form-item label="民族">
              <el-input v-model="form.ethnicity" placeholder="例如：汉族" />
            </el-form-item>
          </el-col>

          <el-col :span="8">
            <el-form-item label="出生地">
              <el-input v-model="form.birthPlace" placeholder="请输入出生地" />
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="身份证">
              <el-input v-model="form.idCardNo" placeholder="请输入身份证号码" maxlength="18" />
            </el-form-item>
          </el-col>

          <el-col :span="6">
            <el-form-item label="手机号">
              <el-input v-model="form.phonenumber" maxlength="11" />
            </el-form-item>
          </el-col>

          <el-col :span="6">
            <el-form-item label="邮箱">
              <el-input v-model="form.email" />
            </el-form-item>
          </el-col>
        </el-row>

        <div class="section-title permission-title">
          <i class="el-icon-lock" />
          核心访问权限
        </div>

        <el-row :gutter="18">
          <el-col :span="8">
            <el-form-item label="人员密级" prop="secretLevel">
              <el-select v-model="form.secretLevel" style="width: 100%">
                <el-option label="公开 · 非密" value="PUBLIC" />
                <el-option label="内部 · 工作秘密" value="INTERNAL" />
                <el-option label="秘密 · 国家秘密" value="SECRET" />
                <el-option label="机密 · 国家秘密" value="CONFIDENTIAL" />
              </el-select>
            </el-form-item>
          </el-col>

          <el-col :span="16">
            <el-form-item label="知悉用户组">
              <el-select
                v-model="form.groupIds"
                :loading="groupLoading"
                :disabled="groupLoading || !!groupError"
                multiple
                filterable
                clearable
                collapse-tags
                placeholder="可同时选择多个用户组"
                style="width: 100%"
              >
                <el-option
                  v-for="group in groupOptions"
                  :key="group.id"
                  :label="groupOptionLabel(group)"
                  :disabled="groupOptionDisabled(group)"
                  :value="group.id"
                />
              </el-select>

              <div class="field-help">用户组表示知悉范围，不提升人员密级。停用组可保留或移出，不参与访问判断；重新启用后按保留的成员关系生效。</div>
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 权限预览 -->
        <div class="permission-preview">
          <div class="preview-header">文档基础资格预览（不代表最终放行）</div>

          <div v-for="item in permissionPreview" :key="item.level" class="preview-row">
            <div class="preview-level">
              <el-tag size="mini" :type="secretType(item.level)">
                {{ item.name }}
              </el-tag>
            </div>

            <div class="preview-result">
              <i :class="item.allow ? 'el-icon-success allow-icon' : 'el-icon-error deny-icon'" />

              <span>
                {{ item.text }}
              </span>
            </div>
          </div>
        </div>

        <div class="section-title">
          <i class="el-icon-setting" />
          附加访问控制
        </div>

        <el-row :gutter="18">
          <el-col :span="8">
            <el-form-item label="访问状态" prop="accessStatus">
              <el-select v-model="form.accessStatus" style="width: 100%">
                <el-option label="启用" value="ACTIVE" />
                <el-option label="禁用" value="DISABLED" />
                <el-option label="锁定" value="LOCKED" />
              </el-select>
            </el-form-item>
          </el-col>

          <el-col :span="8">
            <el-form-item label="开始时间">
              <el-time-picker
                v-model="form.accessStartTime"
                value-format="HH:mm:ss"
                format="HH:mm:ss"
                placeholder="不限制"
                clearable
                style="width: 100%"
              />
            </el-form-item>
          </el-col>

          <el-col :span="8">
            <el-form-item label="结束时间">
              <el-time-picker
                v-model="form.accessEndTime"
                value-format="HH:mm:ss"
                format="HH:mm:ss"
                placeholder="不限制"
                clearable
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <div v-if="runtimeAttr" class="runtime-box">
          <div class="runtime-title">运行安全状态（系统自动维护）</div>

          <el-row :gutter="20">
            <el-col :span="6">
              风险等级：
              <strong>
                {{ runtimeAttr.riskLevel || '未取得' }}
              </strong>
            </el-col>

            <el-col :span="6">
              失败次数：
              <strong>
                {{ runtimeAttr.failCount == null ? '未取得' : runtimeAttr.failCount }}
              </strong>
            </el-col>

            <el-col :span="6">
              最近访问 IP：
              <strong>
                {{ runtimeAttr.lastAccessIp || '-' }}
              </strong>
            </el-col>

            <el-col :span="6">
              最近访问：
              <strong>
                {{ runtimeAttr.lastAccessTime || '-' }}
              </strong>
            </el-col>
          </el-row>
        </div>

        <el-collapse class="advanced-collapse">
          <el-collapse-item title="系统账户高级配置">
            <el-alert
              title="下面角色与岗位属于若依后台系统权限，与文档密级和国家秘密知悉范围是不同概念。"
              type="warning"
              :closable="false"
              show-icon
              class="advanced-alert"
            />

            <el-row :gutter="18">
              <el-col :span="8">
                <el-form-item label="账号状态">
                  <el-radio-group v-model="form.status">
                    <el-radio label="0">正常</el-radio>
                    <el-radio label="1">停用</el-radio>
                  </el-radio-group>
                </el-form-item>
              </el-col>

              <el-col :span="8">
                <el-form-item label="系统岗位">
                  <el-select v-model="form.postIds" multiple style="width: 100%">
                    <el-option
                      v-for="item in postOptions"
                      :key="item.postId"
                      :label="item.postName"
                      :value="item.postId"
                      :disabled="item.status == 1"
                    />
                  </el-select>
                </el-form-item>
              </el-col>

              <el-col :span="8">
                <el-form-item label="系统角色">
                  <el-select v-model="form.roleIds" multiple style="width: 100%">
                    <el-option
                      v-for="item in roleOptions"
                      :key="item.roleId"
                      :label="item.roleName"
                      :value="item.roleId"
                      :disabled="item.status == 1"
                    />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
          </el-collapse-item>
        </el-collapse>

        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>

      <div slot="footer">
        <el-button
          type="primary"
          :loading="saving"
          :disabled="creationUncertain || groupLoading || !!groupError || !can('system:user:edit')"
          @click="submitForm"
        >
          保存人员与权限
        </el-button>

        <el-button :disabled="saving" @click="cancel">取消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import '@/assets/styles/identity-access.scss'
import { canUse } from '@/utils/identityAccess'
import {
  listUser,
  getUser,
  delUser,
  resetUserPwd,
  changeUserStatus
} from '@/api/system/user'

import { getPersonnelAccess, createPersonnel, updatePersonnel } from '@/api/system/personnelAccess'

import { listGroup } from '@/api/system/group'

import { treeselect } from '@/api/system/dept'

import Treeselect from '@riophae/vue-treeselect'
import '@riophae/vue-treeselect/dist/vue-treeselect.css'

export default {
  name: 'PersonnelAccess',

  components: {
    Treeselect
  },

  data() {
    return {
      loading: true,
      saving: false,
      listError: '',
      saveError: '',
      groupError: '',
      groupLoading: false,
      statusBusyId: null,
      creationUncertain: false,

      ids: [],
      single: true,
      multiple: true,
      showSearch: true,

      total: 0,
      userList: [],

      title: '',
      open: false,

      deptName: undefined,
      deptOptions: [],

      initPassword: undefined,

      sexOptions: [],
      postOptions: [],
      roleOptions: [],
      groupOptions: [],
      originalGroupIds: [],

      runtimeAttr: null,

      form: {},

      defaultProps: {
        children: 'children',
        label: 'label'
      },

      queryParams: {
        pageNum: 1,
        pageSize: 10,
        userName: undefined,
        phonenumber: undefined,
        status: undefined,
        secretLevel: undefined,
        deptId: undefined
      },

      rules: {
        userName: [
          {
            required: true,
            message: '人员账号不能为空',
            trigger: 'blur'
          }
        ],

        nickName: [
          {
            required: true,
            message: '姓名不能为空',
            trigger: 'blur'
          }
        ],

        deptId: [
          {
            required: true,
            message: '归属部门不能为空',
            trigger: 'change'
          }
        ],

        password: [
          {
            required: true,
            message: '初始密码不能为空',
            trigger: 'blur'
          }
        ],

        accessStatus: [
          {
            required: true,
            type: 'enum',
            enum: ['ACTIVE', 'DISABLED', 'LOCKED'],
            message: '请选择有效的人员访问状态',
            trigger: 'change'
          }
        ],
        secretLevel: [
          {
            type: 'enum',
            enum: ['PUBLIC', 'INTERNAL', 'SECRET', 'CONFIDENTIAL'],
            message: '请选择有效的人员密级',
            trigger: 'change'
          },
          {
            required: true,
            message: '人员密级不能为空',
            trigger: 'change'
          }
        ]
      }
    }
  },

  computed: {
    selectedGroupNames() {
      const ids = (this.form.groupIds || []).map(String)
      return this.groupOptions
        .filter(
          (group) =>
            group.groupCode !== 'GROUP_PUBLIC' &&
            group.status === '0' &&
            ids.indexOf(String(group.id)) !== -1
        )
        .map((group) => group.groupName)
    },

    permissionPreview() {
      const rank = this.levelRank(this.form.secretLevel)

      const groups = this.selectedGroupNames

      const groupText = groups.length ? groups.join('、') : '尚未配置知悉用户组'

      return [
        {
          level: 'PUBLIC',
          name: '公开',
          allow: true,
          text: '不要求人员密级或知悉用户组；仍须通过请求准入检查'
        },

        {
          level: 'INTERNAL',
          name: '内部',
          allow: rank >= 2,
          text: rank >= 2 ? '密级条件满足；不要求知悉用户组' : '人员密级不足'
        },

        {
          level: 'SECRET',
          name: '秘密',
          allow: rank >= 3 && groups.length > 0,
          text:
            rank < 3
              ? '人员密级不足'
              : groups.length
                ? '密级及组条件满足；候选知悉范围：' + groupText
                : '密级满足，但没有国家秘密知悉范围'
        },

        {
          level: 'CONFIDENTIAL',
          name: '机密',
          allow: rank >= 4 && groups.length > 0,
          text:
            rank < 4
              ? '人员密级不足'
              : groups.length
                ? '密级及组条件满足；候选知悉范围：' + groupText
                : '密级满足，但没有国家秘密知悉范围'
        }
      ]
    }
  },

  watch: {
    deptName(val) {
      if (this.$refs.tree) {
        this.$refs.tree.filter(val)
      }
    }
  },

  created() {
    this.getList()
    this.getTreeselect()
    this.getGroupOptions()

    this.getDicts('sys_user_sex').then((response) => {
      this.sexOptions = response.data || []
    })

    this.getConfigKey('sys.user.initPassword').then((response) => {
      this.initPassword = response.msg
    })
  },

  methods: {
    can(permission) {
      return canUse(this.$store.getters.permissions, permission)
    },
    groupOptionLabel(group) {
      return (
        group.groupName +
        '（' +
        group.groupCode +
        (group.groupCode === 'GROUP_PUBLIC' ? '，历史兼容分组' : '') +
        (group.status === '1' ? '，已停用，不生效' : group.status !== '0' ? '，状态异常，不生效' : '') +
        '）'
      )
    },
    groupOptionDisabled(group) {
      if (group.groupCode === 'GROUP_PUBLIC') {
        return true
      }
      return group.status !== '0' && !this.originalGroupIds.some((id) => String(id) === String(group.id))
    },
    handleRowAction(command, row) {
      if (command === 'password' && this.can('system:user:resetPwd')) {
        this.handleResetPwd(row)
      }
      if (command === 'delete' && this.can('system:user:remove')) {
        this.handleDelete(row)
      }
    },
    closePersonnelDialog(done) {
      if (this.saving) {
        this.msgWarning('正在保存，请稍候。')
        return
      }
      done()
      this.reset()
    },

    async getList() {
      this.loading = true
      this.listError = ''
      const requestId = (this.userListRequestId || 0) + 1
      this.userListRequestId = requestId
      try {
        const response = await listUser(this.queryParams)
        if (requestId !== this.userListRequestId) {
          return
        }
        this.userList = Array.isArray(response.rows) ? response.rows : []
        this.total = Number(response.total) || 0
      } catch (error) {
        if (requestId !== this.userListRequestId) {
          return
        }
        this.userList = []
        this.total = 0
        this.listError = '人员列表加载失败，请重试。'
      } finally {
        if (requestId === this.userListRequestId) {
          this.loading = false
        }
      }
    },

    getTreeselect() {
      treeselect().then((response) => {
        this.deptOptions = response.data || []
      })
    },

    async getGroupOptions() {
      this.groupLoading = true
      this.groupError = ''
      try {
        const response = await listGroup({ pageNum: 1, pageSize: 1000 })
        const rows = Array.isArray(response.rows) ? response.rows : []
        this.groupOptions = rows.filter((group) => group.delFlag === '0')
        if (Number(response.total) > rows.length) {
          this.groupError = '用户组列表未完整加载，暂不能安全修改成员关系。'
        }
      } catch (error) {
        this.groupError = '用户组列表加载失败，权限保存暂不可用。'
      } finally {
        this.groupLoading = false
      }
    },

    filterNode(value, data) {
      if (!value) {
        return true
      }

      return data.label.indexOf(value) !== -1
    },

    handleNodeClick(data) {
      this.queryParams.deptId = data.id
      this.queryParams.pageNum = 1
      this.getList()
    },

    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },

    resetQuery() {
      this.resetForm('queryForm')
      this.queryParams.deptId = undefined

      if (this.$refs.tree) {
        this.$refs.tree.setCurrentKey(null)
      }

      this.handleQuery()
    },

    reset() {
      this.form = {
        userId: undefined,
        deptId: undefined,

        userName: undefined,
        nickName: undefined,
        password: undefined,

        employeeNo: undefined,
        sex: '2',
        ethnicity: undefined,
        idCardNo: undefined,
        birthPlace: undefined,
        jobTitle: undefined,

        phonenumber: undefined,
        email: undefined,

        status: '0',

        postIds: [],
        roleIds: [],

        secretLevel: 'PUBLIC',
        groupIds: [],

        accessStatus: 'ACTIVE',
        accessStartTime: null,
        accessEndTime: null,

        remark: undefined
      }

      this.runtimeAttr = null
      this.originalGroupIds = []
      this.saveError = ''
      this.creationUncertain = false

      if (this.$refs.form) {
        this.resetForm('form')
      }
    },

    handleAdd() {
      this.reset()
      this.getTreeselect()

      Promise.all([getUser(), this.getGroupOptions()])
        .then(([response]) => {
          this.postOptions = response.posts || []
          this.roleOptions = response.roles || []

          this.form.password = this.initPassword

          this.title = '新增人员及权限配置'
          this.open = true
        })
        .catch(() => {
          this.$message.error('人员初始化信息加载失败，请重试。')
        })
    },

    handleUpdate(row) {
      this.reset()
      this.getTreeselect()

      const userId = row && row.userId ? row.userId : this.ids[0]

      Promise.all([getUser(userId), getPersonnelAccess(userId), this.getGroupOptions()])
        .then(([userResponse, accessResponse]) => {
          const user = userResponse.data || {}

          const access = accessResponse.data || {}

          const attr = access.securityAttr || {}
          this.originalGroupIds = (access.groupIds || []).slice()
          // 以人员配置回读的组状态校正已选项，避免把停用归属当成有效知悉范围。
          const assignedGroups = access.groups || []
          assignedGroups.forEach((group) => {
            const option = {
              id: group.groupId,
              groupCode: group.groupCode,
              groupName: group.groupName,
              status: group.groupStatus,
              delFlag: '0'
            }
            const index = this.groupOptions.findIndex((item) => String(item.id) === String(option.id))
            if (index === -1) {
              this.groupOptions.push(option)
            } else {
              this.$set(this.groupOptions, index, option)
            }
          })

          this.form = Object.assign({}, this.form, user, {
            postIds: userResponse.postIds || [],
            roleIds: userResponse.roleIds || [],

            secretLevel: attr.secretLevel || null,

            groupIds: access.groupIds || [],

            accessStatus: attr.accessStatus || null,

            accessStartTime: attr.accessStartTime || null,

            accessEndTime: attr.accessEndTime || null
          })

          this.runtimeAttr = attr

          this.postOptions = userResponse.posts || []

          this.roleOptions = userResponse.roles || []

          this.form.password = undefined

          this.title = '人员信息与权限配置'

          this.open = true
        })
        .catch(() => {
          this.$message.error('人员资料或权限加载失败，请重试。')
        })
    },

    cancel() {
      if (this.saving) {
        return
      }
      this.open = false
      this.reset()
    },

    buildUserPayload() {
      return {
        userId: this.form.userId,
        deptId: this.form.deptId,

        userName: this.form.userName,
        nickName: this.form.nickName,

        password: this.form.password,

        email: this.form.email,
        phonenumber: this.form.phonenumber,
        sex: this.form.sex,

        ethnicity: this.form.ethnicity,
        idCardNo: this.form.idCardNo,
        birthPlace: this.form.birthPlace,
        jobTitle: this.form.jobTitle,
        employeeNo: this.form.employeeNo,

        status: this.form.status,

        postIds: this.form.postIds || [],
        roleIds: this.form.roleIds || [],

        remark: this.form.remark
      }
    },

    buildAccessPayload() {
      return {
        userName: this.form.userName,
        nickName: this.form.nickName,

        secretLevel: this.form.secretLevel,

        groupIds: this.form.groupIds || [],

        accessStatus: this.form.accessStatus,

        accessStartTime: this.form.accessStartTime || null,

        accessEndTime: this.form.accessEndTime || null
      }
    },

    submitForm() {
      if (this.creationUncertain || this.saving || this.groupLoading || this.groupError) {
        return
      }
      if (!this.can('system:user:edit') || (!this.form.userId && !this.can('system:user:add'))) {
        this.msgWarning('保存人员及权限需要对应的人员新增或编辑权限。')
        return
      }
      this.$refs.form.validate(async(valid) => {
        if (!valid || this.saving) {
          return
        }
        if (!!this.form.accessStartTime !== !!this.form.accessEndTime) {
          this.msgWarning('访问时间请同时填写开始和结束，或同时留空。')
          return
        }
        this.saving = true
        this.saveError = ''
        const creating = !this.form.userId
        try {
          const payload = { user: this.buildUserPayload(), access: this.buildAccessPayload() }
          const response = creating
            ? await createPersonnel(payload)
            : await updatePersonnel(this.form.userId, payload)
          if (Number(response.code) !== 200) {
            if (creating && response.saveStatus !== 'REJECTED') {
              this.creationUncertain = true
              this.saveError = '新增结果未能确认，请关闭窗口并刷新列表核实后再操作。'
              this.getList()
            } else {
              this.saveError = response.msg || '保存未成功，请核对信息后重试。'
            }
            return
          }
          if (creating) {
            if (!response.data || !/^\d+$/.test(String(response.data)) || Number(response.data) <= 0) {
              this.creationUncertain = true
              this.saveError = '新增请求已返回，但人员编号未能确认。请刷新列表核实，勿重复新增。'
              this.getList()
              return
            }
            this.form.userId = response.data
            this.form.password = undefined
          }
          this.msgSuccess('人员资料和权限均已保存')
          this.open = false
          this.getList()
        } catch (error) {
          if (creating) {
            this.creationUncertain = true
            this.saveError = '新增结果未能确认，请关闭窗口并刷新列表核实后再操作。'
            this.getList()
          } else {
            this.saveError = '保存结果未能确认，请刷新列表并重新打开人员配置核对后再操作。'
          }
        } finally {
          this.saving = false
        }
      })
    },

    handleSelectionChange(selection) {
      this.ids = selection.map((item) => item.userId)

      this.single = selection.length !== 1

      this.multiple = !selection.length
    },

    handleDelete(row) {
      const userIds = row && row.userId ? row.userId : this.ids

      this.$confirm('确认删除选中的人员吗？', '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
        .then(() => {
          return delUser(userIds)
        })
        .then(() => {
          this.msgSuccess('删除成功')
          this.getList()
        })
        .catch(() => {})
    },

    async handleStatusChange(row) {
      const next = row.status
      const previous = next === '0' ? '1' : '0'
      if (!this.can('system:user:edit')) {
        this.$set(row, 'status', previous)
        return
      }
      this.statusBusyId = row.userId
      try {
        await this.$confirm(
          '确认' +
            (next === '0' ? '启用' : '停用') +
            '账号“' +
            row.userName +
            '”吗？该操作会影响登录。',
          '账号状态变更'
        )
        await changeUserStatus(row.userId, next)
        this.msgSuccess('账号状态已更新')
      } catch (error) {
        this.$set(row, 'status', previous)
      } finally {
        this.statusBusyId = null
      }
    },

    handleResetPwd(row) {
      this.$prompt('请输入“' + row.userName + '”的新密码', '重置密码', {
        confirmButtonText: '确定',
        cancelButtonText: '取消'
      })
        .then(({ value }) => {
          return resetUserPwd(row.userId, value)
        })
        .then(() => {
          this.msgSuccess('密码重置成功')
        })
        .catch(() => {})
    },

    levelRank(level) {
      const map = {
        PUBLIC: 1,
        INTERNAL: 2,
        SECRET: 3,
        CONFIDENTIAL: 4
      }

      return map[level] || 0
    },

    secretText(level) {
      const map = {
        PUBLIC: '公开',
        INTERNAL: '内部',
        SECRET: '秘密',
        CONFIDENTIAL: '机密'
      }

      return map[level] || '未配置'
    },

    secretType(level) {
      const map = {
        PUBLIC: 'success',
        INTERNAL: 'info',
        SECRET: 'warning',
        CONFIDENTIAL: 'danger'
      }

      return map[level] || 'info'
    }
  }
}
</script>

<style scoped>
.personnel-page {
  padding: 20px;
}

.org-panel {
  min-height: 620px;
  padding: 15px;
  border: 1px solid #ebeef5;
  border-radius: 5px;
  background: #fff;
}

.panel-title {
  margin-bottom: 14px;
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.dept-search {
  margin-bottom: 15px;
}

.dialog-alert {
  margin-bottom: 20px;
}

.section-title {
  margin: 8px 0 18px;
  padding-left: 10px;
  border-left: 3px solid #409eff;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.permission-title {
  margin-top: 6px;
}

.field-help {
  margin-top: 5px;
  color: #909399;
  font-size: 12px;
}

.permission-preview {
  margin: 0 0 24px 105px;
  padding: 14px 18px;
  border: 1px solid #ebeef5;
  border-radius: 5px;
  background: #fafafa;
}

.preview-header {
  margin-bottom: 10px;
  font-weight: 600;
  color: #303133;
}

.preview-row {
  display: flex;
  align-items: center;
  min-height: 34px;
}

.preview-level {
  width: 90px;
}

.preview-result {
  color: #606266;
}

.allow-icon {
  margin-right: 6px;
  color: #67c23a;
}

.deny-icon {
  margin-right: 6px;
  color: #f56c6c;
}

.runtime-box {
  margin: 0 0 20px 105px;
  padding: 14px 18px;
  border: 1px dashed #dcdfe6;
  border-radius: 5px;
  color: #606266;
  background: #fafafa;
  font-size: 13px;
}

.runtime-title {
  margin-bottom: 12px;
  font-weight: 600;
  color: #303133;
}

.advanced-collapse {
  margin-bottom: 18px;
}

.advanced-alert {
  margin-bottom: 18px;
}

.empty-text {
  color: #909399;
}

.danger-text {
  color: #f56c6c !important;
}
</style>
