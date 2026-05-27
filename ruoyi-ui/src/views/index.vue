<template>
  <div class="dashboard-container">
    <el-row :gutter="20">
      <el-col :span="24">
        <el-card class="hero-card" shadow="never">
          <div class="hero-title">权限控制 RAG 平台</div>
          <div class="hero-subtitle">
            面向大模型向量库场景，提供权限治理、RAG 文件入库、真实向量检索、权限二次过滤与审计追踪能力。
          </div>
          <div class="hero-tags">
            <el-tag type="success" effect="plain">RuoYi-Vue 3.2</el-tag>
            <el-tag type="primary" effect="plain">RAG Server 8081</el-tag>
            <el-tag type="warning" effect="plain">MariaDB / MinIO / Milvus</el-tag>
            <el-tag type="danger" effect="plain">权限过滤与审计</el-tag>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="status-row">
      <el-col :span="6" v-for="item in statusCards" :key="item.title">
        <el-card class="status-card" shadow="never">
          <div class="status-label">{{ item.title }}</div>
          <div class="status-value">{{ item.value }}</div>
          <div class="status-desc">{{ item.desc }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="main-row">
      <el-col :span="14">
        <el-card shadow="never" class="box-card">
          <div slot="header" class="section-header">
            <span class="section-title">核心安全检索链路</span>
            <el-tag type="success" size="mini">首轮真实联调完成</el-tag>
          </div>

          <div class="flow-box">
            <div class="flow-node">用户问题</div>
            <div class="flow-arrow">→</div>
            <div class="flow-node">权限上下文</div>
            <div class="flow-arrow">→</div>
            <div class="flow-node">Metadata Filter</div>
            <div class="flow-arrow">→</div>
            <div class="flow-node active">RAG Server 真实检索</div>
            <div class="flow-arrow">→</div>
            <div class="flow-node danger">平台二次过滤</div>
            <div class="flow-arrow">→</div>
            <div class="flow-node">审计留痕</div>
          </div>

          <el-alert
            title="当前已完成真实检索第一轮联调：平台侧 useRemote=true 调用 RAG Server /rag/search，RAG Server 基于 Milvus 返回候选结果，平台侧继续执行权限二次过滤与审计留痕。"
            type="success"
            :closable="false"
            show-icon
            class="info-alert"
          />

          <el-table :data="abilityList" border stripe class="ability-table">
            <el-table-column label="模块" prop="module" width="140" align="center" />
            <el-table-column label="当前能力" prop="ability" min-width="260" />
            <el-table-column label="状态" width="120" align="center">
              <template slot-scope="scope">
                <el-tag :type="scope.row.type" size="small" effect="plain">
                  {{ scope.row.status }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <el-col :span="10">
        <el-card shadow="never" class="box-card">
          <div slot="header" class="section-header">
            <span class="section-title">当前分支与进度</span>
            <el-tag type="info" size="mini">feature/platform-remote-search</el-tag>
          </div>

          <div class="branch-block">
            <div class="branch-title">fufu_week4 / RAG Server</div>
            <div class="branch-desc">
              RAG Server 侧已完成文件入库、MariaDB 元数据、MinIO 原文存储、Milvus 切块写入，并已提供真实 /rag/search 检索接口。
            </div>
          </div>

          <div class="branch-block">
            <div class="branch-title">feature/platform-remote-search</div>
            <div class="branch-desc">
              平台侧已完成 useRemote=true、topK、RemoteSearchService、真实返回格式解析、权限二次过滤与 RAG 检索审计适配。
            </div>
          </div>

          <div class="branch-block">
            <div class="branch-title">审计语义修正</div>
            <div class="branch-desc">
              sys_rag_audit_log.allow_access 已统一为：1 表示放行，0 表示拒绝；前端审计日志页面已同步修正。
            </div>
          </div>

          <el-divider />

          <div class="next-title">下一步建议</div>
          <el-timeline>
            <el-timeline-item timestamp="演示链路" type="success">
              固化“上传文件 → Milvus 查看 → 真实检索 → 审计日志”的完整演示流程
            </el-timeline-item>
            <el-timeline-item timestamp="测试数据" type="primary">
              准备 INTERNAL、DOC_ADMIN 等不同权限标签的对照样例
            </el-timeline-item>
            <el-timeline-item timestamp="分支管理" type="warning">
              确认真实检索稳定后，再评估是否合并回 main 稳定展示分支
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
      statusCards: [
        {
          title: '权限治理',
          value: '已完成',
          desc: '用户组、策略、绑定、权限上下文',
        },
        {
          title: 'RAG 文件入库',
          value: '已跑通',
          desc: 'MariaDB / MinIO / Milvus 三段式链路',
        },
        {
          title: '真实检索联调',
          value: '已完成首轮',
          desc: 'useRemote=true 调用 /rag/search',
        },
        {
          title: '审计闭环',
          value: '已修正',
          desc: 'allow_access：1 放行 / 0 拒绝',
        }
      ],
      abilityList: [
        {
          module: '权限上下文',
          ability: '根据当前登录用户生成 userId、userName、groupCodes、scopeCodes、admin 等上下文',
          status: '已完成',
          type: 'success'
        },
        {
          module: '文件入库',
          ability: '若依前端上传文件，经若依后端代理转发到 RAG Server，写入 MariaDB、MinIO、Milvus',
          status: '已跑通',
          type: 'success'
        },
        {
          module: '真实检索',
          ability: '平台侧 useRemote=true 调用 RAG Server /rag/search，解析真实 Milvus 检索结果',
          status: '已联调',
          type: 'success'
        },
        {
          module: '二次过滤',
          ability: '平台侧根据 scopeCode 与当前用户可访问标签进行二次过滤，防止越权结果返回',
          status: '已完成',
          type: 'success'
        },
        {
          module: '审计日志',
          ability: '记录 query、groupCodes、scopeCodes、metadataFilter、allow_access、耗时等字段',
          status: '已修正',
          type: 'success'
        }
      ]
    }
  }
}
</script>

<style lang="scss" scoped>
.dashboard-container {
  padding: 24px;
  background: #f5f7fa;
  min-height: calc(100vh - 84px);
}

.hero-card {
  border-radius: 8px;
  margin-bottom: 20px;
}

.hero-title {
  font-size: 28px;
  font-weight: 700;
  color: #303133;
  margin-bottom: 12px;
}

.hero-subtitle {
  font-size: 15px;
  color: #606266;
  line-height: 1.8;
  margin-bottom: 16px;
}

.hero-tags .el-tag {
  margin-right: 10px;
  margin-bottom: 8px;
}

.status-row {
  margin-bottom: 20px;
}

.status-card {
  height: 132px;
  border-radius: 8px;
}

.status-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 12px;
}

.status-value {
  font-size: 24px;
  font-weight: 700;
  color: #303133;
  margin-bottom: 12px;
}

.status-desc {
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
}

.main-row {
  margin-top: 4px;
}

.box-card {
  border-radius: 8px;
  min-height: 520px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.section-title {
  font-size: 16px;
  font-weight: 700;
  color: #303133;
}

.flow-box {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  margin: 8px 0 18px;
}

.flow-node {
  padding: 10px 14px;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  background: #fff;
  color: #303133;
  font-size: 13px;
  margin-bottom: 10px;
}

.flow-node.active {
  border-color: #67c23a;
  background: #f0f9eb;
  color: #67c23a;
  font-weight: 700;
}

.flow-node.danger {
  border-color: #f56c6c;
  background: #fef0f0;
  color: #f56c6c;
  font-weight: 700;
}

.flow-arrow {
  margin: 0 8px 10px;
  color: #909399;
}

.info-alert {
  margin-bottom: 18px;
}

.ability-table {
  width: 100%;
}

.branch-block {
  padding: 14px 0;
  border-bottom: 1px solid #ebeef5;
}

.branch-title {
  font-size: 15px;
  font-weight: 700;
  color: #303133;
  margin-bottom: 8px;
}

.branch-desc {
  font-size: 13px;
  color: #606266;
  line-height: 1.7;
}

.next-title {
  font-size: 15px;
  font-weight: 700;
  color: #303133;
  margin-bottom: 16px;
}
</style>
