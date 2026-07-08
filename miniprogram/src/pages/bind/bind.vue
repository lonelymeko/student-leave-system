<script setup>
// 微信首次登录 → 绑定校内账号（wx-login 返回 4010 + bindTicket 后进入本页）
import { ref } from 'vue'
import { wxBind } from '../../api'
import AppIcon from '../../components/AppIcon.vue'
import { setAuth, setWxLinked, takeBindTicket, clearBindTicket, homePath } from '../../utils/auth'

const username = ref('')
const password = ref('')
const loading = ref(false)
const focusU = ref(false)
const focusP = ref(false)

async function submit() {
  if (loading.value) return
  if (!username.value.trim() || !password.value) {
    uni.showToast({ title: '请输入学号账号和密码', icon: 'none' })
    return
  }
  const ticket = takeBindTicket()
  if (!ticket) {
    uni.showToast({ title: '绑定票据已失效，请重新微信登录', icon: 'none' })
    uni.reLaunch({ url: '/pages/login/login' })
    return
  }
  loading.value = true
  try {
    const d = await wxBind({ ticket, username: username.value.trim(), password: password.value })
    if (d?.user?.role === 'ADMIN') {
      uni.showToast({ title: '管理员请使用网页端登录', icon: 'none' })
      return
    }
    clearBindTicket()
    setAuth(d.token, d.user)
    setWxLinked(true)
    uni.showToast({ title: '绑定成功，已登录', icon: 'none' })
    uni.reLaunch({ url: homePath(d.user.role) })
  } catch (e) {
    // 票据过期等：拦截器已 toast；无效票据回登录页重新走微信登录
    if (String(e?.msg || '').includes('票据')) {
      clearBindTicket()
      setTimeout(() => uni.reLaunch({ url: '/pages/login/login' }), 800)
    }
  } finally {
    loading.value = false
  }
}

function back() {
  uni.navigateBack({ fail: () => uni.reLaunch({ url: '/pages/login/login' }) })
}
</script>

<template>
  <view class="page-wrap bind-page">
    <view class="subnav" @click="back">
      <AppIcon name="chevron-left" :size="16" color="#0071e3" />
      <text>返回登录</text>
    </view>

    <view class="bind-card card card-pad">
      <view class="bind-logo">
        <AppIcon name="wechat" :size="26" color="#ffffff" />
      </view>
      <view class="bind-title">绑定校内账号</view>
      <view class="bind-desc">首次使用微信登录，请绑定你的学号账号。绑定一次后，下次微信一键登录即可直接进入。</view>

      <view class="form">
        <view class="field">
          <text class="field-label">学号 / 用户名</text>
          <input
            v-model="username"
            class="input"
            :class="{ 'input-focus': focusU }"
            placeholder="如：student1"
            placeholder-class="ph"
            @focus="focusU = true" @blur="focusU = false"
          />
        </view>
        <view class="field">
          <text class="field-label">密码</text>
          <input
            v-model="password"
            class="input"
            :class="{ 'input-focus': focusP }"
            password
            placeholder="账号密码"
            placeholder-class="ph"
            @confirm="submit"
            @focus="focusP = true" @blur="focusP = false"
          />
        </view>
        <view class="btn btn-primary btn-lg btn-block" :class="{ disabled: loading }" style="margin-top: 6px" @click="submit">
          <view v-if="loading" class="spinner white" />
          <text>{{ loading ? '绑定中…' : '绑定并登录' }}</text>
        </view>
      </view>
    </view>
  </view>
</template>

<style scoped>
.bind-page { padding-bottom: 40px; }
.bind-card {
  margin-top: 14px;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 32px;
  padding-bottom: 30px;
}
.bind-logo {
  width: 56px; height: 56px;
  border-radius: 15px;
  background: #34c759;
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 8px 24px rgba(52, 199, 89, .35);
  margin-bottom: 14px;
}
.bind-title { font-size: 20px; font-weight: 700; }
.bind-desc {
  font-size: 13px; color: var(--text-2); text-align: center;
  line-height: 1.7; margin: 8px 0 24px; padding: 0 6px;
}
.form { width: 100%; display: flex; flex-direction: column; gap: 14px; }
</style>
