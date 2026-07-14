import { defineStore } from 'pinia'
import { login as apiLogin } from '../api'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    user: JSON.parse(localStorage.getItem('user') || 'null')
  }),
  getters: {
    isLogin: s => !!s.token,
    role: s => s.user?.role || '',
    homePath: s => {
      switch (s.user?.role) {
        case 'STUDENT': return '/student/leaves'
        case 'TEACHER': return '/teacher/pending'
        case 'LEADER': return '/leader/pending'
        case 'ADMIN': return '/admin/dashboard'
        default: return '/login'
      }
    }
  },
  actions: {
    async login(username, password) {
      const data = await apiLogin({ username, password })
      this.token = data.token
      this.user = data.user
      localStorage.setItem('token', data.token)
      localStorage.setItem('user', JSON.stringify(data.user))
      return data.user
    },
    logout() {
      this.token = ''
      this.user = null
      localStorage.removeItem('token')
      localStorage.removeItem('user')
    }
  }
})
