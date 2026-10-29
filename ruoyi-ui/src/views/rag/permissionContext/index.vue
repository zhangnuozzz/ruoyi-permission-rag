<template>
  <main class="permission-page identity-access-page" aria-label="当前人员权限上下文">
    <header class="page-header">
      <div>
        <h1>权限上下文</h1>
        <p>查看当前人员的身份、密级和知悉用户组，了解文档访问条件。</p>
      </div>
      <div class="header-actions">
        <span class="updated-at">{{ loadedAt ? '本次加载 ' + loadedAt : '等待加载' }}</span>
        <el-button
          type="primary"
          size="small"
          icon="el-icon-refresh"
          :loading="loading"
          @click="loadContext"
        >
          刷新数据
        </el-button>
      </div>
    </header>

    <div class="boundary-note" role="note">
      <i class="el-icon-info" aria-hidden="true" />
      <span>密级决定能接触的等级，用户组限定知悉范围；每次访问还须通过状态、时间和策略校验。</span>
    </div>

    <div v-if="contextError" class="error-panel" role="alert">
      <i class="el-icon-warning-outline" aria-hidden="true" />
      <div>
        <strong>权限上下文加载失败</strong>
        <p>{{ contextError }}</p>
      </div>
      <el-button size="small" :loading="loading" @click="loadContext">重新加载</el-button>
    </div>
    <div v-else-if="securityError" class="partial-warning" role="status">
      {{ securityError }} 其余已加载信息可继续查看，密级资格暂不作判断。
    </div>

    <section
      v-loading="loading"
      class="summary-grid"
      :aria-busy="loading ? 'true' : 'false'"
      aria-label="当前人员权限摘要"
    >
      <article class="summary-card">
        <span class="summary-label">当前人员</span>
        <strong>{{ contextLoaded ? context.userName || '未提供姓名' : '—' }}</strong>
        <span class="summary-detail">
          {{ contextLoaded ? '用户 ID：' + displayId(context.userId) : '身份信息待加载' }}
        </span>
        <el-tag v-if="contextLoaded" size="mini" :type="context.admin ? 'info' : ''">
          {{ context.admin ? '平台管理员' : '业务用户' }}
        </el-tag>
      </article>
      <article class="summary-card">
        <span class="summary-label">当前人员密级</span>
        <strong :class="{ 'unknown-value': !levelKnown }">
          {{ levelKnown ? levelName(userLevel) : '待确认' }}
        </strong>
        <span class="summary-detail">{{ levelKnown ? userLevel : '尚未取得有效的人员密级' }}</span>
        <span class="summary-footnote">管理员身份不代表拥有全部文档权限</span>
      </article>
      <article class="summary-card">
        <span class="summary-label">知悉用户组</span>
        <strong>
          {{ contextLoaded ? knowledgeGroups.length : '—' }}
          <small v-if="contextLoaded">个</small>
        </strong>
        <span class="summary-detail">所属知悉范围</span>
        <span class="summary-footnote">
          {{
            contextLoaded
              ? '另有 ' + compatibilityGroups.length + ' 个公开资料分组'
              : '分组信息待加载'
          }}
        </span>
      </article>
      <article class="summary-card">
        <span class="summary-label">附加策略摘要</span>
        <strong class="status-value" :class="policyStatusClass">{{ policyStatusText }}</strong>
        <span class="summary-detail">
          {{ contextLoaded ? '已关联 ' + policies.length + ' 条策略' : '策略信息待加载' }}
        </span>
      </article>
    </section>

    <section class="runtime-strip" aria-label="人员访问状态与时间配置">
      <div>
        <span>人员访问状态</span>
        <strong>{{ accessStatusText }}</strong>
      </div>
      <div>
        <span>人员访问时间窗口</span>
        <strong>{{ accessWindowText }}</strong>
      </div>
      <div>
        <span>权限更新时间</span>
        <strong>{{ contextLoaded ? context.requestTime || '未提供' : '待加载' }}</strong>
      </div>
    </section>

    <div class="detail-grid">
      <section class="panel" aria-labelledby="knowledge-groups-title">
        <div class="panel-heading">
          <div>
            <h2 id="knowledge-groups-title">当前知悉用户组</h2>
            <p>用户组只代表知悉范围，不具有密级，也不会提高人员密级。</p>
          </div>
          <span class="count-badge">{{ contextLoaded ? knowledgeGroups.length : '—' }}</span>
        </div>
        <el-table
          v-if="contextLoaded"
          :data="knowledgeGroups"
          size="small"
          empty-text="当前没有知悉用户组；公开文档和内部文档仍按各自规则判断。"
        >
          <el-table-column
            label="用户组名称"
            prop="groupName"
            min-width="160"
            show-overflow-tooltip
          />
          <el-table-column
            label="用户组编码"
            prop="groupCode"
            min-width="170"
            show-overflow-tooltip
          />
          <el-table-column label="状态" width="90">
            <template slot-scope="scope">
              <el-tag size="mini" :type="scope.row.status === '0' ? 'success' : 'info'">
                {{ scope.row.status === '0' ? '有效' : '待确认' }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
        <div v-else class="empty-state">
          <i class="el-icon-folder-opened" aria-hidden="true" />
          <span>用户组信息尚未加载</span>
        </div>
      </section>
      <section class="panel" aria-labelledby="decision-details-title">
        <div class="panel-heading">
          <div>
            <h2 id="decision-details-title">校验说明</h2>
            <p>区分后台管理身份、文档访问资格和运行时限制。</p>
          </div>
        </div>
        <ol class="explanation-list">
          <li>
            <strong>人员密级独立维护</strong>
            <span>内部及更高密级的文档，需要人员密级达到相应等级。</span>
          </li>
          <li>
            <strong>知悉范围通过用户组匹配</strong>
            <span>涉密文档绑定的唯一用户组，命中人员所属组集合中的任意一组即可满足范围条件。</span>
          </li>
          <li>
            <strong>实际请求逐次校验</strong>
            <span>人员或文档停用、访问时间限制、风险控制和附加策略仍可能阻止本次访问。</span>
          </li>
        </ol>
        <div class="restriction-box" aria-live="polite">
          <strong>附加策略摘要中的拒绝原因</strong>
          <ul v-if="denyReasons.length">
            <li v-for="reason in denyReasons" :key="reason">{{ reasonName(reason) }}</li>
          </ul>
          <p v-else>
            {{
              contextLoaded
                ? '本次上下文未返回拒绝原因；实际请求仍须校验。'
                : '等待权限上下文加载。'
            }}
          </p>
        </div>
      </section>
    </div>

    <section class="panel" aria-labelledby="associated-policies-title">
      <div class="panel-heading">
        <div>
          <h2 id="associated-policies-title">已关联策略</h2>
          <p>显示当前上下文加载的策略配置，不代表每条表达式已命中或已执行。</p>
        </div>
        <span class="count-badge">{{ contextLoaded ? policies.length : '—' }}</span>
      </div>
      <el-table
        v-if="contextLoaded"
        :data="policies"
        size="small"
        empty-text="当前未关联策略；基础文档访问资格仍由人员密级与知悉用户组决定。"
      >
        <el-table-column label="策略名称" prop="policyName" min-width="180" show-overflow-tooltip />
        <el-table-column label="策略编码" prop="policyCode" min-width="170" show-overflow-tooltip />
        <el-table-column label="配置效果" width="110">
          <template slot-scope="scope">
            <el-tag size="mini" :type="scope.row.effect === '1' ? 'warning' : 'info'">
              {{ effectName(scope.row.effect) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="环境条件" prop="envExpr" min-width="220" show-overflow-tooltip />
        <el-table-column label="优先级" prop="priority" width="90" align="center" />
      </el-table>
      <div v-else class="empty-state"><span>策略信息尚未加载</span></div>
    </section>

    <el-collapse class="context-rule-collapse">
      <el-collapse-item title="查看公开、内部、秘密和机密文档的访问规则" name="rules">
        <section class="panel rules-panel" aria-labelledby="access-rules-title">
          <div class="panel-heading">
            <div>
              <h2 id="access-rules-title">文档访问规则</h2>
              <p>密级资格与知悉范围分别判断，满足对应条件后仍须通过附加安全校验。</p>
            </div>
            <el-tag size="mini" type="info">访问条件</el-tag>
          </div>
          <div class="rule-table-wrap">
            <table class="rule-table">
              <thead>
                <tr>
                  <th scope="col">文档类别</th>
                  <th scope="col">人员密级要求</th>
                  <th scope="col">知悉用户组要求</th>
                  <th scope="col">当前密级资格</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="rule in rules" :key="rule.code">
                  <th scope="row">
                    <span class="document-name">{{ rule.name }}</span>
                    <span class="document-code">{{ rule.code }}</span>
                  </th>
                  <td>{{ rule.levelRequirement }}</td>
                  <td>{{ rule.groupRequirement }}</td>
                  <td>
                    <span class="qualification" :class="qualificationClass(rule)">
                      {{ qualificationText(rule) }}
                    </span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <p class="panel-note">
            公开文档不要求人员密级或用户组。秘密、机密文档各绑定唯一知悉用户组，该组必须属于人员的有效用户组集合。此处不表示开放匿名访问。
          </p>
        </section>
      </el-collapse-item>
    </el-collapse>

    <el-collapse v-model="expandedDetails" class="technical-details">
      <el-collapse-item title="查看权限上下文原始数据" name="context">
        <div class="technical-toolbar">
          <span>供授权管理员排查数据使用</span>
          <el-button size="mini" :disabled="!contextLoaded || loading" @click="copyJson">
            复制 JSON
          </el-button>
        </div>
        <pre class="json-box">{{ contextLoaded ? formatJson(context) : '尚无可展示的数据' }}</pre>
      </el-collapse-item>
    </el-collapse>
  </main>
</template>

<script>
import '@/assets/styles/identity-access.scss'
import { getCurrentPermissionContext } from '@/api/rag/permissionContext'

const LEVEL_RANK = { PUBLIC: 1, INTERNAL: 2, SECRET: 3, CONFIDENTIAL: 4 }

export default {
  name: 'RagPermissionContext',
  data() {
    return {
      loading: false,
      context: null,
      contextError: '',
      securityError: '',
      loadedAt: '',
      expandedDetails: [],
      rules: [
        {
          code: 'PUBLIC',
          name: '公开非密',
          levelRequirement: '不要求人员密级',
          groupRequirement: '不要求用户组'
        },
        {
          code: 'INTERNAL',
          name: '内部工作秘密',
          levelRequirement: '人员密级 ≥ 内部',
          groupRequirement: '不要求知悉用户组'
        },
        {
          code: 'SECRET',
          name: '国家秘密 · 秘密',
          levelRequirement: '人员密级 ≥ 秘密',
          groupRequirement: '文档唯一绑定组属于人员组集合'
        },
        {
          code: 'CONFIDENTIAL',
          name: '国家秘密 · 机密',
          levelRequirement: '人员密级 ≥ 机密',
          groupRequirement: '文档唯一绑定组属于人员组集合'
        }
      ]
    }
  },
  computed: {
    contextLoaded() {
      return this.context !== null
    },
    groups() {
      const seen = {}
      return this.safeList(this.context && this.context.groups).filter((group) => {
        if (!group || !group.groupCode || seen[group.groupCode] || group.status !== '0') {
          return false
        }
        seen[group.groupCode] = true
        return true
      })
    },
    knowledgeGroups() {
      return this.groups.filter((group) => group.groupCode !== 'GROUP_PUBLIC')
    },
    compatibilityGroups() {
      return this.groups.filter((group) => group.groupCode === 'GROUP_PUBLIC')
    },
    policies() {
      return this.safeList(this.context && this.context.policies).filter(
        (item) => item && typeof item === 'object'
      )
    },
    denyReasons() {
      return this.safeList(this.context && this.context.denyReasons).filter(
        (item) => typeof item === 'string' && item.length > 0
      )
    },
    userLevel() {
      const value =
        this.contextLoaded && this.context.securityAttributesPresent === true
          ? this.context.userSecretLevel
          : null
      return typeof value === 'string' ? value.trim().toUpperCase() : ''
    },
    levelKnown() {
      return Object.prototype.hasOwnProperty.call(LEVEL_RANK, this.userLevel)
    },
    accessStatusText() {
      if (!this.contextLoaded || this.context.securityAttributesPresent !== true) {
        return '待确认'
      }
      return (
        { ACTIVE: '已启用', DISABLED: '已停用', LOCKED: '已锁定' }[this.context.userAccessStatus] ||
        '未知状态，需核对'
      )
    },
    accessWindowText() {
      if (!this.contextLoaded || this.context.securityAttributesPresent !== true) {
        return '待确认'
      }
      const start = this.context.accessStartTime
      const end = this.context.accessEndTime
      if (!start && !end) {
        return '未设置人员时间窗口'
      }
      if (!start || !end) {
        return '时间配置不完整，需核对'
      }
      return start + ' — ' + end + (start > end ? '（跨午夜）' : '')
    },
    policyStatusText() {
      if (!this.contextLoaded || typeof this.context.allowAccess !== 'boolean') {
        return '待确认'
      }
      return this.context.allowAccess ? '未报告策略拒绝' : '存在策略限制'
    },
    policyStatusClass() {
      if (!this.contextLoaded || typeof this.context.allowAccess !== 'boolean') {
        return 'status-unknown'
      }
      return this.context.allowAccess ? 'status-ready' : 'status-restricted'
    }
  },
  created() {
    this.loadContext()
  },
  methods: {
    async loadContext() {
      if (this.loading) {
        return
      }
      this.loading = true
      this.contextError = ''
      this.securityError = ''
      try {
        const response = await getCurrentPermissionContext()
        const value = response && response.code === 200 ? response.data : null
        if (!value || typeof value !== 'object' || Array.isArray(value) || value.userId == null) {
          this.context = null
          this.loadedAt = ''
          this.contextError = '未能取得当前人员的有效权限数据，请重试或联系管理员。'
          return
        }
        this.context = value
        if (!this.levelKnown) {
          this.securityError =
            value.securityAttributesPresent === false
              ? '尚未配置人员安全属性。'
              : '人员密级暂时无法确认。'
        }
        this.loadedAt = new Date().toLocaleTimeString('zh-CN', { hour12: false })
      } catch (error) {
        this.context = null
        this.loadedAt = ''
        this.contextError = '权限数据加载异常，请重新加载。'
      } finally {
        this.loading = false
      }
    },
    safeList(value) {
      return Array.isArray(value) ? value : []
    },
    displayId(value) {
      return value == null ? '未提供' : String(value)
    },
    levelName(code) {
      return (
        { PUBLIC: '公开', INTERNAL: '内部', SECRET: '秘密', CONFIDENTIAL: '机密' }[code] ||
        '未知密级'
      )
    },
    effectName(effect) {
      return effect === '0' ? '允许配置' : effect === '1' ? '拒绝配置' : '未知配置'
    },
    qualificationText(rule) {
      if (rule.code === 'PUBLIC') {
        return '无密级门槛'
      }
      if (!this.contextLoaded || !this.levelKnown) {
        return '待确认'
      }
      return LEVEL_RANK[this.userLevel] >= LEVEL_RANK[rule.code] ? '密级条件满足' : '人员密级不足'
    },
    qualificationClass(rule) {
      if (rule.code === 'PUBLIC') {
        return 'qualification-neutral'
      }
      if (!this.contextLoaded || !this.levelKnown) {
        return 'qualification-neutral'
      }
      return LEVEL_RANK[this.userLevel] >= LEVEL_RANK[rule.code]
        ? 'qualification-met'
        : 'qualification-unmet'
    },
    reasonName(reason) {
      return (
        {
          OUT_OF_WORKING_HOURS: '当前不在策略允许的工作时间内',
          OUT_OF_USER_ACCESS_TIME_WINDOW: '当前不在人员允许的访问时间内',
          USER_ACCESS_STATUS_NOT_ACTIVE: '人员访问状态未启用',
          USER_SECURITY_ATTR_NOT_FOUND: '尚未配置人员安全属性'
        }[reason] || reason
      )
    },
    formatJson(value) {
      return JSON.stringify(value || {}, null, 2)
    },
    async copyJson() {
      if (!this.contextLoaded || this.loading) {
        return
      }
      try {
        if (navigator.clipboard && window.isSecureContext) {
          await navigator.clipboard.writeText(this.formatJson(this.context))
        } else {
          const field = document.createElement('textarea')
          field.value = this.formatJson(this.context)
          field.style.position = 'fixed'
          field.style.opacity = '0'
          const previous = document.activeElement
          document.body.appendChild(field)
          try {
            field.select()
            if (!document.execCommand('copy')) {
              throw new Error('copy failed')
            }
          } finally {
            document.body.removeChild(field)
            if (previous && previous.focus) {
              previous.focus()
            }
          }
        }
        this.$message.success('权限上下文已复制')
      } catch (error) {
        this.$message.warning('复制失败，请在下方原始数据中手动选择并复制')
      }
    }
  }
}
</script>

<style scoped>
.permission-page {
  min-height: calc(100vh - 84px);
  padding: 24px;
  background: #f4f6f9;
  color: #243449;
}
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 20px;
}
.page-header h1 {
  margin: 0;
  font-size: 24px;
  line-height: 1.4;
  font-weight: 600;
  color: #1f3048;
}
.page-header p,
.panel-heading p {
  margin: 8px 0 0;
  font-size: 13px;
  line-height: 1.7;
  color: #606c7e;
}
.header-actions {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-shrink: 0;
}
.updated-at {
  color: #68788a;
  font-size: 12px;
  white-space: nowrap;
}
.boundary-note {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 14px 16px;
  border: 1px solid #d9e5f3;
  border-radius: 6px;
  background: #edf3fa;
  color: #405c7d;
  font-size: 13px;
  line-height: 1.7;
}
.boundary-note i {
  margin-top: 4px;
  color: #527ba7;
}
.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin: 18px 0;
}
.summary-card {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 9px;
  padding: 20px;
  background: #fff;
  border: 1px solid #e1e7ef;
  border-radius: 7px;
  min-height: 164px;
}
.summary-label {
  color: #596a7f;
  font-size: 13px;
  font-weight: 500;
}
.summary-card > strong {
  font-size: 25px;
  line-height: 1.4;
  color: #233f60;
  overflow-wrap: anywhere;
}
.summary-card > strong.status-value {
  font-size: 18px;
}
.summary-card small {
  font-size: 13px;
  font-weight: 400;
}
.summary-detail {
  font-size: 12px;
  color: #5c6c7e;
}
.summary-footnote {
  font-size: 12px;
  line-height: 1.5;
  color: #6a7789;
}
.summary-card > .unknown-value {
  color: #6a7789;
}
.summary-card > .status-restricted {
  color: #a94f19;
}
.summary-card > .status-unknown {
  color: #6a7789;
}
.panel {
  margin-bottom: 18px;
  background: #fff;
  border: 1px solid #e1e7ef;
  border-radius: 7px;
  overflow: hidden;
}
.runtime-strip {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  padding: 16px 20px;
  margin-bottom: 18px;
  border: 1px solid #e1e7ef;
  border-radius: 7px;
  background: #fff;
}
.runtime-strip > div {
  display: flex;
  flex-direction: column;
  gap: 7px;
}
.runtime-strip span {
  color: #647386;
  font-size: 12px;
}
.runtime-strip strong {
  color: #344c67;
  font-size: 13px;
  font-weight: 500;
  line-height: 1.6;
}
.panel-heading {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 14px;
  padding: 20px;
  border-bottom: 1px solid #edf0f4;
}
.panel-heading h2 {
  margin: 0;
  font-size: 16px;
  line-height: 1.5;
  color: #263f5a;
  font-weight: 600;
}
.panel-heading p {
  font-size: 12px;
  margin-top: 5px;
}
.rule-table-wrap {
  overflow-x: auto;
}
.rule-table {
  width: 100%;
  min-width: 740px;
  border-collapse: collapse;
  text-align: left;
  font-size: 13px;
}
.rule-table thead {
  background: #f7f9fc;
}
.rule-table th {
  font-weight: 500;
}
.rule-table thead th {
  padding: 13px 20px;
  color: #53657b;
  font-size: 12px;
}
.rule-table td,
.rule-table tbody th {
  padding: 16px 20px;
  border-top: 1px solid #edf0f4;
  line-height: 1.6;
  color: #4b5f76;
}
.document-name {
  display: block;
  font-weight: 500;
  color: #2d435e;
}
.document-code {
  display: block;
  margin-top: 3px;
  color: #6e7d8f;
  font-size: 11px;
}
.qualification {
  display: inline-block;
  padding: 3px 9px;
  border-radius: 4px;
  white-space: nowrap;
  font-size: 12px;
}
.qualification-neutral {
  color: #5d6b7b;
  background: #eff2f6;
}
.qualification-met {
  color: #276947;
  background: #edf6f0;
}
.qualification-unmet {
  color: #9a481b;
  background: #fff3e9;
}
.panel-note {
  margin: 0;
  padding: 13px 20px;
  background: #fafbfd;
  border-top: 1px solid #edf0f4;
  color: #647386;
  font-size: 12px;
  line-height: 1.8;
}
.detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(0, 1fr);
  gap: 18px;
}
.count-badge {
  padding: 3px 9px;
  border-radius: 4px;
  background: #edf2f8;
  color: #4c6c8e;
  font-size: 12px;
}
.explanation-list {
  margin: 0;
  padding: 20px 22px 16px 42px;
  color: #58718d;
}
.explanation-list li {
  margin-bottom: 15px;
  padding-left: 4px;
  font-size: 13px;
}
.explanation-list strong {
  display: block;
  margin-bottom: 5px;
  color: #344c67;
  font-weight: 500;
}
.explanation-list span {
  color: #647386;
  font-size: 12px;
  line-height: 1.8;
}
.restriction-box {
  margin: 0 20px 20px;
  padding: 13px 14px;
  border: 1px solid #e5eaf1;
  border-radius: 5px;
  background: #f8fafc;
  font-size: 12px;
}
.restriction-box > strong {
  font-weight: 500;
}
.restriction-box p {
  margin: 8px 0 0;
  color: #647386;
  line-height: 1.7;
}
.restriction-box ul {
  margin: 8px 0 0;
  padding-left: 18px;
  color: #a94f19;
  line-height: 1.8;
}
.empty-state {
  min-height: 148px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 12px;
  padding: 20px;
  color: #68788a;
  font-size: 13px;
  line-height: 1.8;
}
.empty-state i {
  font-size: 24px;
  color: #8ca0b6;
}
.error-panel {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-top: 16px;
  padding: 18px;
  border: 1px solid #efd5d2;
  border-radius: 6px;
  background: #fff5f3;
  color: #9e4238;
}
.error-panel > i {
  font-size: 24px;
}
.error-panel strong {
  font-size: 14px;
}
.error-panel p {
  margin: 7px 0 0;
  font-size: 12px;
  line-height: 1.6;
}
.error-panel .el-button {
  margin-left: auto;
}
.partial-warning {
  margin-top: 16px;
  padding: 12px 16px;
  background: #fff7eb;
  border: 1px solid #edd8b6;
  border-radius: 6px;
  color: #8b591e;
  font-size: 12px;
  line-height: 1.8;
}
.technical-details {
  padding: 0 20px;
  background: #fff;
  border: 1px solid #e1e7ef;
  border-radius: 7px;
}
.technical-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 12px;
  color: #647386;
  font-size: 12px;
}
.json-box {
  max-height: 340px;
  margin: 0;
  padding: 16px;
  overflow: auto;
  background: #f5f7fa;
  border: 1px solid #e5eaf1;
  border-radius: 5px;
  color: #4e6076;
  font: 12px/1.8 Menlo, Consolas, monospace;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}
::v-deep .el-table th {
  background: #f7f9fc;
  color: #53657b;
  font-weight: 500;
}
::v-deep .el-table td {
  color: #4b5f76;
}
::v-deep .el-table__empty-text {
  max-width: 88%;
  padding: 18px 0;
  line-height: 1.8;
}
::v-deep .el-button:focus-visible,
::v-deep .el-collapse-item__header:focus-visible {
  outline: 2px solid #245d9b;
  outline-offset: 3px;
}
@media (max-width: 1200px) {
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .detail-grid {
    grid-template-columns: 1fr;
    gap: 0;
  }
}
@media (max-width: 640px) {
  .permission-page {
    padding: 14px;
  }
  .page-header {
    align-items: flex-start;
    flex-direction: column;
    gap: 14px;
  }
  .page-header h1 {
    font-size: 21px;
  }
  .header-actions {
    width: 100%;
    justify-content: space-between;
  }
  .summary-grid {
    grid-template-columns: 1fr;
    gap: 10px;
  }
  .runtime-strip {
    grid-template-columns: 1fr;
  }
  .summary-card {
    min-height: 135px;
    padding: 16px;
  }
  .panel-heading {
    padding: 16px;
  }
  .panel-heading > .el-tag {
    display: none;
  }
  .error-panel {
    flex-wrap: wrap;
  }
  .technical-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
