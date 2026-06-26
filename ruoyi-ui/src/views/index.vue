<template>
  <div class="dashboard-container">
    <el-card class="hero-card" shadow="never">
      <div class="hero-title">权限控制 RAG 平台</div>
      <div class="hero-subtitle">
        面向大模型向量库场景，提供权限治理、RAG 文件入库、真实向量检索、权限二次过滤、审计留痕、日志导出与行为分析告警能力。
      </div>
      <div class="tag-row">
        <el-tag type="success">RuoYi-Vue 3.2</el-tag>
        <el-tag>RAG Server 8081</el-tag>
        <el-tag type="warning">MariaDB / MinIO / Milvus</el-tag>
        <el-tag type="danger">权限过滤与审计</el-tag>
        <el-tag type="info">平台侧已完成</el-tag>
      </div>
    </el-card>

    <el-row :gutter="16" class="summary-row">
      <el-col :span="6">
        <el-card shadow="hover" class="summary-card">
          <div class="summary-label">权限治理</div>
          <div class="summary-value">已完成</div>
          <div class="summary-desc">用户组、策略、策略绑定、权限上下文</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="summary-card">
          <div class="summary-label">RAG 文件入库</div>
          <div class="summary-value">已跑通</div>
          <div class="summary-desc">MariaDB / MinIO / Milvus 三段式链路</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="summary-card">
          <div class="summary-label">权限检索过滤</div>
          <div class="summary-value">已联调</div>
          <div class="summary-desc">Metadata Filter + 平台二次过滤</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="summary-card">
          <div class="summary-label">审计与告警</div>
          <div class="summary-value">已闭环</div>
          <div class="summary-desc">JSON 留痕、日志导出、行为分析</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :span="14">
        <el-card shadow="never" class="box-card">
          <div slot="header" class="card-title">
            <span>系统总体架构</span>
            <el-tag size="mini" type="success">1–10 平台侧闭环</el-tag>
          </div>

          <div class="arch-wrapper">
            <img class="arch-img" src="@/assets/images/rag/rag_architecture.png" alt="权限控制 RAG 平台架构图">
          </div>

          <div class="arch-note">
            系统围绕“权限治理 → 文档入库 → RAG 检索 → 权限过滤 → 审计分析”构建平台侧闭环。
            用户请求进入平台后，系统先生成权限上下文和 Metadata Filter，再调用 RAG Server 进行向量检索，
            返回结果会经过平台侧二次过滤，并写入审计日志，后续支持日志导出和行为分析告警。
          </div>
        </el-card>
      </el-col>

      <el-col :span="10">
        <el-card shadow="never" class="box-card">
          <div slot="header" class="card-title">
            <span>当前实现边界</span>
            <el-tag size="mini" type="warning">平台侧完成</el-tag>
          </div>

          <el-timeline>
            <el-timeline-item color="#67C23A" timestamp="平台侧已完成">
              权限主体、权限策略、策略绑定、文档标签、RAG 入库、权限过滤、审计日志、日志导出与行为告警均已实现。
            </el-timeline-item>
            <el-timeline-item color="#409EFF" timestamp="RAG 检索已联调">
              平台侧可调用 RAG Server 的真实检索接口，基于 Milvus 返回候选结果，并进行权限二次过滤。
            </el-timeline-item>
            <el-timeline-item color="#E6A23C" timestamp="后续扩展">
              大模型生成回答层可继续接入 LLM API 或本地模型，将检索结果作为上下文生成自然语言答案。
            </el-timeline-item>
          </el-timeline>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="box-card module-card">
      <div slot="header" class="card-title">
        <span>功能模块实现说明</span>
        <el-tag size="mini" type="danger">对应架构图 1–10</el-tag>
      </div>

      <el-table :data="moduleList" border>
        <el-table-column label="编号" prop="id" width="70" align="center" />
        <el-table-column label="模块" prop="name" width="150" align="center" />
        <el-table-column label="实现说明" prop="desc" min-width="420" />
        <el-table-column label="状态" width="100" align="center">
          <template slot-scope="scope">
            <el-tag v-if="scope.row.status === '已完成'" type="success">{{ scope.row.status }}</el-tag>
            <el-tag v-else-if="scope.row.status === '已联调'" type="primary">{{ scope.row.status }}</el-tag>
            <el-tag v-else type="warning">{{ scope.row.status }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-row :gutter="16">
      <el-col :span="12">
        <el-card shadow="never" class="box-card">
          <div slot="header" class="card-title">核心安全检索链路</div>
          <div class="flow-line">
            <span>用户问题</span>
            <i class="el-icon-right"></i>
            <span>权限上下文</span>
            <i class="el-icon-right"></i>
            <span>Metadata Filter</span>
            <i class="el-icon-right"></i>
            <span class="green">RAG Server 检索</span>
            <i class="el-icon-right"></i>
            <span class="red">二次过滤</span>
            <i class="el-icon-right"></i>
            <span>审计留痕</span>
          </div>
          <div class="green-box">
            当前平台侧已完成真实检索第一轮联调：平台使用 useRemote=true 调用 RAG Server /rag/search，
            RAG Server 基于 Milvus 返回候选结果，平台继续执行权限二次过滤与审计留痕。
          </div>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card shadow="never" class="box-card">
          <div slot="header" class="card-title">审计与行为分析闭环</div>
          <div class="audit-list">
            <div><i class="el-icon-document"></i> 审计日志：记录 query、用户组、权限标签、metadataFilter、allow_access、耗时等字段。</div>
            <div><i class="el-icon-view"></i> JSON 详情：记录用户上下文、请求 JSON、原始候选、通过结果、拦截结果和响应 JSON。</div>
            <div><i class="el-icon-download"></i> 日志导出：支持将 RAG 审计日志导出为 Excel。</div>
            <div><i class="el-icon-warning-outline"></i> 行为分析：基于审计日志生成普通访问、拒绝访问、异常耗时等告警记录。</div>
          </div>
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
        { id: '1', name: '基础信息', desc: '维护权限主体基础信息，包括用户、用户组以及用户与用户组关系，为后续权限上下文生成提供基础数据。', status: '已完成' },
        { id: '2', name: '权限策略', desc: '支持定义读取密级、知悉范围、访问时间、访问对象等权限策略，用于描述文档访问规则。', status: '已完成' },
        { id: '3', name: '策略绑定实体', desc: '将权限策略绑定到用户、用户组、文档或目录等实体上，形成可执行的访问控制关系。', status: '已完成' },
        { id: '4', name: '文件上传', desc: '通过 RAG 文件入库页面上传文档，平台转发到 RAG Server，写入 MariaDB 元数据、MinIO 原文件和 Milvus 向量库。', status: '已完成' },
        { id: '5', name: '标签绑定', desc: '维护文档的知悉范围、文档密级和所属用户组等权限标签，文件入库后自动生成标签记录，也支持后期维护。', status: '已完成' },
        { id: '6', name: '访问请求处理', desc: '用户发起 RAG 检索时，平台解析当前用户身份、用户组和可访问权限标签，并构造权限上下文。', status: '已完成' },
        { id: '7', name: '数据检索', desc: '平台调用 RAG Server 的真实检索接口，RAG Server 基于 Milvus 向量库返回候选检索结果。', status: '已联调' },
        { id: '8', name: '数据过滤模块', desc: '平台对 RAG 返回结果再次进行权限校验，过滤不符合当前用户权限范围的数据，防止越权返回。', status: '已完成' },
        { id: '9', name: '日志导出', desc: 'RAG 审计日志支持页面查看、JSON 详情查看和 Excel 导出，便于后续审计追踪与归档。', status: '已完成' },
        { id: '10', name: '行为分析', desc: '基于访问审计日志进行规则分析，生成普通访问、拒绝访问、异常耗时等行为告警并入库展示。', status: '已完成' }
      ]
    }
  }
}
</script>

<style scoped>
.dashboard-container {
  padding: 24px;
  background: #f5f7fa;
  min-height: calc(100vh - 84px);
}
.hero-card,
.box-card,
.summary-card {
  border-radius: 8px;
}
.hero-card {
  margin-bottom: 16px;
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
  margin-bottom: 14px;
}
.tag-row .el-tag {
  margin-right: 10px;
  margin-bottom: 6px;
}
.summary-row {
  margin-bottom: 16px;
}
.summary-label {
  color: #909399;
  font-size: 14px;
  margin-bottom: 8px;
}
.summary-value {
  color: #303133;
  font-size: 24px;
  font-weight: 700;
  margin-bottom: 8px;
}
.summary-desc {
  color: #606266;
  font-size: 13px;
  line-height: 1.6;
}
.box-card {
  margin-bottom: 16px;
}
.card-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 18px;
  font-weight: 600;
}
.arch-wrapper {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 12px;
  text-align: center;
}
.arch-img {
  width: 100%;
  max-height: 520px;
  object-fit: contain;
}
.arch-note {
  margin-top: 12px;
  padding: 12px 14px;
  background: #ecf5ff;
  color: #606266;
  border-radius: 4px;
  line-height: 1.8;
  font-size: 14px;
}
.flow-line {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  margin-bottom: 18px;
}
.flow-line span {
  padding: 10px 16px;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  background: #fff;
  color: #303133;
}
.flow-line .green {
  color: #67c23a;
  border-color: #67c23a;
  font-weight: 600;
}
.flow-line .red {
  color: #f56c6c;
  border-color: #f56c6c;
  font-weight: 600;
}
.green-box {
  padding: 12px 14px;
  color: #2f9b56;
  background: #e8f8ef;
  border-radius: 4px;
  line-height: 1.8;
  font-size: 14px;
}
.audit-list {
  color: #606266;
  line-height: 2.1;
  font-size: 14px;
}
.audit-list i {
  margin-right: 6px;
  color: #409eff;
}
</style>
