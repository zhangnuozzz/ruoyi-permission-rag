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
              向量库访问控制系统
            </div>
          </div>
        </div>

        <div class="intro-content">

          <h1>
            让知识访问<br />
            <span>安全、可信、可审计</span>
          </h1>

          <div class="feature-grid">
            <div class="feature-item">
              <i class="el-icon-key"></i>
              <div>
                <strong>统一身份认证</strong>
              </div>
            </div>

            <div class="feature-item">
              <i class="el-icon-lock"></i>
              <div>
                <strong>细粒度权限控制</strong>
              </div>
            </div>

            <div class="feature-item">
              <i class="el-icon-search"></i>
              <div>
                <strong>安全向量检索</strong>
              </div>
            </div>

            <div class="feature-item">
              <i class="el-icon-document"></i>
              <div>
                <strong>全链路审计</strong>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- 右侧登录区 -->
      <section class="login-panel">
        <div class="login-card">
          <div class="login-heading">
            <h2>欢迎登录</h2>
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
                记住账号
              </el-checkbox>
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


export default {
  name: 'Login',

  data() {
    return {
      codeUrl: '',

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

      Cookies.remove('password')

      const rememberMe =
        Cookies.get('rememberMe')

      if (username !== undefined) {
        this.loginForm.username =
          username
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
              this.loginForm.password = ''
              this.$router.push({
                path:
                  this.redirect ||
                  '/'
              })
            })
            .catch(() => {
              this.loading = false
              this.loginForm.password = ''
              this.loginForm.code = ''
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
  min-height: 100vh;
  min-width: 320px;
  background: #f4f7fb;
}

.login-shell {
  width: 100%;
  min-height: 100vh;
  display: flex;
}

.login-intro {
  position: relative;
  width: 54%;
  min-width: 0;
  min-height: 100vh;
  padding: 40px 48px;
  display: flex;
  flex-direction: column;
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


.intro-content {
  position: relative;
  z-index: 1;
  width: 610px;
  max-width: 100%;
  margin: auto 0;
  padding: 40px 0;
}


.intro-content h1 {
  margin: 0;
  font-size: 39px;
  line-height: 1.4;
  font-weight: 500;
  letter-spacing: 1px;
}

.intro-content h1 span {
  color: #83cde0;
}


.feature-grid {
  margin-top: 32px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 13px 22px;
}

.feature-item {
  min-height: 72px;
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
  font-size: 14px;
  color: #d9e4ef;
  font-weight: 500;
}



.login-panel {
  flex: 1;
  min-width: 0;
  padding: 40px 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f8fafc;
}

.login-card {
  width: 390px;
  max-width: 100%;
}

.login-heading {
  margin-bottom: 30px;
}

.login-heading h2 {
  margin: 0;
  color: #22384f;
  font-size: 27px;
  font-weight: 600;
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



@media (max-width: 1100px) {
  .login-intro { padding: 32px; }
  .intro-content h1 { font-size: 31px; }
  .feature-grid { gap: 12px; }
  .feature-item { padding: 0 10px; }
  .feature-item strong { font-size: 13px; }
}

@media (max-width: 760px) {
  .login-shell { flex-direction: column; }
  .login-intro { width: 100%; min-height: auto; padding: 24px; }
  .intro-content { display: none; }
  .login-panel { flex: 1; padding: 36px 24px; }
}
</style>
