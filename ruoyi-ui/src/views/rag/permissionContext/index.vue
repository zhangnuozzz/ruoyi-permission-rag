<template>
  <div class="app-container permission-context-page">
    <el-card shadow="never" class="box-card">
      <div slot="header" class="card-header">
        <div>
          <div class="page-title">RAG 权限上下文</div>
          <div class="page-subtitle">
            展示当前登录用户在 RAG 检索前的权限画像，包括所属用户组、可访问标签、命中策略与访问判断结果。后续真实检索接口接入后，该上下文将作为检索请求的权限输入。
          </div>
        </div>
        <el-button type="primary" size="mini" icon="el-icon-refresh" :loading="loading" @click="loadContext">
          刷新上下文
        </el-button>
      </div>

      <el-alert
        title="链路定位：当前用户 → 用户组关系 → 策略绑定 → 权限上下文 → 后续传入 fufu RAG 检索接口"
        type="info"
        :closable="false"
        show-icon
        class="tips-alert"
      />

      <el-row :gutter="12" class="summary-row">
        <el-col :span="6">
          <div class="summary-card">
            <div class="summary-label">当前用户</div>
            <div class="summary-value">{{ context.userName || '-' }}</div>
            <div class="summary-extra">用户ID：{{ context.userId || '-' }}</div>
          </div>
        </el-col>

        <el-col :span="6">
          <div class="summary-card">
            <div class="summary-label">管理员身份</div>
            <div class="summary-value">
              <el-tag :type="context.admin ? 'success' : 'info'" size="small">
                {{ context.admin ? '管理员' : '普通用户' }}
              </el-tag>
            </div>
            <div class="summary-extra">admin = {{ context.admin }}</div>
          </div>
        </el-col>

        <el-col :span="6">
          <div class="summary-card">
            <div class="summary-label">访问判断</div>
            <div class="summary-value">
              <el-tag :type="context.allowAccess ? 'success' : 'danger'" size="small">
                {{ context.allowAccess ? '允许访问' : '拒绝访问' }}
              </el-tag>
            </div>
            <div class="summary-extra">requestTime：{{ context.requestTime || '-' }}</div>
          </div>
        </el-col>

        <el-col :span="6">
          <div class="summary-card">
            <div class="summary-label">权限范围数量</div>
            <div class="summary-value">
              {{ safeList(context.scopeCodes).length }} 个 scopeCode
            </div>
            <div class="summary-extra">{{ safeList(context.groupCodes).length }} 个用户组编码</div>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <el-row :gutter="16" class="content-row">
      <el-col :span="12">
        <el-card shadow="never" class="result-card">
          <div slot="header" class="table-header">
            <span>所属用户组</span>
            <el-tag size="mini" type="info">sys_group / sys_user_group_rel</el-tag>
          </div>

          <el-table v-loading="loading" :data="safeList(context.groups)" border stripe>
            <el-table-column label="组ID" prop="groupId" width="80" align="center" />
            <el-table-column label="用户组编码" prop="groupCode" min-width="150" show-overflow-tooltip>
              <template slot-scope="scope">
                <span class="mono-text">{{ scope.row.groupCode }}</span>
              </template>
            </el-table-column>
            <el-table-column label="用户组名称" prop="groupName" min-width="130" show-overflow-tooltip />
            <el-table-column label="知悉范围" prop="scopeCode" width="120">
              <template slot-scope="scope">
                <el-tag size="mini" type="info">{{ scope.row.scopeCode }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="状态" prop="status" width="90" align="center">
              <template slot-scope="scope">
                <el-tag size="mini" :type="scope.row.status === '0' ? 'success' : 'danger'">
                  {{ scope.row.status === '0' ? '正常' : '停用' }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card shadow="never" class="result-card">
          <div slot="header" class="table-header">
            <span>可访问权限标签</span>
            <el-tag size="mini" type="info">scopeCodes</el-tag>
          </div>

          <div class="tag-area">
            <template v-if="safeList(context.scopeCodes).length">
              <el-tag
                v-for="item in safeList(context.scopeCodes)"
                :key="item"
                type="success"
                effect="plain"
                class="scope-tag"
              >
                {{ item }}
              </el-tag>
            </template>
            <el-empty v-else description="暂无可访问权限标签" :image-size="80" />
          </div>

          <el-divider />

          <div class="section-title">用户组编码</div>
          <div class="tag-area small">
            <template v-if="safeList(context.groupCodes).length">
              <el-tag
                v-for="item in safeList(context.groupCodes)"
                :key="item"
                type="info"
                effect="plain"
                class="scope-tag"
              >
                {{ item }}
              </el-tag>
            </template>
            <span v-else class="empty-text">暂无用户组编码</span>
          </div>

          <el-divider />

          <div class="section-title">拒绝原因</div>
          <div class="tag-area small">
            <template v-if="safeList(context.denyReasons).length">
              <el-tag
                v-for="item in safeList(context.denyReasons)"
                :key="item"
                type="danger"
                effect="plain"
                class="scope-tag"
              >
                {{ item }}
              </el-tag>
            </template>
            <span v-else class="empty-text">当前无拒绝原因</span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="result-card">
      <div slot="header" class="table-header">
        <span>命中策略</span>
        <el-tag size="mini" type="info">sys_policy / sys_policy_bind</el-tag>
      </div>

      <el-table v-loading="loading" :data="safeList(context.policies)" border stripe>
        <el-table-column label="策略ID" prop="policyId" width="90" align="center" />
        <el-table-column label="策略编码" prop="policyCode" min-width="190" show-overflow-tooltip>
          <template slot-scope="scope">
            <span class="mono-text">{{ scope.row.policyCode }}</span>
          </template>
        </el-table-column>
        <el-table-column label="策略名称" prop="policyName" min-width="180" show-overflow-tooltip />
        <el-table-column label="效果" prop="effect" width="90" align="center">
          <template slot-scope="scope">
            <el-tag size="mini" :type="scope.row.effect === '0' ? 'success' : 'danger'">
              {{ scope.row.effect === '0' ? '允许' : '拒绝' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="主体类型" prop="subjectType" width="110" />
        <el-table-column label="主体表达式" prop="subjectExpr" min-width="220" show-overflow-tooltip />
        <el-table-column label="资源表达式" prop="resourceExpr" min-width="180" show-overflow-tooltip />
        <el-table-column label="环境表达式" prop="envExpr" min-width="200" show-overflow-tooltip />
        <el-table-column label="优先级" prop="priority" width="90" align="center" />
      </el-table>
    </el-card>

    <el-card shadow="never" class="result-card">
      <div slot="header" class="table-header">
        <span>后续传给 fufu RAG 检索接口的上下文 JSON</span>
        <el-button size="mini" type="primary" plain @click="copyJson">复制 JSON</el-button>
      </div>

      <pre class="json-box">{{ formatJson(context) }}</pre>
    </el-card>
  </div>
</template>

<script>
import { getCurrentPermissionContext } from '@/api/rag/permissionContext'

export default {
  name: 'RagPermissionContext',
  data() {
    return {
      loading: false,
      context: {}
    }
  },
  created() {
    this.loadContext()
  },
  methods: {
    loadContext() {
      this.loading = true
      getCurrentPermissionContext().then(response => {
        this.context = response.data || response || {}
      }).finally(() => {
        this.loading = false
      })
    },
    safeList(value) {
      return Array.isArray(value) ? value : []
    },
    formatJson(value) {
      try {
        return JSON.stringify(value || {}, null, 2)
      } catch (e) {
        return String(value)
      }
    },
    copyJson() {
      const text = this.formatJson(this.context)
      if (navigator.clipboard) {
        navigator.clipboard.writeText(text).then(() => {
          this.$modal.msgSuccess('权限上下文 JSON 已复制')
        })
      } else {
        this.$modal.msgWarning('当前浏览器不支持自动复制，请手动复制')
      }
    }
  }
}
</script>

<style scoped>
.permission-context-page {
  padding: 20px;
}

.box-card,
.result-card {
  border-radius: 6px;
}

.card-header,
.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
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

.summary-row {
  margin-top: 14px;
}

.summary-card {
  padding: 14px;
  background: #f8fafc;
  border: 1px solid #ebeef5;
  border-radius: 6px;
}

.summary-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 8px;
}

.summary-value {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}

.summary-extra {
  font-size: 12px;
  color: #909399;
}

.content-row {
  margin-top: 16px;
}

.result-card {
  margin-top: 16px;
}

.tag-area {
  min-height: 92px;
  padding: 8px 0;
}

.tag-area.small {
  min-height: 32px;
}

.scope-tag {
  margin-right: 8px;
  margin-bottom: 8px;
}

.section-title {
  font-size: 13px;
  font-weight: 600;
  color: #606266;
  margin-bottom: 8px;
}

.empty-text {
  color: #c0c4cc;
  font-size: 13px;
}

.mono-text {
  font-family: Menlo, Monaco, Consolas, "Courier New", monospace;
  font-size: 12px;
}

.json-box {
  margin: 0;
  padding: 12px;
  max-height: 360px;
  overflow: auto;
  background: #f5f7fa;
  border-radius: 4px;
  color: #606266;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
}

::v-deep .el-table .cell {
  white-space: nowrap;
}
</style>
