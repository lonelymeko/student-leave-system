<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { login, wxEnabled, wxLogin } from '../../api'
import AppIcon from '../../components/AppIcon.vue'
import { setAuth, setWxLinked, setBindTicket, homePath } from '../../utils/auth'

const username = ref('')
const password = ref('')
const loading = ref(false)
const focusU = ref(false)
const focusP = ref(false)

// 微信登录是否可用（后端未配 AppID/Secret 时隐藏微信按钮）
const wxOk = ref(false)
const wxLoading = ref(false)

onLoad(async () => {
  // 仅小程序端探测微信登录可用性；H5 无 uni.login 微信通道，恒隐藏微信按钮
  // #ifdef MP-WEIXIN
  try {
    const d = await wxEnabled()
    wxOk.value = !!d?.enabled
  } catch (e) {
    wxOk.value = false
  }
  // #endif
})

function enter(user, viaWx) {
  if (user.role === 'ADMIN') {
    uni.showToast({ title: '管理员请使用网页端登录', icon: 'none' })
    return false
  }
  setWxLinked(!!viaWx)
  uni.showToast({ title: `欢迎回来，${user.realName || user.username}`, icon: 'none' })
  uni.reLaunch({ url: homePath(user.role) })
  return true
}

// ---- 账号密码登录 ----
async function submit() {
  if (loading.value) return
  if (!username.value.trim() || !password.value) {
    uni.showToast({ title: '请输入用户名和密码', icon: 'none' })
    return
  }
  loading.value = true
  try {
    const d = await login({ username: username.value.trim(), password: password.value })
    if (d?.user?.role === 'ADMIN') {
      uni.showToast({ title: '管理员请使用网页端登录', icon: 'none' })
      return
    }
    setAuth(d.token, d.user)
    enter(d.user, false)
  } catch (e) {
    // 拦截器已提示
  } finally {
    loading.value = false
  }
}

// ---- 微信一键登录（仅小程序）----
async function doWxLogin() {
  // #ifdef MP-WEIXIN
  if (wxLoading.value) return
  wxLoading.value = true
  try {
    const { code } = await uni.login({ provider: 'weixin' })
    if (!code) {
      uni.showToast({ title: '获取微信登录凭证失败', icon: 'none' })
      return
    }
    const d = await wxLogin(code)
    if (d?.user?.role === 'ADMIN') {
      uni.showToast({ title: '管理员请使用网页端登录', icon: 'none' })
      return
    }
    setAuth(d.token, d.user)
    enter(d.user, true)
  } catch (e) {
    if (e?.code === 4010 && e?.data?.bindTicket) {
      // 微信未绑定账号 → 去绑定页（ticket 走 storage，避免超长 URL）
      setBindTicket(e.data.bindTicket)
      uni.navigateTo({ url: '/pages/bind/bind' })
    } else if (e?.code === 5002) {
      uni.showToast({ title: '微信登录暂不可用', icon: 'none' })
    }
    // 其余 code 拦截器已 toast
  } finally {
    wxLoading.value = false
  }
  // #endif
  // #ifndef MP-WEIXIN
  // H5 里 uni.login 无微信小程序通道，降级为提示（验证 h5 时不被卡住）
  uni.showToast({ title: '请在微信小程序中使用微信一键登录', icon: 'none' })
  // #endif
}
</script>

<template>
  <view class="login-page">
    <view class="bg-orb orb-1" />
    <view class="bg-orb orb-2" />
    <view class="bg-orb orb-3" />

    <view class="login-card card">
      <view class="logo">
        <AppIcon name="calendar" :size="26" color="#ffffff" />
      </view>
      <view class="title">学生请销假系统</view>
      <view class="subtitle">Student Leave Management</view>

      <!-- 微信一键登录（主按钮；后端未启用时隐藏；H5 中隐藏） -->
      <!-- #ifdef MP-WEIXIN -->
      <view v-if="wxOk" class="btn btn-primary btn-lg btn-block wx-btn" :class="{ disabled: wxLoading }" @click="doWxLogin">
        <view v-if="wxLoading" class="spinner white" />
        <AppIcon v-else name="wechat" :size="18" color="#ffffff" />
        <text>{{ wxLoading ? '登录中…' : '微信一键登录' }}</text>
      </view>
      <view v-if="wxOk" class="divider">
        <view class="line" /><text class="divider-text">或使用账号登录</text><view class="line" />
      </view>
      <!-- #endif -->

      <!-- 账号密码登录 -->
      <view class="form">
        <view class="input-wrap">
          <view class="input-icon"><AppIcon name="user" :size="16" color="#6e6e73" /></view>
          <input
            v-model="username"
            class="input with-icon"
            :class="{ 'input-focus': focusU }"
            placeholder="用户名"
            placeholder-class="ph"
            @focus="focusU = true"
            @blur="focusU = false"
          />
        </view>
        <view class="input-wrap">
          <view class="input-icon"><AppIcon name="lock" :size="16" color="#6e6e73" /></view>
          <input
            v-model="password"
            class="input with-icon"
            :class="{ 'input-focus': focusP }"
            password
            placeholder="密码"
            placeholder-class="ph"
            @confirm="submit"
            @focus="focusP = true"
            @blur="focusP = false"
          />
        </view>
        <view class="btn btn-lg btn-block" :class="[wxOk ? 'btn-secondary' : 'btn-primary', { disabled: loading }]" @click="submit">
          <view v-if="loading" class="spinner white" :style="wxOk ? 'border-color: rgba(0,0,0,.15); border-top-color: #1d1d1f' : ''" />
          <text>{{ loading ? '登录中…' : '登 录' }}</text>
        </view>
      </view>

      <view class="demo-hint">演示账号：student1 / teacher1（密码 123456）</view>
    </view>
  </view>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  background: #f5f5f7;
  padding: calc(var(--status-bar-height) + 20px) 20px 40px;
  box-sizing: border-box;
}
.bg-orb { position: absolute; border-radius: 50%; filter: blur(80px); opacity: .5; }
.orb-1 { width: 320px; height: 320px; background: #a7c8ff; top: -100px; left: -80px; }
.orb-2 { width: 300px; height: 300px; background: #d8c6ff; bottom: -90px; right: -60px; }
.orb-3 { width: 240px; height: 240px; background: #b8f0d0; bottom: 12%; left: 16%; }

.login-card {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 400px;
  padding: 40px 30px 26px;
  display: flex;
  flex-direction: column;
  align-items: center;
  background: rgba(255, 255, 255, .8);
  box-sizing: border-box;
}
.logo {
  width: 58px; height: 58px;
  border-radius: 15px;
  background: #0071e3;
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 8px 24px rgba(0, 113, 227, .4);
  margin-bottom: 16px;
}
.title { font-size: 24px; font-weight: 700; letter-spacing: -.4px; color: var(--text-1); }
.subtitle { font-size: 12.5px; color: var(--text-2); margin: 4px 0 26px; letter-spacing: .3px; }

.wx-btn { margin-bottom: 4px; }
.divider { display: flex; align-items: center; gap: 10px; width: 100%; margin: 16px 0 14px; }
.divider .line { flex: 1; height: .5px; background: var(--separator); }
.divider-text { font-size: 12px; color: var(--text-2); }

.form { width: 100%; display: flex; flex-direction: column; gap: 13px; }
.input-wrap { position: relative; }
.input-icon {
  position: absolute; left: 13px; top: 50%; transform: translateY(-50%);
  z-index: 2; display: flex; align-items: center;
}
.input.with-icon { padding-left: 38px; }

.demo-hint { margin-top: 22px; font-size: 11.5px; color: var(--text-2); text-align: center; line-height: 1.7; }
</style>
