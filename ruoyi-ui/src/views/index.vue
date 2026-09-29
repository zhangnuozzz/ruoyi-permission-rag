<template>
  <div class="dashboard-container">

    <!-- 顶部概览 -->
    <el-card shadow="never" class="hero-card">
      <div class="hero-main">
        <div>
          <div class="hero-eyebrow">VECTOR ACCESS CONTROL SYSTEM</div>
          <div class="hero-title">向量库访问控制系统</div>
          <div class="hero-subtitle">
            面向大模型知识库与 RAG 场景，统一管理身份权限、知识入库、
            安全检索、结果过滤、审计留痕与行为告警。
          </div>
        </div>

        <div class="hero-tags">
          <el-tag type="success" effect="plain">统一权限治理</el-tag>
          <el-tag type="primary" effect="plain">安全向量检索</el-tag>
          <el-tag type="warning" effect="plain">审计与告警</el-tag>
        </div>
      </div>
    </el-card>

    <!-- 四项核心能力 -->
    <el-row :gutter="16" class="summary-row">
      <el-col :span="6">
        <el-card shadow="never" class="summary-card">
          <div class="summary-icon">
            <i class="el-icon-user"></i>
          </div>
          <div class="summary-content">
            <div class="summary-title">身份与权限治理</div>
            <div class="summary-desc">
              用户安全属性、用户组、权限策略及策略绑定统一管理
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card shadow="never" class="summary-card">
          <div class="summary-icon">
            <i class="el-icon-upload2"></i>
          </div>
          <div class="summary-content">
            <div class="summary-title">知识文件入库</div>
            <div class="summary-desc">
              文件解析、对象存储、向量切块与权限标签同步
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card shadow="never" class="summary-card">
          <div class="summary-icon">
            <i class="el-icon-search"></i>
          </div>
          <div class="summary-content">
            <div class="summary-title">安全检索</div>
            <div class="summary-desc">
              权限约束、向量召回与平台侧结果二次校验
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card shadow="never" class="summary-card">
          <div class="summary-icon">
            <i class="el-icon-warning-outline"></i>
          </div>
          <div class="summary-content">
            <div class="summary-title">审计与告警</div>
            <div class="summary-desc">
              检索审计、风险分析、行为告警与 Syslog 转发
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 架构 -->
    <el-row :gutter="16">
      <el-col :span="15">
        <el-card shadow="never" class="content-card">
          <div slot="header" class="card-header">
            <div>
              <div class="card-title">系统总体架构</div>
              <div class="card-subtitle">
                权限治理与 RAG 检索分层，实现检索前约束与检索后二次校验
              </div>
            </div>
          </div>

          <div class="architecture-box">
            <img
              class="architecture-image"
              src="@/assets/images/rag/rag_architecture.png"
              alt="大模型向量库总体架构"
            >
          </div>
        </el-card>
      </el-col>

      <el-col :span="9">
        <el-card shadow="never" class="content-card">
          <div slot="header" class="card-header">
            <div>
              <div class="card-title">实现思路</div>
              <div class="card-subtitle">
                权限控制贯穿知识检索全链路
              </div>
            </div>
          </div>

          <div class="design-list">
            <div class="design-item">
              <div class="design-index">01</div>
              <div>
                <div class="design-title">统一身份与权限上下文</div>
                <div class="design-desc">
                  将用户密级、用户组、知悉范围及策略统一转换为检索约束。
                </div>
              </div>
            </div>

            <div class="design-item">
              <div class="design-index">02</div>
              <div>
                <div class="design-title">受控候选空间内向量召回</div>
                <div class="design-desc">
                  将权限信息转换为可执行过滤条件，在权限边界内进行候选检索。
                </div>
              </div>
            </div>

            <div class="design-item">
              <div class="design-index">03</div>
              <div>
                <div class="design-title">平台侧二次权限校验</div>
                <div class="design-desc">
                  对候选结果逐条校验密级、知悉范围与用户组，阻断越权结果。
                </div>
              </div>
            </div>

            <div class="design-item">
              <div class="design-index">04</div>
              <div>
                <div class="design-title">审计与风险处置</div>
                <div class="design-desc">
                  对访问决策、过滤结果与异常行为形成完整审计记录和告警闭环。
                </div>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 主链路 -->
    <el-card shadow="never" class="flow-card">
      <div slot="header" class="card-header">
        <div>
          <div class="card-title">安全检索主链路</div>
          <div class="card-subtitle">
            从身份认证到结果返回的统一访问控制过程
          </div>
        </div>
      </div>

      <div class="flow">
        <div class="flow-step">
          <i class="el-icon-user"></i>
          <span>身份认证</span>
        </div>

        <i class="el-icon-right flow-arrow"></i>

        <div class="flow-step">
          <i class="el-icon-key"></i>
          <span>权限解析</span>
        </div>

        <i class="el-icon-right flow-arrow"></i>

        <div class="flow-step">
          <i class="el-icon-search"></i>
          <span>向量召回</span>
        </div>

        <i class="el-icon-right flow-arrow"></i>

        <div class="flow-step">
          <i class="el-icon-lock"></i>
          <span>二次校验</span>
        </div>

        <i class="el-icon-right flow-arrow"></i>

        <div class="flow-step">
          <i class="el-icon-document-checked"></i>
          <span>结果返回</span>
        </div>

        <i class="el-icon-right flow-arrow"></i>

        <div class="flow-step">
          <i class="el-icon-document"></i>
          <span>审计留痕</span>
        </div>
      </div>
    </el-card>

    <div class="project-footer">
      <span>向量库访问控制系统</span>
      <span class="footer-separator">·</span>
      <span>实现：zhangnuozzz · fufu</span>
    </div>
  </div>
</template>

<script>
export default {
  name: 'Index'
}
</script>

<style scoped>
.dashboard-container {
  min-height: calc(100vh - 84px);
  padding: 22px;
  background: #f5f7fa;
}

.hero-card,
.summary-card,
.content-card,
.flow-card {
  border-radius: 8px;
}

.hero-card {
  margin-bottom: 16px;
}

.hero-main {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.hero-eyebrow {
  margin-bottom: 7px;
  color: #6f8ca8;
  font-size: 10px;
  letter-spacing: 1.6px;
}

.hero-title {
  color: #26384d;
  font-size: 27px;
  font-weight: 600;
}

.hero-subtitle {
  max-width: 760px;
  margin-top: 10px;
  color: #788898;
  font-size: 14px;
  line-height: 1.8;
}

.hero-tags {
  display: flex;
  gap: 8px;
  margin-left: 30px;
}

.summary-row {
  margin-bottom: 16px;
}

.summary-card {
  height: 108px;
}

.summary-card ::v-deep .el-card__body {
  height: 100%;
  display: flex;
  align-items: center;
  box-sizing: border-box;
}

.summary-icon {
  width: 44px;
  height: 44px;
  margin-right: 14px;
  flex-shrink: 0;
  border-radius: 10px;
  background: #eef5fc;
  color: #397fc4;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}

.summary-title {
  color: #33485d;
  font-size: 15px;
  font-weight: 600;
}

.summary-desc {
  margin-top: 7px;
  color: #8a98a7;
  font-size: 11px;
  line-height: 1.6;
}

.content-card {
  min-height: 455px;
  margin-bottom: 16px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-title {
  color: #34495e;
  font-size: 17px;
  font-weight: 600;
}

.card-subtitle {
  margin-top: 5px;
  color: #98a5b2;
  font-size: 11px;
}

.architecture-box {
  height: 355px;
  padding: 8px;
  border: 1px solid #edf0f4;
  border-radius: 6px;
  background: #fff;
  text-align: center;
}

.architecture-image {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.design-list {
  padding: 2px 4px;
}

.design-item {
  display: flex;
  padding: 14px 4px;
  border-bottom: 1px solid #edf0f4;
}

.design-item:last-child {
  border-bottom: 0;
}

.design-index {
  width: 38px;
  flex-shrink: 0;
  color: #3f82c4;
  font-size: 12px;
  font-weight: 600;
}

.design-title {
  color: #405569;
  font-size: 13px;
  font-weight: 600;
}

.design-desc {
  margin-top: 5px;
  color: #8897a6;
  font-size: 11px;
  line-height: 1.7;
}

.flow-card {
  margin-bottom: 12px;
}

.flow {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 11px 0;
}

.flow-step {
  min-width: 115px;
  height: 48px;
  padding: 0 14px;
  border: 1px solid #dce5ee;
  border-radius: 7px;
  background: #fafbfd;
  color: #486077;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
}

.flow-step i {
  margin-right: 7px;
  color: #4285c7;
  font-size: 16px;
}

.flow-arrow {
  margin: 0 10px;
  color: #b5c0cb;
}

.project-footer {
  padding: 8px 3px 2px;
  text-align: right;
  color: #a3adb7;
  font-size: 9px;
  letter-spacing: 0.3px;
}

.footer-separator {
  margin: 0 5px;
}
</style>
