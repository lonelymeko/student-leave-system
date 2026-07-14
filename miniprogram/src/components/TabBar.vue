<script setup>
// 自绘 Apple 风底部导航（不用原生 tabBar）
// 学生：请假 / 新建 / 我的；辅导员：待办 / 历史 / 我的；副书记：二级审批 / 排名 / 我的
import { computed } from 'vue'
import AppIcon from './AppIcon.vue'
import { getUser } from '../utils/auth'

const props = defineProps({
  current: { type: String, required: true } // leaves | new | pending | history | mine
})

const ACCENT = '#0071e3'
const GRAY = '#8e8e93'

const STUDENT_TABS = [
  { key: 'leaves', label: '请假', icon: 'list', url: '/pages/student/leaves' },
  { key: 'new', label: '新建', icon: 'plus', url: '/pages/student/new' },
  { key: 'mine', label: '我的', icon: 'user', url: '/pages/mine/mine' }
]
const TEACHER_TABS = [
  { key: 'pending', label: '待办', icon: 'inbox', url: '/pages/teacher/pending' },
  { key: 'history', label: '历史', icon: 'history', url: '/pages/teacher/history' },
  { key: 'mine', label: '我的', icon: 'user', url: '/pages/mine/mine' }
]
const LEADER_TABS = [
  { key: 'leader-pending', label: '二级审批', icon: 'inbox', url: '/pages/leader/pending' },
  { key: 'ranking', label: '排名', icon: 'trophy', url: '/pages/leader/ranking' },
  { key: 'mine', label: '我的', icon: 'user', url: '/pages/mine/mine' }
]

const tabs = computed(() => {
  const role = getUser()?.role
  if (role === 'TEACHER') return TEACHER_TABS
  if (role === 'LEADER') return LEADER_TABS
  return STUDENT_TABS
})

function go(tab) {
  if (tab.key === props.current) return
  // 非原生 tabBar：用 redirectTo 平级切换，避免页面栈无限增长
  uni.redirectTo({ url: tab.url })
}
</script>

<template>
  <view class="tabbar">
    <view
      v-for="t in tabs"
      :key="t.key"
      class="tab-item"
      :class="{ active: t.key === current }"
      @click="go(t)"
    >
      <AppIcon :name="t.icon" :size="22" :color="t.key === current ? ACCENT : GRAY" />
      <text class="tab-label" :style="{ color: t.key === current ? ACCENT : GRAY }">{{ t.label }}</text>
    </view>
  </view>
</template>

<style scoped>
.tabbar {
  position: fixed;
  left: 0; right: 0; bottom: 0;
  z-index: 900;
  display: flex;
  background: rgba(255, 255, 255, .88);
  backdrop-filter: saturate(180%) blur(20px);
  -webkit-backdrop-filter: saturate(180%) blur(20px);
  border-top: .5px solid rgba(0, 0, 0, .08);
  padding-bottom: env(safe-area-inset-bottom);
  box-shadow: 0 -4px 24px rgba(0, 0, 0, .04);
}
.tab-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  padding: 8px 0 6px;
  transition: transform .15s ease-out;
}
.tab-item:active { transform: scale(.94); }
.tab-label { font-size: 10.5px; font-weight: 600; }
</style>
