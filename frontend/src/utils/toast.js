// 轻量 Apple 风顶部浮出 toast
let root = null

function ensureRoot() {
  if (!root) {
    root = document.createElement('div')
    root.id = 'toast-root'
    document.body.appendChild(root)
  }
  return root
}

export function toast(msg, type = 'info', duration = 2600) {
  const el = document.createElement('div')
  el.className = `toast ${type}`
  const dot = document.createElement('span')
  dot.className = 'dot'
  const text = document.createElement('span')
  text.textContent = msg
  el.appendChild(dot)
  el.appendChild(text)
  ensureRoot().appendChild(el)
  requestAnimationFrame(() => requestAnimationFrame(() => el.classList.add('show')))
  setTimeout(() => {
    el.classList.remove('show')
    setTimeout(() => el.remove(), 300)
  }, duration)
}

toast.success = (m, d) => toast(m, 'success', d)
toast.error = (m, d) => toast(m, 'error', d)
toast.info = (m, d) => toast(m, 'info', d)
toast.warning = (m, d) => toast(m, 'warning', d)
