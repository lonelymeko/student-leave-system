<script setup>
// 学生 — 我的请假列表：状态筛选 + 下拉刷新 + 触底加载更多 + 撤回 / 申请销假
import { ref, watch } from 'vue'
import { onShow, onPullDownRefresh, onReachBottom } from '@dcloudio/uni-app'
import { getMyLeaves, revokeLeave, cancelApply } from '../../api'
import AppIcon from '../../components/AppIcon.vue'
import StatusPill from '../../components/StatusPill.vue'
import SegmentedBar from '../../components/SegmentedBar.vue'
import EmptyBox from '../../components/EmptyBox.vue'
import TabBar from '../../components/TabBar.vue'
import SheetModal from '../../components/SheetModal.vue'
import { typeText, fmtTime } from '../../utils/constants'
import { getToken } from '../../utils/auth'

const FILTERS = [
  { value: '', label: '全部' },
  { value: 'PENDING', label: '待审批' },
  { value: 'APPROVED', label: '请假中' },
  { value: 'CANCEL_PENDING', label: '销假中' },
  { value: 'COMPLETED', label: '已完成' },
  { value: 'REJECTED', label: '已驳回' }
]

const status = ref('')
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
    const data = await getMyLeaves({ status: status.value || undefined, page: page.value, size })
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
  load(true)
})
onPullDownRefresh(() => load(true))
onReachBottom(() => {
  if (!finished.value && !loading.value) { page.value += 1; load() }
})

function goDetail(row) {
  uni.navigateTo({ url: `/pages/leave/detail?id=${row.id}` })
}
function goNew() {
  uni.redirectTo({ url: '/pages/student/new' })
}

// ---- 撤回 ----
const revokeTarget = ref(null)
const revoking = ref(false)
async function doRevoke() {
  if (revoking.value) return
  revoking.value = true
  try {
    await revokeLeave(revokeTarget.value.id)
    uni.showToast({ title: '已撤回', icon: 'none' })
    revokeTarget.value = null
    load(true)
  } catch (e) {} finally { revoking.value = false }
}

// ---- 销假申请 ----
const cancelTarget = ref(null)
const cancelNote = ref('')
const canceling = ref(false)
function openCancel(row) {
  cancelTarget.value = row
  cancelNote.value = ''
}
async function doCancelApply() {
  if (canceling.value) return
  canceling.value = true
  try {
    await cancelApply(cancelTarget.value.id, { note: cancelNote.value.trim() })
    uni.showToast({ title: '销假申请已提交，等待辅导员确认', icon: 'none' })
    cancelTarget.value = null
    load(true)
  } catch (e) {} finally { canceling.value = false }
}
</script>

<template>
  <view class="page-wrap">
    <view class="page-head">
      <view>
        <view class="large-title">我的请假</view>
        <view class="page-subtitle">查看和管理你的请假申请</view>
      </view>
    </view>

    <view style="margin-bottom: 16px">
      <SegmentedBar v-model="status" :options="FILTERS" />
    </view>

    <view class="card">
      <view v-if="loading && !list.length" class="loading-block">
        <view class="spinner" />
        <text>加载中…</text>
      </view>
      <EmptyBox v-else-if="!list.length" text="还没有请假记录">
        <view class="btn btn-text btn-sm" @click="goNew">去新建一条</view>
      </EmptyBox>

      <template v-else>
        <view v-for="row in list" :key="row.id" class="group-row leave-row" @click="goDetail(row)">
          <view class="row-main">
            <view class="row-line1">
              <text class="row-type">{{ row.typeText || typeText(row.type) }}</text>
              <StatusPill :status="row.status" />
            </view>
            <view class="row-reason">{{ row.reason }}</view>
            <view class="row-meta">
              <view class="meta-item">
                <AppIcon name="calendar" :size="13" color="#6e6e73" />
                <text>{{ fmtTime(row.startTime) }} → {{ fmtTime(row.endTime) }}</text>
              </view>
              <view class="meta-item">
                <AppIcon name="clock" :size="13" color="#6e6e73" />
                <text>{{ row.days }} 天</text>
              </view>
            </view>
            <view class="row-ops" v-if="row.status === 'PENDING' || row.status === 'APPROVED'">
              <view v-if="row.status === 'PENDING'" class="btn btn-sm btn-danger-text op-btn" @click.stop="revokeTarget = row">
                <AppIcon name="undo" :size="14" color="#ff3b30" />
                <text>撤回</text>
              </view>
              <view v-if="row.status === 'APPROVED'" class="btn btn-sm btn-primary" @click.stop="openCancel(row)">
                <AppIcon name="check" :size="14" color="#ffffff" />
                <text>申请销假</text>
              </view>
            </view>
          </view>
          <AppIcon name="chevron-right" :size="16" color="#c7c7cc" />
        </view>

        <view v-if="list.length && finished" class="list-end">— 共 {{ total }} 条 —</view>
        <view v-else-if="loading" class="list-end">加载中…</view>
      </template>
    </view>

    <!-- 撤回确认 -->
    <SheetModal :show="!!revokeTarget" title="撤回申请" @close="revokeTarget = null">
      <view class="confirm-text">确定撤回这条请假申请吗？撤回后不可恢复，如需请假请重新提交。</view>
      <template #footer>
        <view class="btn btn-secondary" @click="revokeTarget = null">取消</view>
        <view class="btn btn-danger" :class="{ disabled: revoking }" @click="doRevoke">确认撤回</view>
      </template>
    </SheetModal>

    <!-- 销假申请 -->
    <SheetModal :show="!!cancelTarget" title="申请销假" @close="cancelTarget = null">
      <view class="confirm-text" style="margin-bottom: 14px">已返校？提交销假申请后，辅导员确认即完成本次请假。</view>
      <view class="field">
        <text class="field-label">销假说明</text>
        <textarea v-model="cancelNote" class="textarea" placeholder="如：已于15日晚返校" placeholder-class="ph" />
      </view>
      <template #footer>
        <view class="btn btn-secondary" @click="cancelTarget = null">取消</view>
        <view class="btn btn-primary" :class="{ disabled: canceling }" @click="doCancelApply">提交销假</view>
      </template>
    </SheetModal>

    <TabBar current="leaves" />
  </view>
</template>

<style scoped>
.page-head { display: flex; align-items: flex-end; justify-content: space-between; margin-bottom: 18px; }
.leave-row { gap: 10px; align-items: center; }
.row-main { flex: 1; min-width: 0; }
.row-line1 { display: flex; align-items: center; gap: 10px; margin-bottom: 3px; }
.row-type { font-size: 15px; font-weight: 600; }
.row-reason {
  font-size: 13.5px; color: var(--text-1);
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
  margin-bottom: 5px;
}
.row-meta { display: flex; gap: 14px; flex-wrap: wrap; }
.meta-item { display: inline-flex; align-items: center; gap: 4px; font-size: 12px; color: var(--text-2); }
.row-ops { display: flex; gap: 8px; margin-top: 9px; }
.op-btn { background: var(--red-soft); }
.list-end { text-align: center; font-size: 12px; color: #a1a1a6; padding: 14px 0 16px; }
.confirm-text { color: var(--text-2); font-size: 14px; line-height: 1.7; }
</style>
