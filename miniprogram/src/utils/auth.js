// 登录态：token / user 存 storage；wxLinked 标记本设备是否走过微信登录/绑定

const TOKEN_KEY = 'token'
const USER_KEY = 'user'
const WX_LINKED_KEY = 'wxLinked'
const BIND_TICKET_KEY = 'bindTicket'

export const getToken = () => uni.getStorageSync(TOKEN_KEY) || ''
export const getUser = () => {
  const u = uni.getStorageSync(USER_KEY)
  return u && typeof u === 'object' ? u : null
}

export function setAuth(token, user) {
  uni.setStorageSync(TOKEN_KEY, token)
  uni.setStorageSync(USER_KEY, user)
}

export function clearAuth() {
  uni.removeStorageSync(TOKEN_KEY)
  uni.removeStorageSync(USER_KEY)
}

export const setWxLinked = v => uni.setStorageSync(WX_LINKED_KEY, !!v)
export const isWxLinked = () => !!uni.getStorageSync(WX_LINKED_KEY)

export const setBindTicket = t => uni.setStorageSync(BIND_TICKET_KEY, t)
export const takeBindTicket = () => {
  const t = uni.getStorageSync(BIND_TICKET_KEY)
  return t || ''
}
export const clearBindTicket = () => uni.removeStorageSync(BIND_TICKET_KEY)

// 各角色首页（小程序只做学生端 + 辅导员端，管理员请用 Web 端）
export function homePath(role) {
  if (role === 'TEACHER') return '/pages/teacher/pending'
  return '/pages/student/leaves'
}

export function goLogin() {
  clearAuth()
  uni.reLaunch({ url: '/pages/login/login' })
}
