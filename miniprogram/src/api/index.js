import { get, post, put } from '../utils/request'

// ---- 认证 ----
export const login = data => post('/auth/login', data)
export const getMe = () => get('/auth/me')
export const wxEnabled = () => get('/auth/wx-enabled')
export const wxLogin = code => post('/auth/wx-login', { code })
export const wxBind = data => post('/auth/wx-bind', data)

// ---- 学生端 ----
export const submitLeave = data => post('/leave', data)
export const getMyLeaves = params => get('/leave/my', params)
export const getLeaveDetail = id => get(`/leave/${id}`)
export const revokeLeave = id => put(`/leave/${id}/revoke`)
export const cancelApply = (id, data) => post(`/leave/${id}/cancel-apply`, data)

// ---- 辅导员端 ----
export const getPending = params => get('/approval/pending', params)
export const getApprovalHistory = params => get('/approval/history', params)
export const approve = (leaveId, data) => post(`/approval/${leaveId}`, data)
export const cancelConfirm = leaveId => post(`/approval/${leaveId}/cancel-confirm`)

// ---- 副书记端（LEADER）----
export const getLeaderPending = params => get('/approval/leader-pending', params)
export const getRanking = () => get('/approval/ranking')

// ---- AI ----
export const aiDraft = text => post('/ai/draft', { text })
export const aiApprovalAdvice = leaveId => post('/ai/approval-advice', { leaveId })
export const aiChat = message => post('/ai/chat', { message })
