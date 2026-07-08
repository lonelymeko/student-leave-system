// uni.request 封装 — 对齐 Web 端 axios 拦截器行为：
//   code=0    -> resolve(data)
//   code=401  -> 清凭据 + toast + 回登录页（silent reject）
//   code=5001 -> AI 不可用，静默 reject 交调用方降级
//   code=4010 -> 微信未绑定，静默 reject（data.bindTicket 由登录页处理）
//   其他      -> 统一 uni.showToast

import { getToken, goLogin } from './auth'

// baseURL：
//   H5      -> 相对路径 /api（dev 由 vite 代理到 8080）
//   mp-weixin -> 本机后端；微信开发者工具需勾选「不校验合法域名」。
//               ⚠️ 上线前必须：小程序后台配置 https 的 request 合法域名，并把这里改成正式域名。
let BASE_URL = 'http://localhost:8080/api'
// #ifdef H5
BASE_URL = '/api'
// #endif

const SILENT_CODES = [5001, 4010]

/** uni.request 会把 undefined/null 值序列化成字符串 "undefined"/"null" 拼进 query，必须先清洗 */
function cleanParams(data) {
  if (!data || typeof data !== 'object' || Array.isArray(data)) return data
  const out = {}
  for (const k in data) {
    if (data[k] !== undefined && data[k] !== null) out[k] = data[k]
  }
  return out
}

export function request(method, url, data) {
  return new Promise((resolve, reject) => {
    const header = { 'Content-Type': 'application/json' }
    const token = getToken()
    if (token) header.Authorization = `Bearer ${token}`

    uni.request({
      url: BASE_URL + url,
      method,
      data: cleanParams(data),
      header,
      timeout: 60000,
      success: res => {
        const body = res.data
        if (body && typeof body === 'object' && 'code' in body) {
          if (body.code === 0) return resolve(body.data)
          if (body.code === 401 || res.statusCode === 401) {
            uni.showToast({ title: body.msg || '登录已失效，请重新登录', icon: 'none' })
            goLogin()
            return reject({ code: 401, msg: body.msg || '未登录', silent: true })
          }
          const err = { code: body.code, msg: body.msg || '请求失败', data: body.data }
          if (SILENT_CODES.includes(body.code)) {
            err.silent = true
          } else {
            uni.showToast({ title: err.msg, icon: 'none' })
          }
          return reject(err)
        }
        if (res.statusCode === 401) {
          uni.showToast({ title: '登录已失效，请重新登录', icon: 'none' })
          goLogin()
          return reject({ code: 401, msg: '未登录', silent: true })
        }
        if (res.statusCode >= 400) {
          const msg = `请求失败 (${res.statusCode})`
          uni.showToast({ title: msg, icon: 'none' })
          return reject({ code: res.statusCode, msg })
        }
        resolve(body)
      },
      fail: () => {
        uni.showToast({ title: '网络异常，请检查后端服务是否启动', icon: 'none' })
        reject({ code: -1, msg: '网络异常' })
      }
    })
  })
}

export const get = (url, params) => request('GET', url, params)
export const post = (url, data) => request('POST', url, data)
export const put = (url, data) => request('PUT', url, data)
