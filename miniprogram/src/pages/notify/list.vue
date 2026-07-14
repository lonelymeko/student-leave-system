<script setup>
// 消息通知列表：未读高亮 + 下拉刷新 + 触底加载；点击标记已读，审批结果类跳请假详情
import { ref } from 'vue'
import { onShow, onPullDownRefresh, onReachBottom } from '@dcloudio/uni-app'
import { getNotifications, readNotification } from '../../api'
import AppIcon from '../../components/AppIcon.vue'
import EmptyBox from '../../components/EmptyBox.vue'
import { fmtTime } from '../../utils/constants'
import { getToken } from '../../utils/auth'

const page = ref(1)
const size = 10
const total = ref(0)
const list = ref([])
const loading = ref(false)
const finished = ref(false)

async function load(reset = false) {
  if (loading.value) return
  if (reset) { page.value = 1; finished.value = false }
  loading.value = true
  try {
    const data = await getNotifications({ page: page.value, size })
    const records = data?.records || []
    list.value = reset ? records : [...list.value, ...records]
    total.value = data?.total || 0
    finished.value = list.value.length >= total.value
  } catch (e) {
    if (reset) list.value = []
  } finally {
    loading.value = false
    uni.stopPullDownRefresh()
  }
}

onShow(() => {
  if (!getToken()) { uni.reLaunch({ url: '/pages/login/login' }); return }
  load(true)
})
onPullDownRefresh(() => load(true))
onReachBottom(() => {
  if (!finished.value && !loading.value) { page.value += 1; load() }
})

function back() {
  uni.navigateBack({ fail: () => uni.reLaunch({ url: '/pages/mine/mine' }) })
}

async function openItem(row) {
  if (!row.isRead) {
    try {
      await readNotification(row.id)
      row.isRead = 1
    } catch (e) {}
  }
  if (row.bizType === 'APPROVAL_RESULT' && row.bizId) {
    uni.navigateTo({ url: `/pages/leave/detail?id=${row.bizId}` })
  }
}
</script>

<template>
  <view class="page-wrap">
    <view class="subnav" @click="back">
      <AppIcon name="chevron-left" :size="16" color="#0071e3" />
      <text>返回</text>
    </view>

    <view class="page-head">
      <view>
        <view class="large-title">消息通知</view>
        <view class="page-subtitle">请假审批与系统消息</view>
      </view>
    </view>

    <view class="card">
      <view v-if="loading && !list.length" class="loading-block">
        <view class="spinner" />
        <text>加载中…</text>
      </view>
      <EmptyBox v-else-if="!list.length" icon="bell" text="暂无消息通知" />

      <template v-else>
        <view v-for="row in list" :key="row.id" class="group-row notify-row" :class="{ unread: !row.isRead }" @click="openItem(row)">
          <view class="notify-icon">
            <AppIcon name="bell" :size="16" :color="row.isRead ? '#a1a1a6' : '#0071e3'" />
            <view v-if="!row.isRead" class="unread-dot" />
          </view>
          <view class="notify-main">
            <view class="notify-title" :class="{ bold: !row.isRead }">{{ row.title }}</view>
            <view class="notify-content">{{ row.content }}</view>
            <view class="notify-time">{{ fmtTime(row.createTime, true) }}</view>
          </view>
          <AppIcon v-if="row.bizType === 'APPROVAL_RESULT'" name="chevron-right" :size="16" color="#c7c7cc" />
        </view>

        <view v-if="list.length && finished" class="list-end">— 共 {{ total }} 条 —</view>
        <view v-else-if="loading" class="list-end">加载中…</view>
      </template>
    </view>
  </view>
</template>

<style scoped>
.page-head { margin-bottom: 18px; }
.notify-row { gap: 12px; align-items: flex-start; }
.notify-icon {
  position: relative;
  width: 34px; height: 34px; border-radius: 10px;
  background: rgba(0, 0, 0, .045);
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
  margin-top: 2px;
}
.notify-row.unread .notify-icon { background: var(--accent-soft); }
.unread-dot {
  position: absolute; top: -2px; right: -2px;
  width: 9px; height: 9px; border-radius: 50%;
  background: var(--red);
  border: 1.5px solid var(--surface-solid);
}
.notify-main { flex: 1; min-width: 0; }
.notify-title { font-size: 15px; font-weight: 500; color: var(--text-1); }
.notify-title.bold { font-weight: 700; }
.notify-content { font-size: 13px; color: var(--text-2); margin-top: 3px; line-height: 1.5; }
.notify-time { font-size: 11.5px; color: #a1a1a6; margin-top: 6px; }
.list-end { text-align: center; font-size: 12px; color: #a1a1a6; padding: 14px 0 16px; }
</style>
