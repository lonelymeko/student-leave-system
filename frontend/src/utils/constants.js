// 枚举映射 — 与接口文档保持一致

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
  CANCEL_PENDING: { text: '销假待确认', pill: 'pill-teal' },
  COMPLETED: { text: '已完成', pill: 'pill-gray' }
}

export const statusText = v => STATUS_MAP[v]?.text || v || '-'
export const statusPill = v => STATUS_MAP[v]?.pill || 'pill-gray'

export const ROLE_MAP = {
  STUDENT: { text: '学生', pill: 'pill-blue' },
  TEACHER: { text: '辅导员', pill: 'pill-teal' },
  LEADER: { text: '副书记', pill: 'pill-orange' },
  ADMIN: { text: '管理员', pill: 'pill-purple' }
}

export const ACTION_MAP = {
  SUBMIT: { text: '提交申请', color: 'var(--accent)' },
  APPROVE: { text: '审批通过', color: 'var(--green)' },
  REJECT: { text: '审批驳回', color: 'var(--red)' },
  REVOKE: { text: '撤回申请', color: 'var(--text-2)' },
  CANCEL_APPLY: { text: '申请销假', color: 'var(--teal)' },
  CANCEL_CONFIRM: { text: '销假确认', color: 'var(--green)' }
}

export const RISK_MAP = {
  LOW: { text: '低风险', pill: 'pill-green' },
  MEDIUM: { text: '中风险', pill: 'pill-orange' },
  HIGH: { text: '高风险', pill: 'pill-red' }
}

// "2026-07-13 08:00:00" -> "07-13 08:00"
export const fmtTime = (s, withYear = false) => {
  if (!s) return '-'
  const str = String(s).replace('T', ' ')
  return withYear ? str.slice(0, 16) : str.slice(5, 16)
}

// datetime-local value ("2026-07-13T08:00") -> "2026-07-13 08:00:00"
export const toApiTime = v => (v ? v.replace('T', ' ') + ':00' : '')
// api time -> datetime-local value
export const toLocalInput = v => (v ? String(v).replace(' ', 'T').slice(0, 16) : '')
