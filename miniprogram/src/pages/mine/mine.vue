<script setup>
// 我的：用户信息卡 + 微信绑定状态 + AI 制度问答入口 + 退出登录
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import AppIcon from '../../components/AppIcon.vue'
import TabBar from '../../components/TabBar.vue'
import SheetModal from '../../components/SheetModal.vue'
import { ROLE_MAP } from '../../utils/constants'
import { getUser, getToken, clearAuth, isWxLinked, setWxLinked } from '../../utils/auth'

const user = ref(null)
const wxLinked = ref(false)

onShow(() => {
  if (!getToken()) { uni.reLaunch({ url: '/pages/login/login' }); return }
  user.value = getUser()
  wxLinked.value = isWxLinked()
})

const roleInfo = r => ROLE_MAP[r] || { text: r, pill: 'pill-gray' }

function goChat() {
  uni.navigateTo({ url: '/pages/ai/chat' })
}

const showLogout = ref(false)
function doLogout() {
  clearAuth()
  setWxLinked(false)
  showLogout.value = false
  uni.reLaunch({ url: '/pages/login/login' })
}
</script>

<template>
  <view class="page-wrap">
    <view class="large-title" style="margin-bottom: 18px">我的</view>

    <template v-if="user">
      <!-- 用户信息卡 -->
      <view class="card card-pad user-card">
        <view class="avatar">{{ (user.realName || user.username || '?').slice(0, 1) }}</view>
        <view class="user-info">
          <view class="user-name-line">
            <text class="user-name">{{ user.realName || user.username }}</text>
            <view class="pill" :class="roleInfo(user.role).pill">{{ roleInfo(user.role).text }}</view>
          </view>
          <view class="user-sub">
            <text v-if="user.role === 'STUDENT'">{{ user.studentNo || '-' }} · {{ user.className || '-' }}</text>
            <text v-else>账号：{{ user.username }}</text>
          </view>
        </view>
      </view>

      <!-- 详细信息 -->
      <view class="card" style="margin-top: 16px">
        <view class="group-row info-row"><text class="k">用户名</text><text class="v">{{ user.username }}</text></view>
        <view v-if="user.role === 'STUDENT'" class="group-row info-row"><text class="k">学号</text><text class="v">{{ user.studentNo || '-' }}</text></view>
        <view v-if="user.role === 'STUDENT'" class="group-row info-row"><text class="k">班级</text><text class="v">{{ user.className || '-' }}</text></view>
        <view v-if="user.role === 'STUDENT'" class="group-row info-row"><text class="k">辅导员</text><text class="v">{{ user.teacherName || '-' }}</text></view>
        <view v-if="user.phone" class="group-row info-row"><text class="k">电话</text><text class="v">{{ user.phone }}</text></view>
        <view class="group-row info-row">
          <view class="k-with-icon">
            <AppIcon name="wechat" :size="16" :color="wxLinked ? '#34c759' : '#a1a1a6'" />
            <text class="k">微信绑定</text>
          </view>
          <view v-if="wxLinked" class="pill pill-green">本机已绑定</view>
          <text v-else class="v" style="color: #a1a1a6; font-weight: 400">未绑定 · 用微信一键登录即可绑定</text>
        </view>
      </view>

      <!-- 功能入口 -->
      <view class="card" style="margin-top: 16px">
        <view class="group-row entry-row" @click="goChat">
          <view class="entry-badge"><AppIcon name="sparkle" :size="16" color="#ffffff" /></view>
          <view class="entry-main">
            <view class="entry-title">AI 制度问答</view>
            <view class="entry-desc">请销假制度有疑问？问问 AI 助手</view>
          </view>
          <AppIcon name="chevron-right" :size="16" color="#c7c7cc" />
        </view>
      </view>

      <!-- 退出登录 -->
      <view class="card logout-card" @click="showLogout = true">
        <view class="group-row logout-row">
          <AppIcon name="logout" :size="17" color="#ff3b30" />
          <text class="logout-text">退出登录</text>
        </view>
      </view>
    </template>

    <SheetModal :show="showLogout" title="退出登录" @close="showLogout = false">
      <view class="confirm-text">确定退出当前账号吗？</view>
      <template #footer>
        <view class="btn btn-secondary" @click="showLogout = false">取消</view>
        <view class="btn btn-danger" @click="doLogout">退出登录</view>
      </template>
    </SheetModal>

    <TabBar current="mine" />
  </view>
</template>

<style scoped>
.user-card { display: flex; flex-direction: row; align-items: center; gap: 16px; }
.avatar {
  width: 60px; height: 60px;
  border-radius: 50%;
  background: linear-gradient(135deg, #0071e3, #30b0c7);
  color: #ffffff;
  font-size: 24px;
  font-weight: 700;
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 6px 18px rgba(0, 113, 227, .3);
  flex-shrink: 0;
}
.user-info { flex: 1; min-width: 0; }
.user-name-line { display: flex; align-items: center; gap: 10px; }
.user-name { font-size: 20px; font-weight: 700; }
.user-sub { font-size: 13px; color: var(--text-2); margin-top: 3px; }

.info-row { justify-content: space-between; gap: 16px; }
.info-row .k { font-size: 13.5px; color: var(--text-2); flex-shrink: 0; }
.info-row .v { font-size: 14px; font-weight: 500; text-align: right; }
.k-with-icon { display: flex; align-items: center; gap: 7px; }

.entry-row { gap: 12px; }
.entry-badge {
  width: 34px; height: 34px; border-radius: 10px;
  background: linear-gradient(135deg, #0071e3, #af52de);
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 4px 12px rgba(90, 100, 230, .35);
  flex-shrink: 0;
}
.entry-main { flex: 1; min-width: 0; }
.entry-title { font-size: 15px; font-weight: 600; }
.entry-desc { font-size: 12px; color: var(--text-2); }

.logout-card { margin-top: 16px; }
.logout-row { justify-content: center; gap: 8px; }
.logout-card:active { background: rgba(255, 59, 48, .05); }
.logout-text { color: var(--red); font-size: 15px; font-weight: 600; }
.confirm-text { color: var(--text-2); font-size: 14px; line-height: 1.7; text-align: center; }
</style>
