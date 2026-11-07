<template>
  <main class="password-page">
    <el-card class="password-card" shadow="never">
      <h1>修改密码</h1>
      <el-alert v-if="required" title="密码已到期或已重置，请修改后继续使用。" type="warning" :closable="false" />
      <p>{{ passwordMessage }}，不能与当前或曾用密码相同。</p>
      <el-form ref="form" :model="form" :rules="rules" label-width="90px" @submit.native.prevent="save">
        <el-form-item label="当前密码" prop="oldPassword">
          <el-input v-model="form.oldPassword" type="password" autocomplete="current-password" maxlength="128" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="form.newPassword" type="password" autocomplete="new-password" maxlength="64" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" autocomplete="new-password" maxlength="64" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" native-type="submit" :loading="saving">保存</el-button>
          <el-button v-if="!required" :disabled="saving" @click="back">返回</el-button>
          <el-button type="text" :disabled="saving" @click="logout">退出登录</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </main>
</template>

<script>
import { passwordStatus, changePassword } from '@/api/account'
import { validPassword, passwordMessage } from '@/utils/password'

export default {
  name: 'AccountPassword',
  data() {
    return {
      passwordMessage,
      required: this.$store.state.user.passwordChangeRequired,
      saving: false,
      form: { oldPassword: '', newPassword: '', confirmPassword: '' },
      rules: {
        oldPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
        newPassword: [{ validator: (rule, value, done) => done(validPassword(value) ? undefined : new Error(passwordMessage)), trigger: 'blur' }],
        confirmPassword: [{ validator: (rule, value, done) => done(value && value === this.form.newPassword ? undefined : new Error('两次输入的密码不一致')), trigger: 'blur' }]
      }
    }
  },
  created() {
    passwordStatus().then(res => {
      this.required = !!res.changeRequired
      this.$store.commit('SET_PASSWORD_CHANGE', this.required)
    }).catch(() => {})
  },
  beforeDestroy() { this.form = { oldPassword: '', newPassword: '', confirmPassword: '' } },
  methods: {
    save() {
      if (this.saving) return
      this.$refs.form.validate(async valid => {
        if (!valid) return
        this.saving = true
        try {
          await changePassword({ oldPassword: this.form.oldPassword, newPassword: this.form.newPassword })
          this.clear()
          this.required = false
          this.$store.commit('SET_PASSWORD_CHANGE', false)
          this.$message.success('密码已修改')
          // 重新载入身份和路由，避免首次登录时只加载了改密页面。
          location.replace('/')
        } catch (error) {
          this.clear()
        } finally {
          this.saving = false
        }
      })
    },
    clear() {
      this.form = { oldPassword: '', newPassword: '', confirmPassword: '' }
      this.$nextTick(() => this.$refs.form.clearValidate())
    },
    back() { this.$router.push(this.$store.getters.roles.includes('admin') ? '/' : '/knowledge') },
    logout() { this.$store.dispatch('LogOut').finally(() => location.replace('/login')) }
  }
}
</script>

<style scoped>
.password-page { min-height: 100vh; padding: 8vh 20px; background: #f4f7fb; }
.password-card { max-width: 560px; margin: 0 auto; padding: 16px; border-radius: 10px; }
h1 { margin: 0 0 24px; color: #263b53; font-size: 24px; }
p { margin: 20px 0 28px; color: #606b7a; font-size: 14px; line-height: 1.8; }
</style>
