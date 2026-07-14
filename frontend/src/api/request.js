import axios from 'axios'
import { toast } from '../utils/toast'

const instance = axios.create({
  baseURL: '/api',
  timeout: 60000
})

instance.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

function goLogin() {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  if (!location.hash.includes('/login')) {
    location.hash = '#/login'
  }
}

instance.interceptors.response.use(
  res => {
    // 文件流等场景：调用方需要完整 response（含 headers）
    if (res.config?.rawResponse) return res
    const body = res.data
    if (body && typeof body === 'object' && 'code' in body) {
      if (body.code === 0) return body.data
      if (body.code === 401) {
        toast.error(body.msg || '登录已失效，请重新登录')
        goLogin()
        return Promise.reject(Object.assign(new Error(body.msg || '未登录'), { code: 401, silent: true }))
      }
      // 5001 AI 不可用：交由调用方决定提示方式，不全局 toast
      const err = Object.assign(new Error(body.msg || '请求失败'), { code: body.code })
      if (body.code !== 5001) toast.error(body.msg || '请求失败')
      else err.silent = true
      return Promise.reject(err)
    }
    return body
  },
  error => {
    if (error.response) {
      const { status, data } = error.response
      if (status === 401 || data?.code === 401) {
        toast.error('登录已失效，请重新登录')
        goLogin()
        return Promise.reject(Object.assign(new Error('未登录'), { code: 401, silent: true }))
      }
      const msg = data?.msg || `请求失败 (${status})`
      const code = data?.code ?? status
      const err = Object.assign(new Error(msg), { code })
      if (code !== 5001) toast.error(msg)
      else err.silent = true
      return Promise.reject(err)
    }
    toast.error('网络异常，请检查后端服务是否启动')
    return Promise.reject(Object.assign(error, { code: -1 }))
  }
)

export default instance
