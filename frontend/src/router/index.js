import { createRouter, createWebHashHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const routes = [
  { path: '/login', name: 'Login', component: () => import('../views/Login.vue'), meta: { public: true } },
  {
    path: '/',
    component: () => import('../layout/AppLayout.vue'),
    children: [
      // 学生
      { path: 'student/leaves', name: 'MyLeaves', component: () => import('../views/student/MyLeaves.vue'), meta: { roles: ['STUDENT'], title: '我的请假' } },
      { path: 'student/new', name: 'NewLeave', component: () => import('../views/student/NewLeave.vue'), meta: { roles: ['STUDENT'], title: '新建请假' } },
      { path: 'leave/:id', name: 'LeaveDetail', component: () => import('../views/student/LeaveDetail.vue'), meta: { roles: ['STUDENT', 'TEACHER', 'LEADER', 'ADMIN'], title: '请假详情' } },
      // 辅导员
      { path: 'teacher/pending', name: 'Pending', component: () => import('../views/teacher/Pending.vue'), meta: { roles: ['TEACHER'], title: '待审批' } },
      { path: 'teacher/history', name: 'History', component: () => import('../views/teacher/History.vue'), meta: { roles: ['TEACHER'], title: '审批历史' } },
      // 副书记（二级审批）
      { path: 'leader/pending', name: 'LeaderPending', component: () => import('../views/leader/LeaderPending.vue'), meta: { roles: ['LEADER'], title: '二级审批' } },
      // 请假次数排名（辅导员 / 副书记共用）
      { path: 'ranking', name: 'Ranking', component: () => import('../views/Ranking.vue'), meta: { roles: ['TEACHER', 'LEADER'], title: '请假排名' } },
      // 管理员
      { path: 'admin/dashboard', name: 'Dashboard', component: () => import('../views/admin/Dashboard.vue'), meta: { roles: ['ADMIN'], title: '统计看板' } },
      { path: 'admin/users', name: 'Users', component: () => import('../views/admin/Users.vue'), meta: { roles: ['ADMIN'], title: '用户管理' } },
      { path: 'admin/leaves', name: 'AllLeaves', component: () => import('../views/admin/AllLeaves.vue'), meta: { roles: ['ADMIN'], title: '全部请假' } }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/login' }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

router.beforeEach(to => {
  const auth = useAuthStore()
  if (to.meta.public) {
    if (auth.isLogin) return auth.homePath
    return true
  }
  if (!auth.isLogin) return '/login'
  if (to.path === '/') return auth.homePath
  if (to.meta.roles && !to.meta.roles.includes(auth.role)) return auth.homePath
  return true
})

export default router
