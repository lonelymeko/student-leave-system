import { defineStore } from 'pinia'
import { getUnreadCount } from '../api'

export const useNotifyStore = defineStore('notify', {
  state: () => ({
    unread: 0,
    _timer: null
  }),
  actions: {
    async refresh() {
      try {
        const data = await getUnreadCount()
        this.unread = Number(data?.count || 0)
      } catch (e) { /* 静默：铃铛不打扰 */ }
    },
    // 本地即时递减（标记单条已读后调用，避免等下一次轮询）
    decrement(n = 1) {
      this.unread = Math.max(0, this.unread - n)
    },
    reset() {
      this.unread = 0
    },
    startPolling(interval = 30000) {
      this.stopPolling()
      this.refresh()
      this._timer = setInterval(() => this.refresh(), interval)
    },
    stopPolling() {
      if (this._timer) { clearInterval(this._timer); this._timer = null }
    }
  }
})
