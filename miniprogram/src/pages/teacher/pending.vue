<script setup>
// 辅导员 — 待办：PENDING 审批（通过/驳回 + 意见 + AI 建议）、CANCEL_PENDING 销假确认
import { ref } from 'vue'
import { onShow, onPullDownRefresh, onReachBottom } from '@dcloudio/uni-app'
import { getPending, approve, cancelConfirm, aiApprovalAdvice } from '../../api'
import AppIcon from '../../components/AppIcon.vue'
import StatusPill from '../../components/StatusPill.vue'
import EmptyBox from '../../components/EmptyBox.vue'
import TabBar from '../../components/TabBar.vue'
import SheetModal from '../../components/SheetModal.vue'
import { typeText, fmtTime, RISK_MAP } from '../../utils/constants'
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
    const data = await getPending({ page: page.value, size })
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

// AI 建议
const advice = ref(null)
const adviceLoading = ref(false)
const adviceUnavailable = ref(false)

function openApprove(row) {
  target.value = row
  action.value = 'APPROVE'
  comment.value = ''
  advice.value = null
  adviceUnavailable.value = false
}

async function loadAdvice() {
  if (adviceLoading.value) return
  adviceLoading.value = true
  try {
    advice.value = await aiApprovalAdvice(target.value.id)
  } catch (e) {
    if (e?.code === 5001) {
      adviceUnavailable.value = true
      uni.showToast({ title: 'AI 服务暂不可用', icon: 'none' })
    }
  } finally {
    adviceLoading.value = false
  }
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
    uni.showToast({ title: action.value === 'APPROVE' ? '已通过' : '已驳回', icon: 'none' })
    target.value = null
    load(true)
  } catch (e) {} finally { submitting.value = false }
}

// ---- 销假确认 ----
const cancelTarget = ref(null)
const confirming = ref(false)
async function doCancelConfirm() {
  if (confirming.value) return
  confirming.value = true
  try {
    await cancelConfirm(cancelTarget.value.id)
    uni.showToast({ title: '销假已确认，该请假单已完成', icon: 'none' })
    cancelTarget.value = null
    load(true)
  } catch (e) {} finally { confirming.value = false }
}
</script>

<template>
  <view class="page-wrap">
    <view class="page-head">
      <view>
        <view class="large-title">待办</view>
        <view class="page-subtitle">名下学生的请假申请与销假确认</view>
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
      <EmptyBox v-else-if="!list.length" icon="check" text="太棒了，暂无待办事项" />

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
              <text v-if="row.status === 'CANCEL_PENDING' && row.cancelNote"> · 销假说明：{{ row.cancelNote }}</text>
            </view>
            <view class="row-meta">
              <view class="meta-item">
                <AppIcon name="calendar" :size="13" color="#6e6e73" />
                <text>{{ fmtTime(row.startTime) }} → {{ fmtTime(row.endTime) }}（{{ row.days }} 天）</text>
              </view>
            </view>
            <view class="row-ops">
              <view v-if="row.status === 'PENDING'" class="btn btn-sm btn-primary" @click.stop="openApprove(row)">
                <AppIcon name="edit" :size="14" color="#ffffff" />
                <text>审批</text>
              </view>
              <view v-else-if="row.status === 'CANCEL_PENDING'" class="btn btn-sm btn-green" @click.stop="cancelTarget = row">
                <AppIcon name="check" :size="14" color="#ffffff" />
                <text>确认销假</text>
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
    <SheetModal :show="!!target" title="审批请假申请" @close="target = null">
      <template v-if="target">
        <view class="ap-summary">
          <view class="ap-line"><text class="k">学生</text><text>{{ target.studentName }}（{{ target.className }}）</text></view>
          <view class="ap-line"><text class="k">类型</text><text>{{ target.typeText || typeText(target.type) }}</text></view>
          <view class="ap-line"><text class="k">时间</text><text>{{ fmtTime(target.startTime, true) }} → {{ fmtTime(target.endTime, true) }}（{{ target.days }} 天）</text></view>
          <view class="ap-line"><text class="k">事由</text><text>{{ target.reason }}</text></view>
          <view v-if="target.destination" class="ap-line"><text class="k">目的地</text><text>{{ target.destination }}</text></view>
        </view>

        <!-- AI 建议 -->
        <view class="ai-advice">
          <view v-if="!advice && !adviceUnavailable" class="btn btn-sm ai-btn" :class="{ disabled: adviceLoading }" @click="loadAdvice">
            <view v-if="adviceLoading" class="spinner white" style="border-color: rgba(0,113,227,.2); border-top-color: #0071e3" />
            <AppIcon v-else name="sparkle" :size="14" color="#0071e3" />
            <text>{{ adviceLoading ? 'AI 分析中…' : 'AI 建议' }}</text>
          </view>
          <view v-else-if="adviceUnavailable" class="advice-card unavailable">
            <AppIcon name="warning" :size="14" color="#ff9500" />
            <text>AI 服务暂不可用，请人工判断。</text>
          </view>
          <view v-else class="advice-card">
            <view class="advice-head">
              <view class="advice-title">
                <AppIcon name="sparkle" :size="14" color="#0071e3" />
                <text>AI 审批建议</text>
              </view>
              <view class="pill" :class="RISK_MAP[advice.riskLevel]?.pill || 'pill-gray'">
                {{ RISK_MAP[advice.riskLevel]?.text || advice.riskLevel }}
              </view>
            </view>
            <view class="advice-text">{{ advice.advice }}</view>
          </view>
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

    <!-- 销假确认 -->
    <SheetModal :show="!!cancelTarget" title="确认销假" @close="cancelTarget = null">
      <template v-if="cancelTarget">
        <view class="confirm-text">
          确认 <text style="color: #1d1d1f; font-weight: 600">{{ cancelTarget.studentName }}</text> 已返校销假？
          <text v-if="cancelTarget.cancelNote">{{ '\n' }}销假说明：{{ cancelTarget.cancelNote }}</text>
          {{ '\n' }}确认后该请假单状态将变为「已完成」。
        </view>
      </template>
      <template #footer>
        <view class="btn btn-secondary" @click="cancelTarget = null">取消</view>
        <view class="btn btn-green" :class="{ disabled: confirming }" @click="doCancelConfirm">确认销假</view>
      </template>
    </SheetModal>

    <TabBar current="pending" />
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
.confirm-text { color: var(--text-2); font-size: 14px; line-height: 1.8; white-space: pre-line; }

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

.ai-advice { margin-bottom: 14px; }
.ai-btn {
  background: linear-gradient(135deg, rgba(0, 113, 227, .1), rgba(175, 82, 222, .1));
  color: var(--accent);
  font-weight: 600;
  align-self: flex-start;
  display: inline-flex;
}
.advice-card {
  background: linear-gradient(135deg, rgba(0, 113, 227, .06), rgba(175, 82, 222, .06));
  border: .5px solid rgba(0, 113, 227, .15);
  border-radius: 12px;
  padding: 12px 14px;
}
.advice-card.unavailable {
  display: flex; flex-direction: row; align-items: center; gap: 8px;
  font-size: 13px; color: var(--text-2);
  background: var(--orange-soft);
  border-color: rgba(255, 149, 0, .2);
}
.advice-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 7px; }
.advice-title { display: inline-flex; align-items: center; gap: 6px; font-size: 13px; font-weight: 700; color: var(--accent); }
.advice-text { font-size: 13.5px; line-height: 1.65; color: var(--text-1); white-space: pre-wrap; }
</style>
