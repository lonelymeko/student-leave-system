<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import Icon from '../components/Icon.vue'
import AiAssistant from '../components/AiAssistant.vue'
import { ROLE_MAP } from '../utils/constants'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

const MENUS = {
  STUDENT: [
    { path: '/student/leaves', label: '我的请假', icon: 'list' },
    { path: '/student/new', label: '新建请假', icon: 'plus' }
  ],
  TEACHER: [
    { path: '/teacher/pending', label: '待审批', icon: 'clock' },
    { path: '/teacher/history', label: '审批历史', icon: 'history' }
  ],
  ADMIN: [
    { path: '/admin/dashboard', label: '统计看板', icon: 'chart' },
    { path: '/admin/users', label: '用户管理', icon: 'users' },
    { path: '/admin/leaves', label: '全部请假', icon: 'list' }
  ]
}

const menus = computed(() => MENUS[auth.role] || [])
const roleInfo = computed(() => ROLE_MAP[auth.role] || { text: '', pill: 'pill-gray' })

function logout() {
  auth.logout()
  router.push('/login')
}
</script>

<template>
  <div class="layout">
    <aside class="sidebar">
      <div class="brand">
        <div class="brand-icon"><Icon name="calendar" :size="19" /></div>
        <span>请销假系统</span>
      </div>

      <nav class="nav">
        <router-link
          v-for="m in menus"
          :key="m.path"
          :to="m.path"
          class="nav-item"
          :class="{ active: route.path === m.path }"
        >
          <Icon :name="m.icon" :size="18" />
          <span>{{ m.label }}</span>
        </router-link>
      </nav>

      <div class="sidebar-bottom">
        <div class="me">
          <div class="me-avatar"><Icon name="user" :size="17" /></div>
          <div class="me-info">
            <div class="me-name">{{ auth.user?.realName || auth.user?.username }}</div>
            <span class="pill" :class="roleInfo.pill" style="font-size:11px;padding:1px 8px">{{ roleInfo.text }}</span>
          </div>
        </div>
        <button class="nav-item logout" type="button" @click="logout">
          <Icon name="logout" :size="18" />
          <span>退出登录</span>
        </button>
      </div>
    </aside>

    <main class="main">
      <router-view v-slot="{ Component }">
        <Transition name="page" mode="out-in">
          <component :is="Component" :key="route.fullPath" />
        </Transition>
      </router-view>
    </main>

    <AiAssistant />
  </div>
</template>

<style scoped>
.layout { display: flex; height: 100%; }

.sidebar {
  width: 232px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  background: var(--surface);
  backdrop-filter: var(--blur);
  -webkit-backdrop-filter: var(--blur);
  border-right: .5px solid var(--separator);
  padding: 22px 14px 18px;
}
.brand {
  display: flex; align-items: center; gap: 10px;
  font-size: 16.5px; font-weight: 700; letter-spacing: -.2px;
  padding: 0 8px 22px;
}
.brand-icon {
  width: 34px; height: 34px;
  border-radius: 9px;
  background: var(--accent);
  color: #fff;
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 4px 12px rgba(0, 113, 227, .35);
}

.nav { display: flex; flex-direction: column; gap: 3px; flex: 1; }
.nav-item {
  display: flex; align-items: center; gap: 11px;
  padding: 9px 12px;
  border-radius: var(--radius-md);
  color: var(--text-1);
  text-decoration: none;
  font-size: 14.5px;
  font-weight: 500;
  border: none;
  background: transparent;
  cursor: pointer;
  font-family: var(--font-stack);
  transition: all .16s ease-out;
  width: 100%;
}
.nav-item:hover { background: rgba(0, 0, 0, .045); }
.nav-item.active { background: var(--accent); color: #fff; box-shadow: 0 3px 10px rgba(0, 113, 227, .3); }

.sidebar-bottom { display: flex; flex-direction: column; gap: 6px; }
.me {
  display: flex; align-items: center; gap: 10px;
  padding: 10px 8px;
  border-top: .5px solid var(--separator);
  margin-top: 8px;
}
.me-avatar {
  width: 34px; height: 34px;
  border-radius: 50%;
  background: rgba(0, 0, 0, .06);
  color: var(--text-2);
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.me-info { display: flex; flex-direction: column; gap: 2px; min-width: 0; }
.me-name { font-size: 13.5px; font-weight: 600; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.logout { color: var(--text-2); }
.logout:hover { color: var(--red); background: var(--red-soft); }

.main {
  flex: 1;
  overflow-y: auto;
  padding: 36px 40px 60px;
}
</style>
