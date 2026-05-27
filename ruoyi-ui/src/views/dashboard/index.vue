<template>
  <div class="dashboard-container">
    <el-card class="hero-card" shadow="never">
      <div class="hero-left">
        <div class="hero-title">权限控制 RAG 平台</div>
        <div class="hero-subtitle">
          面向大模型向量库场景，提供权限治理、RAG 文件入库、安全检索、二次过滤与审计追踪能力。
        </div>
      </div>
      <el-tag type="success" effect="dark">RuoYi-Vue 3.2.0</el-tag>
    </el-card>

    <el-row :gutter="16" class="status-row">
      <el-col :span="6">
        <el-card shadow="never" class="status-card">
          <div class="status-label">权限治理</div>
          <div class="status-value success">已完成</div>
          <div class="status-desc">用户组、策略、绑定、权限上下文</div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card shadow="never" class="status-card">
          <div class="status-label">RAG 文件入库</div>
          <div class="status-value success">已跑通</div>
          <div class="status-desc">MariaDB / MinIO / Milvus 三段式存储</div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card shadow="never" class="status-card">
          <div class="status-label">安全检索 Mock</div>
          <div class="status-value success">已闭环</div>
          <div class="status-desc">metadataFilter + 二次过滤 + 审计</div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card shadow="never" class="status-card">
          <div class="status-label">真实检索联调</div>
          <div class="status-value success">已联通</div>
          <div class="status-desc">useRemote=true 调用 RAG Server /rag/search</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :span="16">
        <el-card shadow="never" class="box-card">
          <div slot="header" class="section-header">
            <span class="section-title">核心安全检索链路</span>
            <el-tag size="mini" type="info">Platform Flow</el-tag>
          </div>

          <div class="flow-box">
            <div class="flow-node">用户登录</div>
            <div class="flow-arrow">→</div>
            <div class="flow-node">PermissionContext</div>
            <div class="flow-arrow">→</div>
            <div class="flow-node">Metadata Filter</div>
            <div class="flow-arrow">→</div>
            <div class="flow-node">RAG Server 真实检索</div>
            <div class="flow-arrow">→</div>
            <div class="flow-node">二次权限过滤</div>
            <div class="flow-arrow">→</div>
            <div class="flow-node">审计追踪</div>
          </div>

          <el-alert
            title="当前已完成真实检索第一轮联调：平台侧 useRemote=true 调用 RAG Server /rag/search，RAG Server 基于 Milvus 返回候选结果，平台侧继续执行权限二次过滤与审计留痕。"
            type="success"
            :closable="false"
            show-icon
            class="chain-alert"
          />

          <el-table :data="moduleList" border stripe class="module-table">
            <el-table-column prop="module" label="模块" width="160" />
            <el-table-column prop="ability" label="已完成能力" />
            <el-table-column prop="status" label="状态" width="120" align="center">
              <template slot-scope="scope">
                <el-tag :type="scope.row.type" size="small" effect="plain">
                  {{ scope.row.status }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card shadow="never" class="box-card">
          <div slot="header" class="section-header">
            <span class="section-title">分支状态</span>
            <el-tag size="mini">Git</el-tag>
          </div>

          <div class="branch-block">
            <div class="branch-name">main</div>
            <div class="branch-desc">稳定展示分支：权限治理、文件入库、Mock 安全检索闭环</div>
          </div>

          <div class="branch-block">
            <div class="branch-name">fufu_week4</div>
            <div class="branch-desc">RAG Server 侧：文件入库、Milvus 切块、真实 /rag/search 检索接口</div>
          </div>

          <div class="branch-block">
            <div class="branch-name">feature/platform-remote-search</div>
            <div class="branch-desc">平台侧真实检索联调分支：useRemote=true / topK / RemoteSearchService / 二次过滤 / 审计适配已完成</div>
          </div>
        </el-card>

        <el-card shadow="never" class="box-card todo-card">
          <div slot="header" class="section-header">
            <span class="section-title">下一步任务</span>
            <el-tag size="mini" type="warning">TODO</el-tag>
          </div>

          <el-timeline>
            <el-timeline-item color="#67C23A" timestamp="演示流程">
              固化“上传文件 → Milvus 查看 → 真实检索 → 审计日志”的完整演示链路
            </el-timeline-item>
            <el-timeline-item color="#409EFF" timestamp="权限边界">
              补充 INTERNAL / DOC_ADMIN / 非授权用户等权限过滤边界测试
            </el-timeline-item>
            <el-timeline-item color="#E6A23C" timestamp="文档整理">
              更新 README、项目进度文档、演示说明和汇报截图
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
      moduleList: [
        {
          module: '权限治理',
          ability: '用户组管理、用户关系、权限策略、策略绑定、文档权限标签',
          status: '已完成',
          type: 'success'
        },
        {
          module: 'RAG 文件入库',
          ability: '若依前端 → 若依后端代理 → RAG Server → MariaDB / MinIO / Milvus',
          status: '已跑通',
          type: 'success'
        },
        {
          module: '安全检索 Mock',
          ability: 'PermissionContext、Metadata Filter、sys_rag_doc 模拟候选结果、二次过滤',
          status: '已闭环',
          type: 'success'
        },
        {
          module: '真实检索',
          ability: 'useRemote=true 调用 RAG Server /rag/search，解析真实 Milvus 检索结果',
          status: '已联通',
          type: 'success'
        },
        {
          module: '审计追踪',
          ability: 'RAG 文件入库审计、RAG 检索审计、访问决策 allow_access：1 放行 / 0 拒绝',
          status: '已完成',
          type: 'success'
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
  margin-bottom: 16px;
}

.hero-card ::v-deep .el-card__body {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.hero-title {
  font-size: 26px;
  font-weight: 700;
  color: #303133;
  margin-bottom: 8px;
}

.hero-subtitle {
  font-size: 14px;
  color: #606266;
}

.status-row {
  margin-bottom: 16px;
}

.status-card {
  height: 120px;
}

.status-label {
  color: #909399;
  font-size: 14px;
  margin-bottom: 12px;
}

.status-value {
  font-size: 26px;
  font-weight: 700;
  margin-bottom: 10px;
}

.status-value.success {
  color: #67C23A;
}

.status-desc {
  color: #606266;
  font-size: 13px;
  line-height: 1.5;
}

.box-card {
  margin-bottom: 16px;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.section-title {
  font-weight: 700;
  color: #303133;
}

.flow-box {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  margin: 14px 0 18px;
}

.flow-node {
  padding: 10px 14px;
  border: 1px solid #b3d8ff;
  background: #ecf5ff;
  color: #1f6fbf;
  border-radius: 4px;
  font-weight: 700;
  margin: 6px 0;
}

.flow-arrow {
  margin: 0 8px;
  color: #909399;
}

.chain-alert {
  margin-bottom: 16px;
}

.module-table {
  width: 100%;
}

.branch-block {
  padding: 14px 0;
  border-bottom: 1px solid #ebeef5;
}

.branch-block:last-child {
  border-bottom: none;
}

.branch-name {
  font-size: 16px;
  font-weight: 700;
  color: #303133;
  margin-bottom: 8px;
}

.branch-desc {
  color: #606266;
  font-size: 13px;
  line-height: 1.6;
}

.todo-card ::v-deep .el-card__body {
  padding-bottom: 4px;
}
</style>
