<script setup>
// 学生 — 新建请假：AI 智能填单（/ai/draft，5001 降级）+ 手动表单
// 日期时间：picker date + time 组合出 "yyyy-MM-dd HH:mm:ss"
import { ref, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { submitLeave, aiDraft } from '../../api'
import AppIcon from '../../components/AppIcon.vue'
import TabBar from '../../components/TabBar.vue'
import { LEAVE_TYPES, fmtDate } from '../../utils/constants'
import { getToken } from '../../utils/auth'

onShow(() => { if (!getToken()) uni.reLaunch({ url: '/pages/login/login' }) })

const form = ref({
  type: 'PERSONAL',
  startDate: '', startClock: '',
  endDate: '', endClock: '',
  reason: '',
  destination: '',
  contactPhone: ''
})
const submitting = ref(false)
const todayStr = fmtDate(new Date())

// ---- AI 智能填单 ----
const aiText = ref('')
const aiLoading = ref(false)
const aiUnavailable = ref(false)

// "2026-07-13 08:00:00" -> {date:'2026-07-13', clock:'08:00'}
function splitApiTime(s) {
  const str = String(s || '').replace('T', ' ')
  return { date: str.slice(0, 10), clock: str.slice(11, 16) }
}

async function runAiDraft() {
  const text = aiText.value.trim()
  if (!text) { uni.showToast({ title: '先用一句话描述你的请假需求', icon: 'none' }); return }
  if (aiLoading.value) return
  aiLoading.value = true
  try {
    const d = await aiDraft(text)
    if (d) {
      if (d.type) form.value.type = d.type
      if (d.startTime) {
        const t = splitApiTime(d.startTime)
        form.value.startDate = t.date; form.value.startClock = t.clock
      }
      if (d.endTime) {
        const t = splitApiTime(d.endTime)
        form.value.endDate = t.date; form.value.endClock = t.clock
      }
      if (d.reason) form.value.reason = d.reason
      if (d.destination) form.value.destination = d.destination
      uni.showToast({ title: 'AI 已生成草稿，请核对后提交', icon: 'none' })
    }
  } catch (e) {
    if (e?.code === 5001) {
      aiUnavailable.value = true
      uni.showToast({ title: 'AI 服务暂不可用，请手动填写表单', icon: 'none' })
    }
  } finally {
    aiLoading.value = false
  }
}

const startFull = computed(() =>
  form.value.startDate && form.value.startClock ? `${form.value.startDate} ${form.value.startClock}:00` : '')
const endFull = computed(() =>
  form.value.endDate && form.value.endClock ? `${form.value.endDate} ${form.value.endClock}:00` : '')

const days = computed(() => {
  if (!startFull.value || !endFull.value) return null
  const s = new Date(startFull.value.replace(' ', 'T'))
  const e = new Date(endFull.value.replace(' ', 'T'))
  if (e <= s) return null
  return Math.max(0.5, Math.round(((e - s) / 86400000) * 2) / 2)
})

function validate() {
  const f = form.value
  if (!startFull.value || !endFull.value) return '请选择起止时间'
  const s = new Date(startFull.value.replace(' ', 'T'))
  const e = new Date(endFull.value.replace(' ', 'T'))
  if (e <= s) return '结束时间必须晚于开始时间'
  const today0 = new Date(); today0.setHours(0, 0, 0, 0)
  if (s < today0) return '开始时间不得早于今天'
  const r = f.reason.trim()
  if (r.length < 5 || r.length > 200) return '请假事由需 5~200 字'
  return ''
}

async function submit() {
  const err = validate()
  if (err) { uni.showToast({ title: err, icon: 'none' }); return }
  if (submitting.value) return
  submitting.value = true
  try {
    await submitLeave({
      type: form.value.type,
      startTime: startFull.value,
      endTime: endFull.value,
      reason: form.value.reason.trim(),
      destination: form.value.destination.trim() || undefined,
      contactPhone: form.value.contactPhone.trim() || undefined
    })
    uni.showToast({ title: '请假申请已提交，等待辅导员审批', icon: 'none' })
    setTimeout(() => uni.redirectTo({ url: '/pages/student/leaves' }), 600)
  } catch (e) {} finally { submitting.value = false }
}
</script>

<template>
  <view class="page-wrap">
    <view class="large-title">新建请假</view>
    <view class="page-subtitle" style="margin-bottom: 18px">填写请假信息，或让 AI 帮你一句话生成</view>

    <!-- AI 智能填单 -->
    <view v-if="!aiUnavailable" class="card ai-box">
      <view class="ai-box-head">
        <view class="ai-badge"><AppIcon name="sparkle" :size="16" color="#ffffff" /></view>
        <view>
          <view class="ai-box-title">AI 智能填单</view>
          <view class="ai-box-desc">用一句话描述，AI 自动生成请假单草稿</view>
        </view>
      </view>
      <view class="ai-box-input">
        <input
          v-model="aiText"
          class="input ai-input"
          placeholder="例如：我下周一到周三回家参加表哥的婚礼"
          placeholder-class="ph"
          :disabled="aiLoading"
          @confirm="runAiDraft"
        />
        <view class="btn btn-primary ai-gen" :class="{ disabled: aiLoading || !aiText.trim() }" @click="runAiDraft">
          <view v-if="aiLoading" class="spinner white" />
          <AppIcon v-else name="sparkle" :size="15" color="#ffffff" />
          <text>{{ aiLoading ? '生成中' : '生成' }}</text>
        </view>
      </view>
    </view>
    <view v-else class="card ai-box unavailable">
      <AppIcon name="warning" :size="16" color="#ff9500" />
      <text class="unavail-text">AI 智能填单暂不可用（服务未配置），请手动填写下方表单。</text>
    </view>

    <!-- 表单 -->
    <view class="card card-pad form-card">
      <view class="field">
        <text class="field-label">请假类型</text>
        <view class="segmented type-seg">
          <view
            v-for="t in LEAVE_TYPES" :key="t.value"
            class="seg-item"
            :class="{ active: form.type === t.value }"
            @click="form.type = t.value"
          >{{ t.label }}</view>
        </view>
      </view>

      <view class="field">
        <text class="field-label">开始时间</text>
        <view class="dt-row">
          <picker mode="date" :value="form.startDate" :start="todayStr" @change="e => form.startDate = e.detail.value" class="dt-picker">
            <view class="picker-box">
              <text class="val" :class="{ empty: !form.startDate }">{{ form.startDate || '选择日期' }}</text>
              <AppIcon name="calendar" :size="15" color="#6e6e73" />
            </view>
          </picker>
          <picker mode="time" :value="form.startClock || '08:00'" @change="e => form.startClock = e.detail.value" class="dt-picker time">
            <view class="picker-box">
              <text class="val" :class="{ empty: !form.startClock }">{{ form.startClock || '时间' }}</text>
              <AppIcon name="clock" :size="15" color="#6e6e73" />
            </view>
          </picker>
        </view>
      </view>

      <view class="field">
        <text class="field-label">结束时间</text>
        <view class="dt-row">
          <picker mode="date" :value="form.endDate" :start="form.startDate || todayStr" @change="e => form.endDate = e.detail.value" class="dt-picker">
            <view class="picker-box">
              <text class="val" :class="{ empty: !form.endDate }">{{ form.endDate || '选择日期' }}</text>
              <AppIcon name="calendar" :size="15" color="#6e6e73" />
            </view>
          </picker>
          <picker mode="time" :value="form.endClock || '18:00'" @change="e => form.endClock = e.detail.value" class="dt-picker time">
            <view class="picker-box">
              <text class="val" :class="{ empty: !form.endClock }">{{ form.endClock || '时间' }}</text>
              <AppIcon name="clock" :size="15" color="#6e6e73" />
            </view>
          </picker>
        </view>
      </view>
      <view v-if="days" class="days-hint">
        <AppIcon name="clock" :size="13" color="#0071e3" />
        <text>约 {{ days }} 天</text>
      </view>

      <view class="field">
        <text class="field-label">请假事由（5~200 字）</text>
        <textarea v-model="form.reason" class="textarea" placeholder="请简要说明请假原因" placeholder-class="ph" :maxlength="200" />
        <text class="count">{{ form.reason.length }}/200</text>
      </view>

      <view class="field">
        <text class="field-label">目的地（选填）</text>
        <input v-model="form.destination" class="input" placeholder="如：乌鲁木齐" placeholder-class="ph" />
      </view>
      <view class="field">
        <text class="field-label">联系电话（选填）</text>
        <input v-model="form.contactPhone" class="input" type="number" placeholder="如：13800000000" placeholder-class="ph" />
      </view>

      <view class="btn btn-primary btn-lg btn-block" :class="{ disabled: submitting }" style="margin-top: 6px" @click="submit">
        <view v-if="submitting" class="spinner white" />
        <text>{{ submitting ? '提交中…' : '提交申请' }}</text>
      </view>
    </view>

    <TabBar current="new" />
  </view>
</template>

<style scoped>
.ai-box { margin-bottom: 16px; padding: 18px; }
.ai-box.unavailable { display: flex; flex-direction: row; align-items: center; gap: 10px; }
.unavail-text { font-size: 13px; color: var(--text-2); }
.ai-box-head { display: flex; align-items: center; gap: 12px; margin-bottom: 13px; }
.ai-badge {
  width: 34px; height: 34px; border-radius: 10px;
  background: linear-gradient(135deg, #0071e3, #af52de);
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 4px 12px rgba(90, 100, 230, .35);
  flex-shrink: 0;
}
.ai-box-title { font-size: 15px; font-weight: 600; }
.ai-box-desc { font-size: 12px; color: var(--text-2); }
.ai-box-input { display: flex; gap: 8px; }
.ai-input { flex: 1; }
.ai-gen { flex-shrink: 0; padding: 9px 16px; }

.form-card { display: flex; flex-direction: column; gap: 16px; }
.type-seg { align-self: flex-start; }
.dt-row { display: flex; gap: 10px; }
.dt-picker { flex: 1.4; }
.dt-picker.time { flex: 1; }
.days-hint {
  display: inline-flex; align-items: center; gap: 5px;
  font-size: 12.5px; color: var(--accent); font-weight: 500;
  margin-top: -8px;
}
.field { position: relative; }
.count { position: absolute; right: 4px; bottom: -17px; font-size: 11px; color: var(--text-2); }
</style>
