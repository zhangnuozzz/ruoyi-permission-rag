<template>
  <div class="knowledge-login">
    <div class="login-shell">
      <!-- 左侧品牌区 -->
      <section class="login-intro">
        <div class="intro-brand">
          <div class="brand-icon">
            <i class="el-icon-connection"></i>
          </div>

          <div>
            <div class="brand-cn">
              大模型向量知识库系统
            </div>
            <div class="brand-en">
              VECTOR KNOWLEDGE SYSTEM
            </div>
          </div>
        </div>

        <div class="intro-content">
          <div class="intro-label">
            VECTOR KNOWLEDGE SERVICE
          </div>

          <h1>
            让知识访问<br />
            <span>安全、可信、可审计</span>
          </h1>

          <p>
            面向知识检索场景的大模型向量库服务，
            统一提供身份认证、权限过滤、向量检索、
            智能问答与安全审计能力。
          </p>

          <div class="feature-grid">
            <div class="feature-item">
              <i class="el-icon-key"></i>
              <div>
                <strong>统一身份认证</strong>
                <span>账号与权限统一管理</span>
              </div>
            </div>

            <div class="feature-item">
              <i class="el-icon-lock"></i>
              <div>
                <strong>细粒度权限控制</strong>
                <span>授权范围实时判定</span>
              </div>
            </div>

            <div class="feature-item">
              <i class="el-icon-search"></i>
              <div>
                <strong>安全向量检索</strong>
                <span>检索结果二次过滤</span>
              </div>
            </div>

            <div class="feature-item">
              <i class="el-icon-document"></i>
              <div>
                <strong>全链路审计</strong>
                <span>访问行为完整留痕</span>
              </div>
            </div>
          </div>
        </div>

        <div class="intro-footer">
          <div>大模型向量库项目 · 安全知识服务入口</div>
          <div class="designer-line">
            实现：zhangnuozzz · fufu
          </div>
        </div>
      </section>

      <!-- 右侧登录区 -->
      <section class="login-panel">
        <div class="login-card">
          <div class="login-heading">
            <h2>欢迎登录</h2>
            <p>
              使用您的组织账号进入知识服务系统
            </p>
          </div>

          <el-form
            ref="loginForm"
            :model="loginForm"
            :rules="loginRules"
          >
            <div class="field-label">
              账号
            </div>

            <el-form-item prop="username">
              <el-input
                v-model="loginForm.username"
                auto-complete="off"
                placeholder="请输入账号"
                class="enterprise-input"
              >
                <i
                  slot="prefix"
                  class="el-icon-user input-prefix"
                ></i>
              </el-input>
            </el-form-item>

            <div class="field-label">
              密码
            </div>

            <el-form-item prop="password">
              <el-input
                v-model="loginForm.password"
                type="password"
                auto-complete="off"
                placeholder="请输入密码"
                class="enterprise-input"
                @keyup.enter.native="handleLogin"
              >
                <i
                  slot="prefix"
                  class="el-icon-lock input-prefix"
                ></i>
              </el-input>
            </el-form-item>

            <div class="field-label">
              安全验证
            </div>

            <el-form-item prop="code">
              <div class="captcha-row">
                <el-input
                  v-model="loginForm.code"
                  auto-complete="off"
                  placeholder="请输入验证码"
                  class="enterprise-input captcha-input"
                  @keyup.enter.native="handleLogin"
                >
                  <i
                    slot="prefix"
                    class="el-icon-circle-check input-prefix"
                  ></i>
                </el-input>

                <div
                  class="captcha-image"
                  title="点击刷新验证码"
                  @click="getCode"
                >
                  <img
                    :src="codeUrl"
                    alt="验证码"
                  />
                </div>
              </div>
            </el-form-item>

            <div class="login-options">
              <el-checkbox
                v-model="loginForm.rememberMe"
              >
                记住登录状态
              </el-checkbox>

              <span class="security-tip">
                <i class="el-icon-lock"></i>
                安全登录
              </span>
            </div>

            <el-button
              :loading="loading"
              type="primary"
              class="login-button"
              @click.native.prevent="handleLogin"
            >
              <span v-if="!loading">
                登 录
              </span>

              <span v-else>
                正在验证身份...
              </span>
            </el-button>
          </el-form>

          <div class="login-notice">
            <i class="el-icon-info"></i>
            登录即表示您的访问行为将按照组织安全策略进行权限校验与审计。
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script>
import {
  getCodeImg
} from '@/api/login'

import Cookies from 'js-cookie'

import {
  encrypt,
  decrypt
} from '@/utils/jsencrypt'

export default {
  name: 'Login',

  data() {
    return {
      codeUrl: '',
      cookiePassword: '',

      loginForm: {
        username: '',
        password: '',
        rememberMe: false,
        code: '',
        uuid: ''
      },

      loginRules: {
        username: [
          {
            required: true,
            trigger: 'blur',
            message: '请输入账号'
          }
        ],

        password: [
          {
            required: true,
            trigger: 'blur',
            message: '请输入密码'
          }
        ],

        code: [
          {
            required: true,
            trigger: 'change',
            message: '请输入验证码'
          }
        ]
      },

      loading: false,
      redirect: undefined
    }
  },

  watch: {
    $route: {
      handler(route) {
        this.redirect =
          route.query &&
          route.query.redirect
      },

      immediate: true
    }
  },

  created() {
    this.getCode()
    this.getCookie()
  },

  methods: {
    getCode() {
      getCodeImg().then(res => {
        this.codeUrl =
          'data:image/gif;base64,' +
          res.img

        this.loginForm.uuid =
          res.uuid
      })
    },

    getCookie() {
      const username =
        Cookies.get('username')

      const password =
        Cookies.get('password')

      const rememberMe =
        Cookies.get('rememberMe')

      if (username !== undefined) {
        this.loginForm.username =
          username
      }

      if (password !== undefined) {
        this.loginForm.password =
          decrypt(password)
      }

      this.loginForm.rememberMe =
        rememberMe === undefined
          ? false
          : rememberMe === 'true'
    },

    handleLogin() {
      this.$refs.loginForm.validate(
        valid => {
          if (!valid) {
            return
          }

          this.loading = true

          if (
            this.loginForm.rememberMe
          ) {
            Cookies.set(
              'username',
              this.loginForm.username,
              { expires: 30 }
            )

            Cookies.set(
              'password',
              encrypt(
                this.loginForm.password
              ),
              { expires: 30 }
            )

            Cookies.set(
              'rememberMe',
              this.loginForm.rememberMe,
              { expires: 30 }
            )
          } else {
            Cookies.remove('username')
            Cookies.remove('password')
            Cookies.remove('rememberMe')
          }

          this.$store
            .dispatch(
              'Login',
              this.loginForm
            )
            .then(() => {
              this.$router.push({
                path:
                  this.redirect ||
                  '/'
              })
            })
            .catch(() => {
              this.loading = false
              this.getCode()
            })
        }
      )
    }
  }
}
</script>

<style lang="scss">
.knowledge-login {
  width: 100%;
  height: 100vh;
  min-width: 1040px;
  background: #f4f7fb;
}

.login-shell {
  width: 100%;
  height: 100%;
  display: flex;
}

.login-intro {
  position: relative;
  width: 56%;
  min-width: 600px;
  height: 100%;
  padding: 47px 58px;
  box-sizing: border-box;
  color: #fff;
  overflow: hidden;
  background:
    radial-gradient(
      circle at 82% 14%,
      rgba(38, 175, 165, 0.22),
      transparent 30%
    ),
    radial-gradient(
      circle at 12% 88%,
      rgba(55, 129, 230, 0.22),
      transparent 31%
    ),
    linear-gradient(
      145deg,
      #10233c,
      #152c49 62%,
      #153b51
    );
}

.login-intro::before {
  content: '';
  position: absolute;
  right: -100px;
  bottom: -130px;
  width: 420px;
  height: 420px;
  border: 1px solid rgba(255,255,255,0.06);
  border-radius: 50%;
}

.login-intro::after {
  content: '';
  position: absolute;
  right: -20px;
  bottom: -55px;
  width: 260px;
  height: 260px;
  border: 1px solid rgba(255,255,255,0.05);
  border-radius: 50%;
}

.intro-brand {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
}

.brand-icon {
  width: 44px;
  height: 44px;
  margin-right: 13px;
  border-radius: 11px;
  background: linear-gradient(
    135deg,
    #438ce6,
    #3fc3aa
  );
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 23px;
}

.brand-cn {
  font-size: 18px;
  font-weight: 600;
  letter-spacing: 1px;
}

.brand-en {
  margin-top: 4px;
  color: #7089a4;
  font-size: 9px;
  letter-spacing: 1.5px;
}

.intro-content {
  position: relative;
  z-index: 1;
  width: 610px;
  max-width: 92%;
  margin-top: 16vh;
}

.intro-label {
  color: #59b9c2;
  font-size: 11px;
  letter-spacing: 2px;
}

.intro-content h1 {
  margin: 14px 0 18px;
  font-size: 39px;
  line-height: 1.4;
  font-weight: 500;
  letter-spacing: 1px;
}

.intro-content h1 span {
  color: #83cde0;
}

.intro-content > p {
  width: 530px;
  max-width: 100%;
  color: #91a5ba;
  line-height: 1.9;
  font-size: 14px;
}

.feature-grid {
  margin-top: 39px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 13px 22px;
}

.feature-item {
  height: 62px;
  padding: 0 13px;
  border: 1px solid rgba(
    255,
    255,
    255,
    0.07
  );
  border-radius: 8px;
  background: rgba(
    255,
    255,
    255,
    0.035
  );
  display: flex;
  align-items: center;
}

.feature-item > i {
  width: 31px;
  height: 31px;
  margin-right: 10px;
  border-radius: 7px;
  background: rgba(
    61,
    148,
    218,
    0.14
  );
  color: #6eb5e5;
  display: flex;
  align-items: center;
  justify-content: center;
}

.feature-item strong {
  display: block;
  font-size: 12px;
  color: #d9e4ef;
  font-weight: 500;
}

.feature-item span {
  display: block;
  margin-top: 4px;
  color: #6d849c;
  font-size: 10px;
}

.intro-footer {
  position: absolute;
  left: 58px;
  bottom: 35px;
  color: #566f89;
  font-size: 10px;
  letter-spacing: 0.8px;
}

.login-panel {
  flex: 1;
  min-width: 440px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f8fafc;
}

.login-card {
  width: 390px;
}

.login-heading {
  margin-bottom: 34px;
}

.login-heading h2 {
  margin: 0;
  color: #22384f;
  font-size: 27px;
  font-weight: 600;
}

.login-heading p {
  margin: 10px 0 0;
  color: #95a3b1;
  font-size: 12px;
}

.field-label {
  margin: 0 0 8px 2px;
  color: #68798a;
  font-size: 11px;
  font-weight: 500;
}

.enterprise-input .el-input__inner {
  height: 46px;
  border-radius: 8px;
  border-color: #dfe6ed;
  background: #fff;
}

.enterprise-input .el-input__inner:focus {
  border-color: #458ad4;
}

.input-prefix {
  height: 46px;
  line-height: 46px;
  color: #9ba8b6;
}

.captcha-row {
  display: flex;
  gap: 10px;
}

.captcha-input {
  flex: 1;
}

.captcha-image {
  width: 120px;
  height: 46px;
  border: 1px solid #dfe6ed;
  border-radius: 8px;
  overflow: hidden;
  background: #fff;
  cursor: pointer;
}

.captcha-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.login-options {
  margin: -3px 0 25px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.login-options .el-checkbox__label {
  color: #77889a;
  font-size: 11px;
}

.security-tip {
  color: #7f93a6;
  font-size: 10px;
}

.security-tip i {
  margin-right: 4px;
  color: #41a87d;
}

.login-button {
  width: 100%;
  height: 47px;
  border-radius: 8px;
  border-color: #347fc8;
  background: #347fc8;
  font-size: 14px;
  letter-spacing: 2px;
}

.login-button:hover {
  border-color: #2e75bc;
  background: #2e75bc;
}

.login-notice {
  margin-top: 22px;
  padding: 13px 14px;
  border-radius: 7px;
  background: #f0f5f9;
  color: #91a0af;
  font-size: 10px;
  line-height: 1.6;
}

.login-notice i {
  margin-right: 5px;
  color: #658db4;
}

.designer-line {
  margin-top: 7px;
  color: #48647f;
  font-size: 9px;
  letter-spacing: 0.5px;
}
</style>
