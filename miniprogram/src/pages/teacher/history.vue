<script setup>
// 辅导员 — 审批历史（我处理过的）
import { ref } from 'vue'
import { onShow, onPullDownRefresh, onReachBottom } from '@dcloudio/uni-app'
import { getApprovalHistory } from '../../api'
import AppIcon from '../../components/AppIcon.vue'
import StatusPill from '../../components/StatusPill.vue'
import EmptyBox from '../../components/EmptyBox.vue'
import TabBar from '../../components/TabBar.vue'
import { typeText, fmtTime } from '../../utils/constants'
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
    const data = await getApprovalHistory({ page: page.value, size })
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

function goDetail(row) {
  uni.navigateTo({ url: `/pages/leave/detail?id=${row.id}` })
}
</script>

<template>
  <view class="page-wrap">
    <view class="large-title">审批历史</view>
    <view class="page-subtitle" style="margin-bottom: 18px">我处理过的请假单</view>

    <view class="card">
      <view v-if="loading && !list.length" class="loading-block">
        <view class="spinner" />
        <text>加载中…</text>
      </view>
      <EmptyBox v-else-if="!list.length" icon="history" text="还没有处理过的记录" />

      <template v-else>
        <view v-for="row in list" :key="row.id" class="group-row his-row" @click="goDetail(row)">
          <view class="row-main">
            <view class="row-line1">
              <text class="row-student">{{ row.studentName }}</text>
              <text class="row-class">{{ row.className }}</text>
              <StatusPill :status="row.status" />
            </view>
            <view class="row-reason">
              <text style="font-weight: 600">{{ row.typeText || typeText(row.type) }}</text>
              <text> · {{ row.reason }}</text>
            </view>
            <view class="row-meta">
              <view class="meta-item">
                <AppIcon name="calendar" :size="13" color="#6e6e73" />
                <text>{{ fmtTime(row.startTime) }} → {{ fmtTime(row.endTime) }}（{{ row.days }} 天）</text>
              </view>
              <view v-if="row.approveTime" class="meta-item">
                <AppIcon name="clock" :size="13" color="#6e6e73" />
                <text>处理于 {{ fmtTime(row.approveTime) }}</text>
              </view>
            </view>
            <view v-if="row.approveComment" class="row-comment">意见：{{ row.approveComment }}</view>
          </view>
          <AppIcon name="chevron-right" :size="16" color="#c7c7cc" />
        </view>

        <view v-if="list.length && finished" class="list-end">— 共 {{ total }} 条 —</view>
        <view v-else-if="loading" class="list-end">加载中…</view>
      </template>
    </view>

    <TabBar current="history" />
  </view>
</template>

<style scoped>
.his-row { gap: 10px; align-items: center; }
.row-main { flex: 1; min-width: 0; }
.row-line1 { display: flex; align-items: center; gap: 8px; margin-bottom: 3px; flex-wrap: wrap; }
.row-student { font-size: 15px; font-weight: 600; }
.row-class { font-size: 12px; color: var(--text-2); }
.row-reason {
  font-size: 13.5px;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
  margin-bottom: 5px;
}
.row-meta { display: flex; gap: 14px; flex-wrap: wrap; }
.meta-item { display: inline-flex; align-items: center; gap: 4px; font-size: 12px; color: var(--text-2); }
.row-comment {
  margin-top: 6px;
  font-size: 12.5px;
  color: var(--text-2);
  background: rgba(0, 0, 0, .035);
  border-radius: 8px;
  padding: 6px 10px;
}
.list-end { text-align: center; font-size: 12px; color: #a1a1a6; padding: 14px 0 16px; }
</style>
