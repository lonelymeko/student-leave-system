import request from './request'

// ---- 认证 ----
export const login = data => request.post('/auth/login', data)
export const getMe = () => request.get('/auth/me')

// ---- 学生端 ----
export const submitLeave = data => request.post('/leave', data)
export const getMyLeaves = params => request.get('/leave/my', { params })
export const getLeaveDetail = id => request.get(`/leave/${id}`)
export const revokeLeave = id => request.put(`/leave/${id}/revoke`)
export const cancelApply = (id, data) => request.post(`/leave/${id}/cancel-apply`, data)

// ---- 辅导员端 ----
export const getPending = params => request.get('/approval/pending', { params })
export const getApprovalHistory = params => request.get('/approval/history', { params })
export const approve = (leaveId, data) => request.post(`/approval/${leaveId}`, data)
export const cancelConfirm = leaveId => request.post(`/approval/${leaveId}/cancel-confirm`)

// ---- 副书记端 / 二级审批 ----
export const getLeaderPending = params => request.get('/approval/leader-pending', { params })
export const getRanking = () => request.get('/approval/ranking')
// 导出 Excel：返回原始 axios response（含 headers），responseType blob
export const exportLeaves = status =>
  request.get('/approval/leaves/export', {
    params: status ? { status } : {},
    responseType: 'blob',
    rawResponse: true
  })

// ---- 消息通知（全角色） ----
export const getNotifications = params => request.get('/notifications', { params })
export const getUnreadCount = () => request.get('/notifications/unread-count')
export const readNotification = id => request.put(`/notifications/${id}/read`)

// ---- 请假附件 ----
export const uploadAttachment = (id, file) => {
  const fd = new FormData()
  fd.append('file', file)
  // 不手动设 Content-Type，交由浏览器自动带 multipart boundary
  return request.post(`/leave/${id}/attachment`, fd)
}
export const getAttachments = id => request.get(`/leave/${id}/attachments`)

// ---- 系统配置（管理端） ----
export const getConfigs = () => request.get('/admin/configs')
export const updateConfig = (key, value) => request.put(`/admin/configs/${encodeURIComponent(key)}`, { value })

// ---- 管理端 ----
export const getUsers = params => request.get('/admin/users', { params })
export const createUser = data => request.post('/admin/users', data)
export const updateUser = (id, data) => request.put(`/admin/users/${id}`, data)
export const resetPassword = (id, password) => request.put(`/admin/users/${id}/password`, { password })
export const deleteUser = id => request.delete(`/admin/users/${id}`)
export const getTeachers = () => request.get('/admin/teachers')
export const getAllLeaves = params => request.get('/admin/leaves', { params })
export const getStatsOverview = () => request.get('/admin/stats/overview')

// ---- AI ----
export const aiDraft = text => request.post('/ai/draft', { text })
export const aiApprovalAdvice = leaveId => request.post('/ai/approval-advice', { leaveId })
export const aiChat = message => request.post('/ai/chat', { message })
