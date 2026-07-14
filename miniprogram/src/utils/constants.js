// 枚举映射 — 与接口文档 / Web 端保持一致

export const LEAVE_TYPES = [
  { value: 'SICK', label: '病假' },
  { value: 'PERSONAL', label: '事假' },
  { value: 'EMERGENCY', label: '急事' },
  { value: 'OTHER', label: '其他' }
]

export const typeText = v => (LEAVE_TYPES.find(t => t.value === v)?.label || v || '-')

export const STATUS_MAP = {
  PENDING: { text: '待审批', pill: 'pill-orange' },
  LEADER_PENDING: { text: '待副书记审批', pill: 'pill-blue' },
  APPROVED: { text: '请假中', pill: 'pill-green' },
  REJECTED: { text: '已驳回', pill: 'pill-red' },
  REVOKED: { text: '已撤回', pill: 'pill-gray' },
  CANCEL_PENDING: { text: '销假待确认', pill: 'pill-blue' },
  COMPLETED: { text: '已完成', pill: 'pill-gray' }
}

export const statusText = v => STATUS_MAP[v]?.text || v || '-'
export const statusPill = v => STATUS_MAP[v]?.pill || 'pill-gray'

export const ROLE_MAP = {
  STUDENT: { text: '学生', pill: 'pill-blue' },
  TEACHER: { text: '辅导员', pill: 'pill-teal' },
  LEADER: { text: '副书记', pill: 'pill-purple' },
  ADMIN: { text: '管理员', pill: 'pill-purple' }
}

// 时间线动作 — 颜色用十六进制（Icon/时间线圆点要拼进内联样式/SVG，不能用 CSS 变量）
export const ACTION_MAP = {
  SUBMIT: { text: '提交申请', color: '#0071e3' },
  APPROVE: { text: '审批通过', color: '#34c759' },
  REJECT: { text: '审批驳回', color: '#ff3b30' },
  REVOKE: { text: '撤回申请', color: '#6e6e73' },
  CANCEL_APPLY: { text: '申请销假', color: '#30b0c7' },
  CANCEL_CONFIRM: { text: '销假确认', color: '#34c759' }
}

export const RISK_MAP = {
  LOW: { text: '低风险', pill: 'pill-green' },
  MEDIUM: { text: '中风险', pill: 'pill-orange' },
  HIGH: { text: '高风险', pill: 'pill-red' }
}

// "2026-07-13 08:00:00" -> "07-13 08:00"（withYear -> "2026-07-13 08:00"）
export const fmtTime = (s, withYear = false) => {
  if (!s) return '-'
  const str = String(s).replace('T', ' ')
  return withYear ? str.slice(0, 16) : str.slice(5, 16)
}

// Date -> "yyyy-MM-dd"
export const fmtDate = d => {
  const p = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`
}
