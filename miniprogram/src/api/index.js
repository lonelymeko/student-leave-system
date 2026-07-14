import { get, post, put, del } from '../utils/request'

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

// ---- 消息通知 ----
export const getNotifications = params => get('/notifications', params)
export const getUnreadCount = () => get('/notifications/unread-count')
export const readNotification = id => put(`/notifications/${id}/read`)

// ---- 请假附件 ----
export const getLeaveAttachments = id => get(`/leave/${id}/attachments`)

// ---- 管理员端（ADMIN）----
export const getStatsOverview = () => get('/admin/stats/overview')
export const getUsers = params => get('/admin/users', params)
export const createUser = data => post('/admin/users', data)
export const updateUser = (id, data) => put(`/admin/users/${id}`, data)
export const resetPassword = (id, password) => put(`/admin/users/${id}/password`, { password })
export const deleteUser = id => del(`/admin/users/${id}`)
export const getTeachers = () => get('/admin/teachers')
export const getAllLeaves = params => get('/admin/leaves', params)
export const getConfigs = () => get('/admin/configs')
export const updateConfig = (key, value) => put(`/admin/configs/${encodeURIComponent(key)}`, { value })

// ---- AI ----
export const aiDraft = text => post('/ai/draft', { text })
export const aiApprovalAdvice = leaveId => post('/ai/approval-advice', { leaveId })
export const aiChat = message => post('/ai/chat', { message })
