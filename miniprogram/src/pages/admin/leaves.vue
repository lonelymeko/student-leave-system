<script setup>
// 管理员 — 全部请假：状态筛选 + 搜索 + 列表；点进详情复用 leave/detail；导出 Excel
import { ref, watch } from 'vue'
import { onShow, onPullDownRefresh, onReachBottom } from '@dcloudio/uni-app'
import { getAllLeaves } from '../../api'
import AppIcon from '../../components/AppIcon.vue'
import StatusPill from '../../components/StatusPill.vue'
import SegmentedBar from '../../components/SegmentedBar.vue'
import EmptyBox from '../../components/EmptyBox.vue'
import TabBar from '../../components/TabBar.vue'
import { typeText, fmtTime } from '../../utils/constants'
import { exportLeavesFile } from '../../utils/export'
import { getToken, getUser } from '../../utils/auth'

const FILTERS = [
  { value: '', label: '全部' },
  { value: 'PENDING', label: '待审批' },
  { value: 'APPROVED', label: '请假中' },
  { value: 'CANCEL_PENDING', label: '销假中' },
  { value: 'COMPLETED', label: '已完成' },
  { value: 'REJECTED', label: '已驳回' },
  { value: 'REVOKED', label: '已撤回' }
]

const status = ref('')
const keyword = ref('')
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
    const data = await getAllLeaves({
      status: status.value || undefined,
      keyword: keyword.value.trim() || undefined,
      page: page.value, size
    })
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

watch(status, () => load(true))

onShow(() => {
  if (!getToken()) { uni.reLaunch({ url: '/pages/login/login' }); return }
  if (getUser()?.role !== 'ADMIN') { uni.reLaunch({ url: '/pages/login/login' }); return }
  load(true)
})
onPullDownRefresh(() => load(true))
onReachBottom(() => {
  if (!finished.value && !loading.value) { page.value += 1; load() }
})

function search() { load(true) }
function goDetail(row) {
  uni.navigateTo({ url: `/pages/leave/detail?id=${row.id}` })
}

// 导出
const exporting = ref(false)
async function doExport() {
  if (exporting.value) return
  exporting.value = true
  try {
    await exportLeavesFile(status.value)
  } catch (e) {} finally { exporting.value = false }
}
</script>

<template>
  <view class="page-wrap">
    <view class="page-head">
      <view>
        <view class="large-title">全部请假</view>
        <view class="page-subtitle">全校请假记录查询</view>
      </view>
      <view class="btn btn-secondary btn-sm" :class="{ disabled: exporting }" @click="doExport">
        <AppIcon name="download" :size="14" color="#1d1d1f" />
        <text>{{ exporting ? '导出中…' : '导出Excel' }}</text>
      </view>
    </view>

    <view style="margin-bottom: 12px">
      <SegmentedBar v-model="status" :options="FILTERS" />
    </view>

    <view class="search-wrap" style="margin-bottom: 16px">
      <view class="search-icon"><AppIcon name="search" :size="15" color="#8e8e93" /></view>
      <input
        v-model="keyword"
        class="input search-input"
        placeholder="搜索学生 / 学号"
        placeholder-class="ph"
        confirm-type="search"
        @confirm="search"
      />
    </view>

    <view class="card">
      <view v-if="loading && !list.length" class="loading-block">
        <view class="spinner" /><text>加载中…</text>
      </view>
      <EmptyBox v-else-if="!list.length" text="没有符合条件的请假记录" />

      <template v-else>
        <view v-for="row in list" :key="row.id" class="group-row leave-row" @click="goDetail(row)">
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
              <view class="meta-item" v-if="row.approverName">
                <AppIcon name="user" :size="13" color="#6e6e73" />
                <text>{{ row.approverName }}</text>
              </view>
            </view>
          </view>
          <AppIcon name="chevron-right" :size="16" color="#c7c7cc" />
        </view>

        <view v-if="list.length && finished" class="list-end">— 共 {{ total }} 条 —</view>
        <view v-else-if="loading" class="list-end">加载中…</view>
      </template>
    </view>

    <TabBar current="admin-leaves" />
  </view>
</template>

<style scoped>
.page-head { display: flex; align-items: flex-end; justify-content: space-between; margin-bottom: 16px; }
.search-wrap { position: relative; }
.search-icon { position: absolute; left: 13px; top: 50%; transform: translateY(-50%); z-index: 2; display: flex; align-items: center; }
.search-input { padding-left: 38px; border-radius: var(--radius-pill); }

.leave-row { gap: 10px; align-items: center; }
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
.list-end { text-align: center; font-size: 12px; color: #a1a1a6; padding: 14px 0 16px; }
</style>
