<template>
  <div class="knowledge-page">
    <!-- 左侧导航 -->
    <aside class="knowledge-sidebar">
      <div class="brand">
        <div class="brand-mark">
          <i class="el-icon-connection"></i>
        </div>

        <div>
          <div class="brand-name">大模型向量知识库</div>
          <div class="brand-en">VECTOR KNOWLEDGE</div>
        </div>
      </div>

      <el-button
        class="new-chat-btn"
        icon="el-icon-plus"
        @click="newConversation"
      >
        新建对话
      </el-button>

      <div class="sidebar-section">
        <div class="section-caption">最近使用</div>

        <div
          v-for="item in history"
          :key="item.id"
          class="history-item"
          @click="useHistory(item)"
        >
          <i class="el-icon-chat-line-round"></i>
          <span>{{ item.title }}</span>
        </div>

        <div v-if="history.length === 0" class="history-empty">
          暂无最近问答
        </div>
      </div>

      <div class="sidebar-user">
        <div class="avatar">
          {{ userInitial }}
        </div>

        <div class="user-text">
          <div class="user-name">{{ userName }}</div>
          <div class="user-status">
            <span class="status-dot"></span>
            已通过身份认证
          </div>
        </div>

        <el-dropdown trigger="click" @command="handleUserCommand">
          <i class="el-icon-more user-more"></i>

          <el-dropdown-menu slot="dropdown">
            <el-dropdown-item
              v-if="isAdmin"
              command="admin"
              icon="el-icon-setting"
            >
              管理后台
            </el-dropdown-item>

            <el-dropdown-item
              command="logout"
              icon="el-icon-switch-button"
            >
              退出登录
            </el-dropdown-item>
          </el-dropdown-menu>
        </el-dropdown>
      </div>
    </aside>

    <!-- 右侧主体 -->
    <main class="knowledge-main">
      <!-- 顶栏 -->
      <header class="topbar">
        <div>
          <div class="topbar-title">大模型向量知识库系统</div>
          <div class="topbar-subtitle">
            知识安全检索与智能问答服务
          </div>
        </div>

        <div class="topbar-right">
          <div class="security-state">
            <span class="security-dot"></span>
            安全连接
          </div>

          <el-popover
            placement="bottom-end"
            width="330"
            trigger="click"
          >
            <div class="permission-popover">
              <div class="permission-title">
                当前访问权限
              </div>

              <div class="permission-row">
                <span>用户</span>
                <strong>{{ userContext.userName || userName }}</strong>
              </div>

              <div class="permission-row">
                <span>所属用户组</span>
                <div class="permission-tags">
                  <el-tag
                    v-for="group in userContext.groupCodes || []"
                    :key="group"
                    size="mini"
                    effect="plain"
                  >
                    {{ group }}
                  </el-tag>

                  <span
                    v-if="!userContext.groupCodes || userContext.groupCodes.length === 0"
                    class="muted"
                  >
                    -
                  </span>
                </div>
              </div>

              <div class="permission-row">
                <span>可访问范围</span>
                <div class="permission-tags">
                  <el-tag
                    v-for="scope in userContext.scopeCodes || []"
                    :key="scope"
                    size="mini"
                    type="success"
                    effect="plain"
                  >
                    {{ scope }}
                  </el-tag>

                  <span
                    v-if="!userContext.scopeCodes || userContext.scopeCodes.length === 0"
                    class="muted"
                  >
                    -
                  </span>
                </div>
              </div>

              <div class="permission-note">
                实际文档访问结果仍由服务端权限策略实时判定。
              </div>
            </div>

            <el-button
              slot="reference"
              size="small"
              class="permission-button"
              icon="el-icon-lock"
            >
              我的权限
            </el-button>
          </el-popover>
        </div>
      </header>

      <!-- 消息区域 -->
      <section ref="messageArea" class="message-area">
        <!-- 欢迎页 -->
        <div v-if="messages.length === 0" class="welcome">
          <div class="welcome-icon">
            <i class="el-icon-reading"></i>
          </div>

          <h1>欢迎使用大模型向量知识库系统</h1>

          <p>
            基于向量检索、权限控制与大模型能力，为知识访问提供
            安全、可信、可审计的智能检索与问答服务。
          </p>

          <div class="quick-grid">
            <div
              v-for="item in quickQuestions"
              :key="item.title"
              class="quick-card"
              @click="selectQuickQuestion(item)"
            >
              <div class="quick-icon">
                <i :class="item.icon"></i>
              </div>

              <div>
                <div class="quick-title">{{ item.title }}</div>
                <div class="quick-question">{{ item.question }}</div>
              </div>

              <i class="el-icon-arrow-right quick-arrow"></i>
            </div>
          </div>

          <div class="welcome-security">
            <span>
              <i class="el-icon-circle-check"></i>
              统一身份认证
            </span>
            <span>
              <i class="el-icon-circle-check"></i>
              权限过滤
            </span>
            <span>
              <i class="el-icon-circle-check"></i>
              安全向量检索
            </span>
            <span>
              <i class="el-icon-circle-check"></i>
              全程审计
            </span>
          </div>
        </div>

        <!-- 对话 -->
        <div v-else class="conversation">
          <div
            v-for="(message, index) in messages"
            :key="index"
            class="message-row"
            :class="'message-' + message.role"
          >
            <div class="message-avatar">
              <i
                v-if="message.role === 'assistant'"
                class="el-icon-connection"
              ></i>
              <span v-else>{{ userInitial }}</span>
            </div>

            <div class="message-body">
              <div class="message-name">
                {{ message.role === 'assistant'
                  ? '知识库助手'
                  : userName }}
              </div>

              <div
                class="message-bubble"
                :class="{ 'blocked-bubble': message.blocked }"
              >
                <div class="message-content">
                  {{ message.content }}
                </div>

                <!-- 被拒绝 -->
                <div
                  v-if="message.blocked"
                  class="blocked-info"
                >
                  <i class="el-icon-lock"></i>

                  <div>
                    <strong>当前请求受到访问限制</strong>
                    <span>
                      系统已根据当前身份、用户组和安全策略完成权限校验，
                      未授权内容不会向您展示。
                    </span>
                  </div>
                </div>

                <!-- 授权来源 -->
                <div
                  v-if="message.sources && message.sources.length"
                  class="source-section"
                >
                  <div class="source-title">
                    <i class="el-icon-document"></i>
                    授权知识来源
                  </div>

                  <div
                    v-for="(source, sourceIndex) in message.sources"
                    :key="sourceIndex"
                    class="source-card"
                  >
                    <div class="source-main">
                      <div class="source-name">
                        {{ source.title || '知识文档' }}
                      </div>

                      <div class="source-tags">
                        <el-tag
                          v-if="source.level"
                          size="mini"
                          type="warning"
                          effect="plain"
                        >
                          {{ source.level }}
                        </el-tag>

                        <el-tag
                          v-if="source.scopeCode"
                          size="mini"
                          type="success"
                          effect="plain"
                        >
                          {{ source.scopeCode }}
                        </el-tag>

                        <el-tag
                          size="mini"
                          type="success"
                        >
                          已授权
                        </el-tag>
                      </div>
                    </div>

                    <div
                      v-if="source.content"
                      class="source-content"
                    >
                      {{ source.content }}
                    </div>
                  </div>
                </div>

                <!-- 安全信息 -->
                <div
                  v-if="message.security"
                  class="security-summary"
                >
                  <div class="security-summary-left">
                    <i
                      :class="
                        message.blocked
                          ? 'el-icon-warning-outline'
                          : 'el-icon-circle-check'
                      "
                    ></i>

                    <span>
                      {{
                        message.blocked
                          ? '请求已执行安全策略'
                          : '本次回答已通过权限过滤'
                      }}
                    </span>

                    <span class="summary-divider">·</span>

                    <span>已记录审计日志</span>
                  </div>

                  <div class="security-summary-right">
                    <span v-if="message.security.passed !== undefined">
                      授权结果 {{ message.security.passed }} 条
                    </span>

                    <span
                      v-if="message.security.blocked > 0"
                      class="blocked-count"
                    >
                      已过滤 {{ message.security.blocked }} 条
                    </span>

                    <span v-if="message.security.costTime">
                      {{ message.security.costTime }} ms
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 正在检索 -->
          <div v-if="loading" class="message-row message-assistant">
            <div class="message-avatar">
              <i class="el-icon-connection"></i>
            </div>

            <div class="message-body">
              <div class="message-name">知识库助手</div>

              <div class="message-bubble loading-bubble">
                <span class="thinking-dot"></span>
                <span class="thinking-dot"></span>
                <span class="thinking-dot"></span>
                <span class="thinking-text">
                  正在进行安全检索与权限校验
                </span>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- 输入区 -->
      <footer class="composer-wrapper">
        <div class="composer">
          <el-input
            ref="questionInput"
            v-model="question"
            type="textarea"
            :rows="1"
            resize="none"
            maxlength="1000"
            placeholder="输入您的问题，Enter 发送，Shift + Enter 换行"
            @keydown.native="handleKeydown"
          />

          <el-button
            class="send-button"
            type="primary"
            :loading="loading"
            :disabled="!question.trim()"
            @click="sendQuestion"
          >
            <i
              v-if="!loading"
              class="el-icon-position"
            ></i>
          </el-button>
        </div>

        <div class="composer-note">
          大模型生成内容仅供参考 · 所有知识访问均经过权限校验并记录审计
        </div>
      </footer>
    </main>
  </div>
</template>

<script>
import {
  ragSearch,
  getRagUserContext
} from '@/api/rag/search'

export default {
  name: 'KnowledgePortal',

  data() {
    return {
      question: '',
      loading: false,
      messages: [],
      userContext: {},
      history: [],
      quickQuestions: [
        {
          title: '查询知识',
          question: '帮我查询当前权限范围内的知识文档',
          icon: 'el-icon-document'
        },
        {
          title: '我的可访问内容',
          question: '查看当前账号的用户组、知悉范围和授权信息',
          icon: 'el-icon-lock',
          type: 'permission'
        },
        {
          title: '项目资料检索',
          question: '帮我检索项目相关的资料和内容',
          icon: 'el-icon-folder-opened'
        },
        {
          title: '安全规范查询',
          question: '查询与信息安全相关的规范和要求',
          icon: 'el-icon-circle-check'
        }
      ]
    }
  },

  computed: {
    userName() {
      return this.$store.getters.name || '用户'
    },

    userInitial() {
      return this.userName
        ? this.userName.substring(0, 1).toUpperCase()
        : 'U'
    },

    isAdmin() {
      const roles = this.$store.getters.roles || []
      return roles.indexOf('admin') !== -1
    }
  },

  created() {
    this.loadContext()
    this.loadHistory()
  },

  methods: {
    loadContext() {
      getRagUserContext()
        .then(response => {
          this.userContext = response.data || {}
        })
        .catch(() => {
          this.userContext = {}
        })
    },

    loadHistory() {
      try {
        const value = localStorage.getItem(
          'vectorKnowledgeQuestionHistory'
        )

        this.history = value
          ? JSON.parse(value)
          : []
      } catch (e) {
        this.history = []
      }
    },

    saveHistory(query) {
      const item = {
        id: Date.now(),
        title:
          query.length > 22
            ? query.substring(0, 22) + '...'
            : query,
        query: query
      }

      this.history = [
        item,
        ...this.history.filter(
          historyItem => historyItem.query !== query
        )
      ].slice(0, 8)

      localStorage.setItem(
        'vectorKnowledgeQuestionHistory',
        JSON.stringify(this.history)
      )
    },

    useHistory(item) {
      this.question = item.query

      this.$nextTick(() => {
        this.$refs.questionInput.focus()
      })
    },

    newConversation() {
      this.messages = []
      this.question = ''

      this.$nextTick(() => {
        this.$refs.questionInput.focus()
      })
    },

    selectQuickQuestion(item) {
      if (item.type === 'permission') {
        this.showPermissionSummary()
        return
      }

      this.question = item.question
      this.sendQuestion()
    },

    showPermissionSummary() {
      const query = '查看我的可访问内容'

      this.messages.push({
        role: 'user',
        content: query
      })

      this.saveHistory(query)
      this.scrollToBottom()

      getRagUserContext()
        .then(response => {
          const context = response.data || {}

          this.userContext = context

          const groups =
            context.groupCodes &&
            context.groupCodes.length
              ? context.groupCodes.join('、')
              : '暂无用户组'

          const scopes =
            context.scopeCodes &&
            context.scopeCodes.length
              ? context.scopeCodes.join('、')
              : '暂无可访问范围'

          const policyCount =
            context.policyCount === undefined ||
            context.policyCount === null
              ? 0
              : context.policyCount

          const content =
            '已读取当前账号的权限摘要：\n\n' +
            '当前用户：' +
            (context.userName || this.userName) +
            '\n' +
            '所属用户组：' +
            groups +
            '\n' +
            '可访问范围：' +
            scopes +
            '\n' +
            '已关联策略：' +
            policyCount +
            ' 条\n\n' +
            '实际文档访问权限会在每次检索时由服务端根据当前身份、用户组、知悉范围和安全策略实时判定。'

          this.messages.push({
            role: 'assistant',
            content: content,
            blocked: false,
            sources: [],
            security: null
          })

          this.scrollToBottom()
        })
        .catch(() => {
          this.messages.push({
            role: 'assistant',
            content: '当前无法获取账号权限摘要，请稍后重新尝试。',
            blocked: false,
            sources: [],
            security: null
          })

          this.scrollToBottom()
        })
    },

    handleKeydown(event) {
      if (event.key === 'Enter' && !event.shiftKey) {
        event.preventDefault()
        this.sendQuestion()
      }
    },

    sendQuestion() {
      const query = this.question.trim()

      if (!query || this.loading) {
        return
      }

      this.messages.push({
        role: 'user',
        content: query
      })

      this.saveHistory(query)
      this.question = ''
      this.loading = true
      this.scrollToBottom()

      ragSearch({
        query: query,
        topK: 5,
        useRemote: true
      })
        .then(response => {
          const data =
            response && response.data
              ? response.data
              : {}

          const blocked =
            data.allowAccess === false

          let content = ''

          if (blocked) {
            content =
              '当前问题涉及您现有授权范围之外的内容。系统已完成权限校验，未授权知识不会向您展示。'
          } else {
            content =
              data.answer ||
              '已完成知识检索，但当前没有生成可展示的回答。'
          }

          const sources = (
            data.filteredResults || []
          ).map(item => ({
            title:
              item.title ||
              item.fileName ||
              item.docName ||
              item.docId,
            level:
              item.level ||
              item.securityLevel ||
              item.docLevel,
            scopeCode:
              item.scopeCode,
            content:
              item.content || ''
          }))

          this.messages.push({
            role: 'assistant',
            content: content,
            blocked: blocked,
            sources: sources,
            security: {
              passed:
                data.filteredResultCount || 0,
              blocked:
                data.rejectedResultCount || 0,
              costTime:
                data.costTime || 0
            }
          })

          if (data.groupCodes || data.scopeCodes) {
            this.userContext = Object.assign(
              {},
              this.userContext,
              {
                groupCodes:
                  data.groupCodes ||
                  this.userContext.groupCodes,
                scopeCodes:
                  data.scopeCodes ||
                  this.userContext.scopeCodes
              }
            )
          }
        })
        .catch(() => {
          this.messages.push({
            role: 'assistant',
            content:
              '当前知识服务暂时无法完成请求，请稍后重新尝试。',
            blocked: false,
            sources: [],
            security: null
          })
        })
        .finally(() => {
          this.loading = false
          this.scrollToBottom()
        })
    },

    scrollToBottom() {
      this.$nextTick(() => {
        const element = this.$refs.messageArea

        if (element) {
          element.scrollTop =
            element.scrollHeight
        }
      })
    },

    handleUserCommand(command) {
      if (command === 'admin') {
        this.$router.push('/')
        return
      }

      if (command === 'logout') {
        this.$confirm(
          '确认退出当前账号吗？',
          '退出登录',
          {
            type: 'warning',
            confirmButtonText: '退出',
            cancelButtonText: '取消'
          }
        )
          .then(() => {
            return this.$store.dispatch(
              'LogOut'
            )
          })
          .then(() => {
            this.$router.replace('/login')
          })
          .catch(() => {})
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.knowledge-page {
  display: flex;
  width: 100%;
  height: 100vh;
  min-width: 1000px;
  background: #f6f8fb;
  color: #1f2d3d;
}

.knowledge-sidebar {
  width: 270px;
  height: 100vh;
  flex-shrink: 0;
  background: #132238;
  padding: 24px 18px 18px;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
}

.brand {
  height: 58px;
  display: flex;
  align-items: center;
  padding: 0 8px;
  color: #fff;
}

.brand-mark {
  width: 38px;
  height: 38px;
  margin-right: 11px;
  border-radius: 10px;
  background: linear-gradient(
    135deg,
    #2f80ed,
    #31b8a4
  );
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}

.brand-name {
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 0.5px;
}

.brand-en {
  margin-top: 3px;
  font-size: 9px;
  color: #8397af;
  letter-spacing: 1.4px;
}

.new-chat-btn {
  width: 100%;
  margin-top: 24px;
  height: 42px;
  border-color: rgba(255, 255, 255, 0.15);
  background: rgba(255, 255, 255, 0.07);
  color: #eef5ff;
}

.new-chat-btn:hover {
  background: rgba(71, 145, 255, 0.16);
  border-color: #3f8cff;
  color: #fff;
}

.sidebar-section {
  margin-top: 27px;
  flex: 1;
  min-height: 0;
}

.section-caption {
  padding: 0 10px 9px;
  font-size: 12px;
  color: #71869f;
}

.history-item {
  height: 40px;
  padding: 0 10px;
  margin-bottom: 3px;
  border-radius: 7px;
  display: flex;
  align-items: center;
  color: #bac7d5;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.history-item i {
  margin-right: 9px;
  color: #788ca4;
}

.history-item span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.history-item:hover {
  color: #fff;
  background: rgba(255, 255, 255, 0.07);
}

.history-empty {
  padding: 10px;
  font-size: 12px;
  color: #63788f;
}

.sidebar-user {
  height: 60px;
  padding: 10px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  display: flex;
  align-items: center;
}

.avatar {
  width: 36px;
  height: 36px;
  flex-shrink: 0;
  border-radius: 9px;
  background: #2e80ed;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 14px;
  font-weight: 600;
}

.user-text {
  flex: 1;
  min-width: 0;
  margin-left: 10px;
}

.user-name {
  font-size: 13px;
  color: #f1f5fa;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-status {
  margin-top: 4px;
  color: #7e91a7;
  font-size: 10px;
}

.status-dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  margin-right: 4px;
  border-radius: 50%;
  background: #39c487;
}

.user-more {
  color: #8ca0b6;
  cursor: pointer;
}

.knowledge-main {
  min-width: 0;
  flex: 1;
  height: 100vh;
  display: flex;
  flex-direction: column;
}

.topbar {
  height: 74px;
  padding: 0 34px;
  flex-shrink: 0;
  background: rgba(255, 255, 255, 0.95);
  border-bottom: 1px solid #e8edf3;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.topbar-title {
  font-size: 17px;
  font-weight: 600;
  color: #25364a;
}

.topbar-subtitle {
  margin-top: 4px;
  font-size: 11px;
  color: #99a6b5;
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 17px;
}

.security-state {
  font-size: 12px;
  color: #64758a;
}

.security-dot {
  display: inline-block;
  width: 7px;
  height: 7px;
  margin-right: 6px;
  border-radius: 50%;
  background: #36bf83;
}

.permission-button {
  border-color: #dce5ee;
  color: #52677d;
}

.message-area {
  flex: 1;
  overflow-y: auto;
  scroll-behavior: smooth;
}

.welcome {
  width: 780px;
  max-width: calc(100% - 80px);
  margin: 8vh auto 0;
  text-align: center;
}

.welcome-icon {
  width: 68px;
  height: 68px;
  margin: 0 auto 22px;
  border-radius: 20px;
  background: linear-gradient(
    135deg,
    #e9f2ff,
    #e6f8f4
  );
  color: #347fd9;
  font-size: 29px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.welcome h1 {
  margin: 0;
  font-size: 28px;
  font-weight: 600;
  color: #1e3148;
}

.welcome > p {
  width: 610px;
  max-width: 100%;
  margin: 15px auto 30px;
  color: #7a899a;
  font-size: 14px;
  line-height: 1.9;
}

.quick-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 13px;
  text-align: left;
}

.quick-card {
  min-height: 88px;
  padding: 17px;
  box-sizing: border-box;
  background: #fff;
  border: 1px solid #e6ecf2;
  border-radius: 11px;
  display: flex;
  align-items: center;
  cursor: pointer;
  transition: all 0.2s;
}

.quick-card:hover {
  border-color: #b7d1ef;
  box-shadow: 0 7px 24px rgba(31, 67, 105, 0.07);
  transform: translateY(-1px);
}

.quick-icon {
  width: 38px;
  height: 38px;
  margin-right: 13px;
  flex-shrink: 0;
  border-radius: 9px;
  background: #f0f6fd;
  color: #397ec6;
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 17px;
}

.quick-title {
  color: #33485e;
  font-size: 14px;
  font-weight: 500;
}

.quick-question {
  margin-top: 6px;
  color: #9aa7b5;
  font-size: 11px;
}

.quick-arrow {
  margin-left: auto;
  color: #bec8d2;
}

.welcome-security {
  margin-top: 28px;
  color: #8c9bab;
  font-size: 11px;
  display: flex;
  justify-content: center;
  gap: 26px;
}

.welcome-security i {
  margin-right: 4px;
  color: #47b98a;
}

.conversation {
  width: 850px;
  max-width: calc(100% - 70px);
  margin: 0 auto;
  padding: 37px 0 32px;
}

.message-row {
  display: flex;
  margin-bottom: 28px;
}

.message-avatar {
  width: 36px;
  height: 36px;
  flex-shrink: 0;
  border-radius: 9px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.message-user {
  flex-direction: row-reverse;
}

.message-user .message-avatar {
  margin-left: 12px;
  background: #367fd3;
  color: #fff;
}

.message-assistant .message-avatar {
  margin-right: 12px;
  background: #eaf2fb;
  color: #347bc6;
}

.message-user .message-body {
  align-items: flex-end;
}

.message-body {
  max-width: calc(100% - 48px);
  display: flex;
  flex-direction: column;
}

.message-name {
  margin-bottom: 7px;
  color: #98a6b4;
  font-size: 11px;
}

.message-bubble {
  max-width: 730px;
  border-radius: 12px;
  border: 1px solid #e6ebf1;
  background: #fff;
  overflow: hidden;
}

.message-user .message-bubble {
  background: #337fd1;
  border-color: #337fd1;
}

.message-content {
  padding: 15px 18px;
  color: #35495d;
  font-size: 14px;
  line-height: 1.85;
  white-space: pre-wrap;
}

.message-user .message-content {
  color: #fff;
}

.blocked-bubble {
  border-color: #f1d3ce;
}

.blocked-info {
  margin: 0 16px 15px;
  padding: 13px;
  border-radius: 8px;
  background: #fff7f5;
  display: flex;
  color: #b35e51;
}

.blocked-info > i {
  margin: 2px 9px 0 0;
}

.blocked-info strong {
  display: block;
  font-size: 12px;
}

.blocked-info span {
  display: block;
  margin-top: 4px;
  color: #9f7a74;
  font-size: 11px;
  line-height: 1.6;
}

.source-section {
  margin: 4px 16px 16px;
  padding-top: 13px;
  border-top: 1px solid #edf1f5;
}

.source-title {
  margin-bottom: 10px;
  color: #6c7f92;
  font-size: 11px;
  font-weight: 500;
}

.source-title i {
  margin-right: 5px;
}

.source-card {
  padding: 11px 12px;
  margin-top: 7px;
  border-radius: 8px;
  background: #f8fafc;
  border: 1px solid #edf1f5;
}

.source-main {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.source-name {
  min-width: 0;
  color: #42576d;
  font-size: 12px;
  font-weight: 500;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.source-tags {
  display: flex;
  margin-left: 10px;
  gap: 5px;
}

.source-content {
  margin-top: 8px;
  color: #8492a2;
  font-size: 11px;
  line-height: 1.6;
  display: -webkit-box;
  overflow: hidden;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.security-summary {
  padding: 10px 16px;
  border-top: 1px solid #edf1f5;
  background: #fafbfd;
  color: #8796a5;
  font-size: 10px;
  display: flex;
  justify-content: space-between;
  gap: 20px;
}

.security-summary-left,
.security-summary-right {
  display: flex;
  align-items: center;
  gap: 7px;
}

.security-summary-left i {
  color: #46b686;
}

.summary-divider {
  color: #c6cdd4;
}

.blocked-count {
  color: #c68358;
}

.loading-bubble {
  min-width: 260px;
  padding: 16px 18px;
  display: flex;
  align-items: center;
}

.thinking-dot {
  width: 6px;
  height: 6px;
  margin-right: 4px;
  border-radius: 50%;
  background: #65a3df;
  animation: thinking 1.2s infinite ease-in-out;
}

.thinking-dot:nth-child(2) {
  animation-delay: 0.15s;
}

.thinking-dot:nth-child(3) {
  animation-delay: 0.3s;
}

.thinking-text {
  margin-left: 8px;
  color: #8798aa;
  font-size: 12px;
}

@keyframes thinking {
  0%,
  80%,
  100% {
    opacity: 0.3;
  }

  40% {
    opacity: 1;
  }
}

.composer-wrapper {
  padding: 13px 35px 17px;
  flex-shrink: 0;
  background: linear-gradient(
    to bottom,
    rgba(246, 248, 251, 0),
    #f6f8fb 22%
  );
}

.composer {
  width: 850px;
  max-width: 100%;
  min-height: 60px;
  margin: 0 auto;
  padding: 9px 9px 9px 18px;
  border: 1px solid #dfe6ed;
  border-radius: 14px;
  background: #fff;
  display: flex;
  align-items: center;
  box-shadow: 0 7px 25px rgba(27, 52, 78, 0.06);
}

.composer ::v-deep .el-textarea__inner {
  padding: 9px 0;
  min-height: 40px !important;
  max-height: 120px;
  border: 0;
  box-shadow: none;
  font-size: 14px;
}

.send-button {
  width: 43px;
  height: 43px;
  margin-left: 12px;
  flex-shrink: 0;
  padding: 0;
  border-radius: 11px;
}

.send-button i {
  font-size: 17px;
}

.composer-note {
  margin-top: 8px;
  text-align: center;
  color: #a0aab5;
  font-size: 10px;
}

.permission-title {
  margin-bottom: 14px;
  color: #33495f;
  font-size: 14px;
  font-weight: 600;
}

.permission-row {
  padding: 9px 0;
  border-bottom: 1px solid #f0f2f5;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 15px;
  color: #8997a5;
  font-size: 12px;
}

.permission-row strong {
  color: #44596e;
}

.permission-tags {
  text-align: right;
}

.permission-tags .el-tag {
  margin: 0 0 4px 4px;
}

.permission-note {
  margin-top: 13px;
  color: #a0acb8;
  font-size: 10px;
  line-height: 1.6;
}

.muted {
  color: #aab5c0;
}
</style>
