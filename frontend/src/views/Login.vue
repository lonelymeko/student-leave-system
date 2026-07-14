<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import Icon from '../components/Icon.vue'
import { toast } from '../utils/toast'

const router = useRouter()
const auth = useAuthStore()

const username = ref('')
const password = ref('')
const loading = ref(false)

async function submit() {
  if (!username.value.trim() || !password.value) {
    toast.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    await auth.login(username.value.trim(), password.value)
    toast.success(`欢迎回来，${auth.user?.realName || auth.user?.username}`)
    router.push(auth.homePath)
  } catch (e) {
    // 拦截器已提示
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <div class="bg-orb orb-1"></div>
    <div class="bg-orb orb-2"></div>
    <div class="bg-orb orb-3"></div>

    <div class="login-card card">
      <div class="logo"><Icon name="calendar" :size="26" /></div>
      <h1 class="title">学生请销假系统</h1>
      <p class="subtitle">Student Leave Management</p>

      <form class="form" @submit.prevent="submit">
        <div class="field">
          <div class="input-wrap">
            <Icon name="user" :size="16" class="input-icon" />
            <input v-model="username" class="input with-icon" placeholder="用户名" autocomplete="username" />
          </div>
        </div>
        <div class="field">
          <div class="input-wrap">
            <Icon name="lock" :size="16" class="input-icon" />
            <input v-model="password" type="password" class="input with-icon" placeholder="密码" autocomplete="current-password" />
          </div>
        </div>
        <button class="btn btn-primary btn-lg btn-block" type="submit" :disabled="loading">
          <span v-if="!loading">登 录</span>
          <span v-else class="logging"><span class="spinner small"></span>登录中…</span>
        </button>
      </form>

      <p class="demo-hint">
        演示账号：student1 / teacher1 / leader1（密码 123456）· admin（密码 admin123）
      </p>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  background: var(--bg);
}
.bg-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: .5;
}
.orb-1 { width: 420px; height: 420px; background: #a7c8ff; top: -120px; left: -80px; }
.orb-2 { width: 380px; height: 380px; background: #d8c6ff; bottom: -100px; right: -60px; }
.orb-3 { width: 300px; height: 300px; background: #b8f0d0; bottom: 10%; left: 22%; }

.login-card {
  position: relative;
  z-index: 1;
  width: 400px;
  max-width: calc(100vw - 40px);
  padding: 44px 40px 30px;
  display: flex;
  flex-direction: column;
  align-items: center;
  background: rgba(255, 255, 255, .68);
}
.logo {
  width: 58px; height: 58px;
  border-radius: 15px;
  background: var(--accent);
  color: #fff;
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 8px 24px rgba(0, 113, 227, .4);
  margin-bottom: 18px;
}
.title { font-size: 25px; font-weight: 700; letter-spacing: -.4px; }
.subtitle { font-size: 13px; color: var(--text-2); margin: 4px 0 28px; letter-spacing: .3px; }

.form { width: 100%; display: flex; flex-direction: column; gap: 14px; }
.input-wrap { position: relative; }
.input-icon {
  position: absolute; left: 13px; top: 50%; transform: translateY(-50%);
  color: var(--text-2);
}
.input.with-icon { padding-left: 38px; }

.logging { display: inline-flex; align-items: center; gap: 8px; }
.spinner.small { width: 15px; height: 15px; border-width: 2px; border-color: rgba(255,255,255,.4); border-top-color: #fff; }

.demo-hint {
  margin-top: 24px;
  font-size: 12px;
  color: var(--text-2);
  text-align: center;
  line-height: 1.7;
}
</style>
