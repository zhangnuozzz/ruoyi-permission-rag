<template>
  <div v-loading="loading" class="situation-page">
    <header class="overview-header">
      <div class="header-copy">
        <div class="eyebrow"><span class="pulse-dot" /> SECURITY SITUATION OVERVIEW</div>
        <h1>安全态势感知</h1>
        <p>向量库访问控制系统 · 聚合身份、知识资产、检索请求与风险告警</p>
      </div>
      <div class="header-actions">
        <div class="update-info">
          <span>数据更新时间</span>
          <strong>{{ overview.generatedAt || '等待加载' }}</strong>
        </div>
        <el-button icon="el-icon-refresh" plain :loading="loading" @click="loadOverview">刷新数据</el-button>
      </div>
    </header>

    <div v-if="loadError" class="feedback-card">
      <i class="el-icon-warning-outline" />
      <div>
        <strong>暂时无法读取态势数据</strong>
        <p>{{ loadError }}</p>
      </div>
      <el-button size="small" type="primary" plain @click="loadOverview">重新加载</el-button>
    </div>

    <template v-else-if="loaded">
      <section class="metrics-grid" aria-label="平台核心指标">
        <div class="metric-card users">
          <div class="metric-icon"><i class="el-icon-user-solid" /></div>
          <div class="metric-label">用户总量</div>
          <div class="metric-value">{{ formatNumber(overview.totals.users) }}</div>
          <div class="metric-note">未删除的平台注册用户</div>
          <span class="metric-index">01</span>
        </div>
        <div class="metric-card documents">
          <div class="metric-icon"><i class="el-icon-document" /></div>
          <div class="metric-label">文档总量</div>
          <div class="metric-value">{{ formatNumber(overview.totals.documents) }}</div>
          <div class="metric-note">已登记的文档权限标签</div>
          <span class="metric-index">02</span>
        </div>
        <div class="metric-card requests">
          <div class="metric-icon"><i class="el-icon-search" /></div>
          <div class="metric-label">请求总数</div>
          <div class="metric-value">{{ formatNumber(overview.totals.requests) }}</div>
          <div class="metric-note">今日新增 {{ formatNumber(overview.totals.todayRequests) }} 次</div>
          <span class="metric-index">03</span>
        </div>
        <div class="metric-card alerts">
          <div class="metric-icon"><i class="el-icon-warning" /></div>
          <div class="metric-label">待处理告警</div>
          <div class="metric-value">{{ formatNumber(overview.totals.pendingAlerts) }}</div>
          <div class="metric-note">今日拒绝请求 {{ formatNumber(overview.totals.todayDenied) }} 次</div>
          <span class="metric-index">04</span>
        </div>
      </section>

      <section class="analysis-grid" aria-label="态势分析">
        <div class="panel trend-panel">
          <div class="panel-heading">
            <div>
              <div class="panel-kicker">REQUEST TRAFFIC</div>
              <h2>近七日请求趋势</h2>
              <p>按审计日志产生时间统计，每日请求量与拒绝量</p>
            </div>
            <span class="period-chip">最近 7 天</span>
          </div>
          <div ref="trendChart" class="trend-chart" role="img" aria-label="近七日请求与拒绝趋势图" />
          <div class="chart-legend"><span><i class="legend-dot primary" /> 请求总量</span><span><i class="legend-dot danger" /> 拒绝请求</span></div>
        </div>

        <div class="panel decision-panel">
          <div class="panel-heading">
            <div>
              <div class="panel-kicker">ACCESS DECISION</div>
              <h2>访问决策分布</h2>
              <p>基于所有检索请求的访问决策</p>
            </div>
          </div>
          <div v-if="decisionTotal" class="decision-body">
            <div class="donut-wrap">
              <div ref="decisionChart" class="decision-chart" role="img" aria-label="放行与拒绝请求占比" />
              <div class="donut-center"><strong>{{ allowRate }}%</strong><span>放行率</span></div>
            </div>
            <div class="decision-stats">
              <div><span><i class="legend-dot primary" /> 已放行</span><strong>{{ formatNumber(overview.decisions.allowed) }}</strong></div>
              <div><span><i class="legend-dot danger" /> 已拒绝</span><strong>{{ formatNumber(overview.decisions.denied) }}</strong></div>
              <div><span><i class="legend-dot neutral" /> 未判定</span><strong>{{ formatNumber(overview.decisions.undetermined) }}</strong></div>
            </div>
          </div>
          <div v-else class="empty-state"><i class="el-icon-pie-chart" /><span>暂无访问决策数据</span></div>
        </div>
      </section>

      <section class="detail-grid" aria-label="资产与风险动态">
        <div class="panel levels-panel">
          <div class="panel-heading compact">
            <div>
              <div class="panel-kicker">KNOWLEDGE ASSETS</div>
              <h2>文档分级概览</h2>
              <p>按文档权限标签中的密级统计</p>
            </div>
          </div>
          <div v-if="overview.documentLevels.length" class="level-list">
            <div v-for="item in overview.documentLevels" :key="item.level" class="level-item">
              <div class="level-label"><span>{{ levelName(item.level) }}</span><strong>{{ formatNumber(item.total) }}</strong></div>
              <div class="level-track"><span :style="{ width: levelWidth(item.total) }" /></div>
            </div>
          </div>
          <div v-else class="empty-state small"><i class="el-icon-folder-opened" /><span>暂无文档数据</span></div>
        </div>

        <div class="panel alerts-panel">
          <div class="panel-heading compact">
            <div>
              <div class="panel-kicker">RISK WATCH</div>
              <h2>最新待处理告警</h2>
              <p>优先关注尚未处置的安全事件</p>
            </div>
            <span class="pending-badge">{{ overview.totals.pendingAlerts }} 待处理</span>
          </div>
          <div v-if="overview.pendingAlerts.length" class="alert-list">
            <div v-for="item in overview.pendingAlerts" :key="item.id" class="alert-item">
              <span class="severity-mark" :class="severityClass(item.alertLevel)" />
              <div class="alert-main"><strong>{{ alertName(item.alertType) }}</strong><span>{{ item.userName || '未知用户' }} · {{ item.createTime || '--' }}</span></div>
              <span class="severity-label" :class="severityClass(item.alertLevel)">{{ severityName(item.alertLevel) }}</span>
            </div>
          </div>
          <div v-else class="empty-state small"><i class="el-icon-circle-check" /><span>当前没有待处理告警</span></div>
        </div>
      </section>

      <section class="panel activity-panel" aria-label="最近检索请求">
        <div class="panel-heading compact">
          <div>
            <div class="panel-kicker">LATEST ACTIVITY</div>
            <h2>最近检索请求</h2>
            <p>仅展示必要的审计摘要，不在首页暴露检索正文</p>
          </div>
          <span class="table-caption">最近 6 条</span>
        </div>
        <div class="table-scroll">
          <table v-if="overview.recentRequests.length" class="activity-table">
            <thead><tr><th>时间</th><th>用户</th><th>访问决策</th><th>通过结果</th><th>拦截结果</th><th>耗时</th></tr></thead>
            <tbody>
              <tr v-for="item in overview.recentRequests" :key="item.id">
                <td>{{ item.createTime || '--' }}</td>
                <td class="user-cell">{{ item.userName || '未知用户' }}</td>
                <td><span class="decision-tag" :class="decisionClass(item.allowAccess)">{{ decisionName(item.allowAccess) }}</span></td>
                <td>{{ formatNumber(item.passedCount) }}</td>
                <td>{{ formatNumber(item.blockedCount) }}</td>
                <td>{{ item.costTime == null ? '--' : formatNumber(item.costTime) + ' ms' }}</td>
              </tr>
            </tbody>
          </table>
          <div v-else class="empty-state small"><i class="el-icon-document" /><span>暂无检索请求记录</span></div>
        </div>
      </section>
      <p class="data-note">统计口径：用户和文档不含已删除记录；请求与访问决策来自 RAG 审计日志；待处理告警来自行为告警表。页面每 60 秒自动刷新。</p>
    </template>
    <div v-else class="feedback-card initial-loading"><i class="el-icon-loading" /><span>正在读取平台态势数据…</span></div>
  </div>
</template>

<script>
import echarts from 'echarts'
import { getSituationOverview } from '@/api/rag/dashboard'

export default {
  name: 'Index',
  data() {
    return {
      loading: false,
      loaded: false,
      loadError: '',
      refreshTimer: null,
      trendInstance: null,
      decisionInstance: null,
      overview: {
        generatedAt: '',
        totals: { users: 0, documents: 0, requests: 0, pendingAlerts: 0, todayRequests: 0, todayDenied: 0 },
        decisions: { allowed: 0, denied: 0, undetermined: 0 },
        trend: [],
        documentLevels: [],
        pendingAlerts: [],
        recentRequests: []
      }
    }
  },
  computed: {
    decisionTotal() {
      return Number(this.overview.decisions.allowed || 0) + Number(this.overview.decisions.denied || 0)
    },
    allowRate() {
      return this.decisionTotal ? Math.round(Number(this.overview.decisions.allowed || 0) / this.decisionTotal * 100) : 0
    },
    maxLevelCount() {
      return Math.max(1, ...this.overview.documentLevels.map(item => Number(item.total || 0)))
    }
  },
  mounted() {
    this.loadOverview()
    this.refreshTimer = window.setInterval(this.loadOverview, 60000)
    window.addEventListener('resize', this.resizeCharts)
  },
  beforeDestroy() {
    window.clearInterval(this.refreshTimer)
    window.removeEventListener('resize', this.resizeCharts)
    this.disposeCharts()
  },
  methods: {
    async loadOverview() {
      if (this.loading) return
      this.loading = true
      try {
        const response = await getSituationOverview()
        this.overview = response.data
        this.loaded = true
        this.loadError = ''
        this.$nextTick(this.renderCharts)
      } catch (error) {
        if (!this.loaded) {
          this.loadError = error && error.response && error.response.status === 403
            ? '平台级统计仅对管理员开放，请使用管理员账号登录。'
            : '请确认后台服务与数据库连接正常，然后重试。'
        }
      } finally {
        this.loading = false
      }
    },
    renderCharts() {
      if (!this.$refs.trendChart) return
      if (!this.trendInstance) this.trendInstance = echarts.init(this.$refs.trendChart)
      const trend = this.overview.trend || []
      this.trendInstance.setOption({
        color: ['#3578e5', '#ee6b6b'],
        animationDuration: 450,
        grid: { left: 18, right: 20, top: 22, bottom: 20, containLabel: true },
        tooltip: { trigger: 'axis', backgroundColor: '#173253', textStyle: { color: '#fff' }, axisPointer: { type: 'line' }},
        xAxis: { type: 'category', boundaryGap: false, data: trend.map(item => item.date.slice(5)), axisLine: { lineStyle: { color: '#dfe7f0' }}, axisTick: { show: false }, axisLabel: { color: '#8292a8', margin: 14 }},
        yAxis: { type: 'value', minInterval: 1, splitLine: { lineStyle: { color: '#edf1f6', type: 'dashed' }}, axisLine: { show: false }, axisTick: { show: false }, axisLabel: { color: '#9aa9ba' }},
        series: [
          { name: '请求总量', type: 'line', smooth: true, symbol: 'circle', symbolSize: 7, lineStyle: { width: 3 }, areaStyle: { color: 'rgba(53, 120, 229, 0.10)' }, data: trend.map(item => item.requests) },
          { name: '拒绝请求', type: 'line', smooth: true, symbol: 'circle', symbolSize: 6, lineStyle: { width: 2 }, data: trend.map(item => item.denied) }
        ]
      }, true)

      if (this.decisionTotal && this.$refs.decisionChart) {
        if (!this.decisionInstance) this.decisionInstance = echarts.init(this.$refs.decisionChart)
        this.decisionInstance.setOption({
          animationDuration: 450,
          color: ['#3578e5', '#ee6b6b'],
          tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
          series: [{ type: 'pie', radius: ['69%', '85%'], center: ['50%', '50%'], hoverOffset: 5, label: { show: false }, data: [
            { name: '已放行', value: Number(this.overview.decisions.allowed || 0) },
            { name: '已拒绝', value: Number(this.overview.decisions.denied || 0) }
          ] }]
        }, true)
      } else if (this.decisionInstance) {
        this.decisionInstance.dispose()
        this.decisionInstance = null
      }
    },
    resizeCharts() {
      if (this.trendInstance) this.trendInstance.resize()
      if (this.decisionInstance) this.decisionInstance.resize()
    },
    disposeCharts() {
      if (this.trendInstance) this.trendInstance.dispose()
      if (this.decisionInstance) this.decisionInstance.dispose()
      this.trendInstance = null
      this.decisionInstance = null
    },
    formatNumber(value) {
      return Number(value || 0).toLocaleString('zh-CN')
    },
    levelWidth(value) {
      return Math.max(3, Number(value || 0) / this.maxLevelCount * 100) + '%'
    },
    levelName(value) {
      return ({ PUBLIC: '公开', INTERNAL: '内部', SECRET: '秘密', CONFIDENTIAL: '机密', TOP_SECRET: '绝密', UNKNOWN: '未标记' })[value] || value || '未标记'
    },
    severityClass(value) {
      return ['critical', 'high', 'medium', 'low'].includes(value) ? value : 'low'
    },
    severityName(value) {
      return ({ critical: '严重', high: '高危', medium: '中危', low: '低危' })[value] || '低危'
    },
    alertName(value) {
      return ({ CRITICAL_RISK_QUERY: '严重风险查询', HIGH_RISK_QUERY: '高风险查询', DENY_ACCESS: '拒绝访问', SENSITIVE_QUERY: '敏感词查询', LARGE_TOPK_QUERY: '超量检索', MASSIVE_RESULT_BLOCK: '大量结果拦截', SECOND_FILTER_BLOCK: '二次过滤拦截', SLOW_QUERY: '异常耗时', HIGH_FREQUENCY_ACCESS: '高频访问' })[value] || value || '安全告警'
    },
    decisionClass(value) {
      return String(value) === '1' ? 'allowed' : String(value) === '0' ? 'denied' : 'unknown'
    },
    decisionName(value) {
      return String(value) === '1' ? '放行' : String(value) === '0' ? '拒绝' : '未判定'
    }
  }
}
</script>

<style scoped>
.situation-page { min-height: calc(100vh - 84px); padding: 22px 24px 28px; background: #f3f6fb; color: #263b55; }
.overview-header { min-height: 172px; padding: 29px 34px; display: flex; justify-content: space-between; align-items: center; gap: 24px; border-radius: 15px; background: linear-gradient(115deg, #153455 0%, #1b456f 54%, #245b88 100%); color: #fff; box-shadow: 0 12px 28px rgba(28, 63, 101, .13); }
.eyebrow { display: flex; align-items: center; gap: 8px; color: #9fc8ee; font-size: 11px; font-weight: 700; letter-spacing: 1.8px; }
.pulse-dot { width: 7px; height: 7px; border-radius: 50%; background: #64d9c1; box-shadow: 0 0 0 4px rgba(100, 217, 193, .15); }
.header-copy h1 { margin: 13px 0 9px; font-size: 29px; line-height: 1.2; font-weight: 650; letter-spacing: .5px; }
.header-copy p { margin: 0; color: #bfd2e6; font-size: 13px; }
.header-actions { display: flex; align-items: center; gap: 18px; }
.update-info { display: flex; flex-direction: column; gap: 6px; text-align: right; white-space: nowrap; }
.update-info span { color: #9eb9d2; font-size: 11px; }
.update-info strong { color: #ecf5ff; font-size: 13px; font-weight: 500; }
.header-actions .el-button { border-color: rgba(255,255,255,.35); background: rgba(255,255,255,.10); color: #fff; }
.header-actions .el-button:hover { background: rgba(255,255,255,.20); border-color: #fff; }
.metrics-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 16px; margin-top: 18px; }
.metric-card { position: relative; min-height: 156px; padding: 22px 23px; overflow: hidden; background: #fff; border: 1px solid #e5ebf3; border-radius: 13px; box-shadow: 0 4px 15px rgba(30, 58, 91, .035); }
.metric-card::before { content: ''; position: absolute; top: 0; left: 22px; width: 42px; height: 3px; border-radius: 0 0 4px 4px; background: var(--tone); }
.metric-card.users { --tone: #3279e8; --pale: #eaf2ff; }.metric-card.documents { --tone: #18a7a1; --pale: #e5f7f5; }.metric-card.requests { --tone: #7768d7; --pale: #f0edff; }.metric-card.alerts { --tone: #e69745; --pale: #fff3e6; }
.metric-icon { position: absolute; right: 23px; top: 23px; width: 42px; height: 42px; display: grid; place-items: center; border-radius: 11px; background: var(--pale); color: var(--tone); font-size: 20px; }
.metric-label { color: #6f829a; font-size: 13px; font-weight: 600; }
.metric-value { margin-top: 15px; color: #1d3552; font-size: 34px; line-height: 1.15; font-weight: 700; font-variant-numeric: tabular-nums; }
.metric-note { margin-top: 10px; color: #93a3b6; font-size: 12px; }
.metric-index { position: absolute; right: 24px; bottom: 18px; color: #e8edf4; font-size: 21px; font-weight: 700; }
.analysis-grid, .detail-grid { display: grid; gap: 16px; margin-top: 16px; }
.analysis-grid { grid-template-columns: minmax(0, 1.75fr) minmax(330px, 1fr); }.detail-grid { grid-template-columns: minmax(320px, 1fr) minmax(0, 1.55fr); }
.panel { min-width: 0; background: #fff; border: 1px solid #e5ebf3; border-radius: 13px; box-shadow: 0 4px 15px rgba(30, 58, 91, .035); }
.panel-heading { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; padding: 22px 23px 0; }
.panel-heading.compact { padding-bottom: 18px; border-bottom: 1px solid #edf1f5; }
.panel-kicker { margin-bottom: 6px; color: #8da6c3; font-size: 10px; font-weight: 700; letter-spacing: 1.4px; }
.panel-heading h2 { margin: 0; color: #233a56; font-size: 17px; font-weight: 650; }
.panel-heading p { margin: 7px 0 0; color: #9aa8b9; font-size: 12px; }
.period-chip, .pending-badge, .table-caption { white-space: nowrap; padding: 6px 10px; border-radius: 6px; background: #eef4fc; color: #537aa7; font-size: 11px; font-weight: 600; }
.pending-badge { background: #fff3e7; color: #ce8132; }
.trend-chart { height: 250px; margin: 2px 12px 0; }.chart-legend { display: flex; justify-content: center; gap: 24px; padding: 0 0 20px; color: #70829a; font-size: 11px; }
.chart-legend span, .decision-stats span { display: inline-flex; align-items: center; gap: 8px; }
.legend-dot { display: inline-block; width: 8px; height: 8px; border-radius: 50%; background: #b9c4d1; }.legend-dot.primary { background: #3578e5; }.legend-dot.danger { background: #ee6b6b; }.legend-dot.neutral { background: #b9c4d1; }
.decision-body { display: flex; align-items: center; justify-content: space-around; gap: 5px; min-height: 286px; padding: 8px 20px 16px; }
.donut-wrap { position: relative; width: 180px; height: 180px; flex: 0 0 180px; }.decision-chart { width: 100%; height: 100%; }.donut-center { position: absolute; inset: 0; display: flex; flex-direction: column; align-items: center; justify-content: center; pointer-events: none; }.donut-center strong { color: #274a73; font-size: 28px; line-height: 1.15; }.donut-center span { margin-top: 3px; color: #9aacbd; font-size: 12px; }
.decision-stats { width: 136px; display: flex; flex-direction: column; gap: 17px; }.decision-stats div { display: flex; justify-content: space-between; gap: 12px; align-items: center; color: #7e8fa5; font-size: 12px; }.decision-stats strong { color: #29425f; font-size: 14px; font-weight: 650; }
.level-list { padding: 19px 23px 23px; }.level-item + .level-item { margin-top: 17px; }.level-label { display: flex; justify-content: space-between; color: #73869c; font-size: 12px; }.level-label strong { color: #355575; font-size: 13px; }.level-track { height: 7px; margin-top: 9px; border-radius: 6px; background: #edf2f7; overflow: hidden; }.level-track span { display: block; height: 100%; border-radius: inherit; background: linear-gradient(90deg, #56a7ed, #3978da); }
.alert-list { padding: 0 22px; }.alert-item { display: flex; align-items: center; gap: 12px; min-height: 56px; border-bottom: 1px solid #f0f3f7; }.alert-item:last-child { border-bottom: 0; }.severity-mark { width: 7px; height: 7px; border-radius: 50%; background: #75a6d9; flex: 0 0 7px; }.severity-mark.critical, .severity-mark.high { background: #eb6970; }.severity-mark.medium { background: #eca654; }
.alert-main { min-width: 0; flex: 1; display: flex; flex-direction: column; gap: 4px; }.alert-main strong { color: #314b69; font-size: 12px; font-weight: 600; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }.alert-main span { color: #9baabb; font-size: 11px; }.severity-label { padding: 4px 8px; border-radius: 4px; background: #eef4fa; color: #6284a9; font-size: 11px; }.severity-label.critical, .severity-label.high { background: #fff0f0; color: #d85861; }.severity-label.medium { background: #fff5e8; color: #c88636; }
.activity-panel { margin-top: 16px; }.table-scroll { overflow-x: auto; }.activity-table { width: 100%; border-collapse: collapse; font-size: 12px; }.activity-table th { padding: 12px 24px; background: #f8fafd; color: #8b9cb0; text-align: left; font-weight: 600; white-space: nowrap; }.activity-table td { padding: 11px 24px; color: #667c96; border-top: 1px solid #f0f3f7; white-space: nowrap; }.activity-table .user-cell { color: #324f70; font-weight: 600; }.decision-tag { display: inline-block; min-width: 46px; padding: 4px 8px; text-align: center; border-radius: 4px; background: #f2f4f8; color: #8293a6; }.decision-tag.allowed { background: #e8f7f1; color: #3aa77b; }.decision-tag.denied { background: #fff0f0; color: #d75b65; }
.empty-state { min-height: 286px; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 10px; color: #a0aec0; font-size: 12px; }.empty-state i { color: #bdcddd; font-size: 29px; }.empty-state.small { min-height: 180px; }
.data-note { margin: 15px 2px 0; color: #9aa9ba; font-size: 11px; line-height: 1.7; }.feedback-card { margin-top: 18px; min-height: 140px; padding: 27px; display: flex; align-items: center; gap: 17px; border: 1px solid #e5ebf3; border-radius: 13px; background: #fff; color: #526b86; }.feedback-card > i { font-size: 25px; color: #e29c50; }.feedback-card strong { color: #29425f; }.feedback-card p { margin: 7px 0 0; font-size: 12px; }.feedback-card .el-button { margin-left: auto; }.initial-loading { justify-content: center; min-height: 220px; }.initial-loading i { color: #4d8bd4; }
@media (max-width: 1280px) { .metrics-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }.analysis-grid, .detail-grid { grid-template-columns: 1fr; }.decision-body { justify-content: center; gap: 55px; } }
@media (max-width: 700px) { .situation-page { padding: 12px; }.overview-header { padding: 22px; align-items: flex-start; flex-direction: column; }.header-copy h1 { font-size: 24px; }.header-actions { width: 100%; justify-content: space-between; }.update-info { text-align: left; }.metrics-grid { grid-template-columns: 1fr; gap: 10px; }.metric-card { min-height: 135px; }.decision-body { gap: 12px; padding: 12px; }.donut-wrap { width: 145px; height: 145px; flex-basis: 145px; }.decision-stats { width: 125px; }.panel-heading { padding: 18px 18px 0; } }
</style>
