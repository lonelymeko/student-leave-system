<script setup>
// 副书记（LEADER）— 二级审批：LEADER_PENDING 单据（辅导员通过后天数超限转上来的）
// 通过 → APPROVED（单变请假中），驳回 → REJECTED（驳回必填意见）。
import { ref } from 'vue'
import { onShow, onPullDownRefresh, onReachBottom } from '@dcloudio/uni-app'
import { getLeaderPending, approve } from '../../api'
import AppIcon from '../../components/AppIcon.vue'
import StatusPill from '../../components/StatusPill.vue'
import EmptyBox from '../../components/EmptyBox.vue'
import TabBar from '../../components/TabBar.vue'
import SheetModal from '../../components/SheetModal.vue'
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
    const data = await getLeaderPending({ page: page.value, size })
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

// ---- 审批弹层 ----
const target = ref(null)
const action = ref('APPROVE')
const comment = ref('')
const submitting = ref(false)

function openApprove(row) {
  target.value = row
  action.value = 'APPROVE'
  comment.value = ''
}

async function doApprove() {
  if (action.value === 'REJECT' && !comment.value.trim()) {
    uni.showToast({ title: '驳回时必须填写审批意见', icon: 'none' })
    return
  }
  if (submitting.value) return
  submitting.value = true
  try {
    await approve(target.value.id, { action: action.value, comment: comment.value.trim() })
    uni.showToast({ title: action.value === 'APPROVE' ? '已通过，学生进入请假中' : '已驳回', icon: 'none' })
    target.value = null
    load(true)
  } catch (e) {} finally { submitting.value = false }
}
</script>

<template>
  <view class="page-wrap">
    <view class="page-head">
      <view>
        <view class="large-title">二级审批</view>
        <view class="page-subtitle">辅导员通过后天数超限的请假申请</view>
      </view>
      <view class="btn btn-secondary btn-sm" @click="load(true)">
        <AppIcon name="refresh" :size="14" color="#1d1d1f" />
        <text>刷新</text>
      </view>
    </view>

    <view class="card">
      <view v-if="loading && !list.length" class="loading-block">
        <view class="spinner" />
        <text>加载中…</text>
      </view>
      <EmptyBox v-else-if="!list.length" icon="check" text="太棒了，暂无待审批事项" />

      <template v-else>
        <view v-for="row in list" :key="row.id" class="group-row pending-row" @click="goDetail(row)">
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
            </view>
            <view class="row-ops">
              <view class="btn btn-sm btn-primary" @click.stop="openApprove(row)">
                <AppIcon name="edit" :size="14" color="#ffffff" />
                <text>审批</text>
              </view>
            </view>
          </view>
          <AppIcon name="chevron-right" :size="16" color="#c7c7cc" />
        </view>

        <view v-if="list.length && finished" class="list-end">— 共 {{ total }} 条 —</view>
        <view v-else-if="loading" class="list-end">加载中…</view>
      </template>
    </view>

    <!-- 审批弹层 -->
    <SheetModal :show="!!target" title="副书记二级审批" @close="target = null">
      <template v-if="target">
        <view class="ap-summary">
          <view class="ap-line"><text class="k">学生</text><text>{{ target.studentName }}（{{ target.className }}）</text></view>
          <view class="ap-line"><text class="k">类型</text><text>{{ target.typeText || typeText(target.type) }}</text></view>
          <view class="ap-line"><text class="k">时间</text><text>{{ fmtTime(target.startTime, true) }} → {{ fmtTime(target.endTime, true) }}（{{ target.days }} 天）</text></view>
          <view class="ap-line"><text class="k">事由</text><text>{{ target.reason }}</text></view>
          <view v-if="target.destination" class="ap-line"><text class="k">目的地</text><text>{{ target.destination }}</text></view>
        </view>

        <view class="field" style="margin-bottom: 14px">
          <text class="field-label">审批结果</text>
          <view class="segmented" style="align-self: flex-start">
            <view class="seg-item" :class="{ active: action === 'APPROVE' }" @click="action = 'APPROVE'">通过</view>
            <view class="seg-item" :class="{ active: action === 'REJECT' }" @click="action = 'REJECT'">驳回</view>
          </view>
        </view>
        <view class="field">
          <text class="field-label">审批意见{{ action === 'REJECT' ? '（驳回必填）' : '（选填）' }}</text>
          <textarea
            v-model="comment"
            class="textarea"
            :placeholder="action === 'APPROVE' ? '如：同意，注意安全' : '请说明驳回原因'"
            placeholder-class="ph"
          />
        </view>
      </template>
      <template #footer>
        <view class="btn btn-secondary" @click="target = null">取消</view>
        <view class="btn" :class="[action === 'APPROVE' ? 'btn-primary' : 'btn-danger', { disabled: submitting }]" @click="doApprove">
          {{ submitting ? '提交中…' : (action === 'APPROVE' ? '确认通过' : '确认驳回') }}
        </view>
      </template>
    </SheetModal>

    <TabBar current="leader-pending" />
  </view>
</template>

<style scoped>
.page-head { display: flex; align-items: flex-end; justify-content: space-between; margin-bottom: 18px; }
.pending-row { gap: 10px; align-items: center; }
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
.row-ops { display: flex; gap: 8px; margin-top: 9px; }
.list-end { text-align: center; font-size: 12px; color: #a1a1a6; padding: 14px 0 16px; }

.ap-summary {
  background: rgba(0, 0, 0, .035);
  border-radius: 12px;
  padding: 13px 15px;
  display: flex; flex-direction: column; gap: 7px;
  font-size: 13.5px;
  margin-bottom: 14px;
}
.ap-line { display: flex; gap: 12px; }
.ap-line .k { color: var(--text-2); flex-shrink: 0; width: 44px; }
</style>
