<template>
  <div class="dashboard-container">
    <el-card shadow="never" class="hero-card">
      <div class="hero-content">
        <div>
          <div class="hero-title">权限控制 RAG 平台</div>
          <div class="hero-subtitle">
            面向大模型向量库场景，提供权限治理、RAG 文件入库、安全检索、二次过滤与审计追踪能力。
          </div>
        </div>
        <el-tag type="success" effect="dark">RuoYi-Vue 3.2.0</el-tag>
      </div>
    </el-card>

    <el-row :gutter="16" class="status-row">
      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="hover" class="status-card">
          <div class="status-title">权限治理</div>
          <div class="status-value">已完成</div>
          <div class="status-desc">用户组、策略、绑定、权限上下文</div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="hover" class="status-card">
          <div class="status-title">RAG 文件入库</div>
          <div class="status-value">已跑通</div>
          <div class="status-desc">MariaDB / MinIO / Milvus 三段式存储</div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="hover" class="status-card">
          <div class="status-title">安全检索 Mock</div>
          <div class="status-value">已闭环</div>
          <div class="status-desc">metadataFilter + 二次过滤 + 审计</div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="hover" class="status-card warning-card">
          <div class="status-title">真实检索联调</div>
          <div class="status-value warning">待对接</div>
          <div class="status-desc">等待 RAG Server /rag/search</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :xs="24" :md="16">
        <el-card shadow="never" class="panel-card">
          <div slot="header" class="panel-header">
            <span>核心安全检索链路</span>
            <el-tag size="mini" type="info">Platform Flow</el-tag>
          </div>

          <div class="flow">
            <div class="flow-item">用户登录</div>
            <div class="flow-arrow">→</div>
            <div class="flow-item">PermissionContext</div>
            <div class="flow-arrow">→</div>
            <div class="flow-item">Metadata Filter</div>
            <div class="flow-arrow">→</div>
            <div class="flow-item">RAG 候选结果</div>
            <div class="flow-arrow">→</div>
            <div class="flow-item">二次权限过滤</div>
            <div class="flow-arrow">→</div>
            <div class="flow-item">审计追踪</div>
          </div>

          <el-alert
            class="flow-alert"
            type="success"
            :closable="false"
            show-icon
            title="当前平台侧已完成权限上下文生成、策略决策、模拟候选结果过滤与检索审计。真实检索接口完成后，只需替换候选结果来源。"
          />

          <el-table :data="moduleTable" border stripe class="module-table">
            <el-table-column prop="module" label="模块" width="160" />
            <el-table-column prop="content" label="已完成能力" />
            <el-table-column prop="status" label="状态" width="120">
              <template slot-scope="scope">
                <el-tag :type="scope.row.type" size="mini">{{ scope.row.status }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <el-col :xs="24" :md="8">
        <el-card shadow="never" class="panel-card">
          <div slot="header" class="panel-header">
            <span>分支状态</span>
            <el-tag size="mini">Git</el-tag>
          </div>

          <div class="branch-item">
            <div class="branch-name">main</div>
            <div class="branch-desc">稳定展示分支：权限治理、文件入库、mock 安全检索闭环</div>
          </div>

          <div class="branch-item">
            <div class="branch-name">fufu_week3</div>
            <div class="branch-desc">RAG Server 入库分支：MariaDB / MinIO / Milvus 已完成，待补 /rag/search</div>
          </div>

          <div class="branch-item">
            <div class="branch-name">feature/platform-remote-search</div>
            <div class="branch-desc">平台侧远程检索适配分支：useRemote / topK / RemoteSearchService</div>
          </div>
        </el-card>

        <el-card shadow="never" class="panel-card todo-card">
          <div slot="header" class="panel-header">
            <span>下一步任务</span>
            <el-tag size="mini" type="warning">TODO</el-tag>
          </div>

          <el-timeline>
            <el-timeline-item timestamp="RAG Server" type="warning">
              fufu 补充 POST /rag/search 真实检索接口
            </el-timeline-item>
            <el-timeline-item timestamp="平台联调" type="primary">
              平台侧 useRemote=true 调用真实 RAG Server
            </el-timeline-item>
            <el-timeline-item timestamp="最终闭环" type="success">
              完成端到端权限控制 RAG 检索与审计
            </el-timeline-item>
          </el-timeline>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
export default {
  name: 'Index',
  data() {
    return {
      moduleTable: [
        {
          module: '权限治理',
          content: '用户组管理、用户组关系、权限策略、策略绑定、文档权限标签',
          status: '已完成',
          type: 'success'
        },
        {
          module: 'RAG 文件入库',
          content: '若依前端 → 若依后端 → RAG Server → MariaDB / MinIO / Milvus',
          status: '已跑通',
          type: 'success'
        },
        {
          module: '安全检索',
          content: 'PermissionContext、Metadata Filter、sys_rag_doc 模拟候选结果、二次过滤',
          status: '已闭环',
          type: 'success'
        },
        {
          module: '审计追踪',
          content: 'RAG 文件入库审计、RAG 检索审计、安全中心访问监控',
          status: '已完成',
          type: 'success'
        },
        {
          module: '真实检索',
          content: '等待 RAG Server 提供 /rag/search，随后进行 useRemote=true 联调',
          status: '待联调',
          type: 'warning'
        }
      ]
    }
  }
}
</script>

<style scoped>
.dashboard-container {
  padding: 20px;
  background: #f5f7fa;
  min-height: calc(100vh - 84px);
}

.hero-card {
  border-radius: 8px;
  margin-bottom: 16px;
}

.hero-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.hero-title {
  font-size: 24px;
  font-weight: 700;
  color: #303133;
  line-height: 34px;
}

.hero-subtitle {
  margin-top: 8px;
  font-size: 14px;
  color: #606266;
}

.status-row {
  margin-bottom: 16px;
}

.status-card {
  border-radius: 8px;
  margin-bottom: 16px;
}

.status-title {
  font-size: 13px;
  color: #909399;
  margin-bottom: 10px;
}

.status-value {
  font-size: 24px;
  font-weight: 700;
  color: #67c23a;
  margin-bottom: 8px;
}

.status-value.warning {
  color: #e6a23c;
}

.status-desc {
  font-size: 12px;
  color: #606266;
  line-height: 20px;
}

.panel-card {
  border-radius: 8px;
  margin-bottom: 16px;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
}

.flow {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  margin: 6px 0 18px;
}

.flow-item {
  padding: 10px 14px;
  background: #ecf5ff;
  border: 1px solid #b3d8ff;
  border-radius: 6px;
  color: #1f6fbf;
  font-weight: 600;
  margin: 6px 0;
}

.flow-arrow {
  margin: 0 8px;
  color: #909399;
  font-weight: 700;
}

.flow-alert {
  margin-bottom: 16px;
}

.module-table {
  margin-top: 10px;
}

.branch-item {
  padding: 12px 0;
  border-bottom: 1px solid #ebeef5;
}

.branch-item:last-child {
  border-bottom: none;
}

.branch-name {
  font-weight: 700;
  color: #303133;
  margin-bottom: 6px;
}

.branch-desc {
  font-size: 13px;
  color: #606266;
  line-height: 20px;
}

.todo-card {
  margin-top: 16px;
}
</style>
