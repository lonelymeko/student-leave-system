<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { submitLeave, aiDraft } from '../../api'
import Icon from '../../components/Icon.vue'
import { LEAVE_TYPES, toApiTime, toLocalInput } from '../../utils/constants'
import { toast } from '../../utils/toast'

const router = useRouter()

const form = ref({
  type: 'PERSONAL',
  startTime: '',
  endTime: '',
  reason: '',
  destination: '',
  contactPhone: ''
})
const submitting = ref(false)

// ---- AI 智能填单 ----
const aiText = ref('')
const aiLoading = ref(false)
const aiUnavailable = ref(false)

async function runAiDraft() {
  const text = aiText.value.trim()
  if (!text) { toast.warning('先用一句话描述你的请假需求'); return }
  aiLoading.value = true
  try {
    const d = await aiDraft(text)
    if (d) {
      if (d.type) form.value.type = d.type
      if (d.startTime) form.value.startTime = toLocalInput(d.startTime)
      if (d.endTime) form.value.endTime = toLocalInput(d.endTime)
      if (d.reason) form.value.reason = d.reason
      if (d.destination) form.value.destination = d.destination
      toast.success('AI 已生成草稿，请核对后提交')
    }
  } catch (e) {
    if (e?.code === 5001) {
      aiUnavailable.value = true
      toast.warning('AI 服务暂不可用，请手动填写表单')
    }
  } finally {
    aiLoading.value = false
  }
}

const days = computed(() => {
  if (!form.value.startTime || !form.value.endTime) return null
  const s = new Date(form.value.startTime)
  const e = new Date(form.value.endTime)
  if (e <= s) return null
  return Math.max(0.5, Math.round(((e - s) / 86400000) * 2) / 2)
})

function validate() {
  const f = form.value
  if (!f.startTime || !f.endTime) return '请选择起止时间'
  if (new Date(f.endTime) <= new Date(f.startTime)) return '结束时间必须晚于开始时间'
  const today0 = new Date(); today0.setHours(0, 0, 0, 0)
  if (new Date(f.startTime) < today0) return '开始时间不得早于今天'
  const r = f.reason.trim()
  if (r.length < 5 || r.length > 200) return '请假事由需 5~200 字'
  return ''
}

async function submit() {
  const err = validate()
  if (err) { toast.warning(err); return }
  submitting.value = true
  try {
    await submitLeave({
      type: form.value.type,
      startTime: toApiTime(form.value.startTime),
      endTime: toApiTime(form.value.endTime),
      reason: form.value.reason.trim(),
      destination: form.value.destination.trim() || undefined,
      contactPhone: form.value.contactPhone.trim() || undefined
    })
    toast.success('请假申请已提交，等待辅导员审批')
    router.push('/student/leaves')
  } catch (e) {} finally { submitting.value = false }
}
</script>

<template>
  <div class="new-leave">
    <h1 class="large-title">新建请假</h1>
    <p class="page-subtitle" style="margin-bottom:22px">填写请假信息，或让 AI 帮你一句话生成</p>

    <!-- AI 智能填单 -->
    <div class="card card-pad ai-box" v-if="!aiUnavailable">
      <div class="ai-box-head">
        <span class="ai-badge"><Icon name="sparkle" :size="15" /></span>
        <div>
          <div class="ai-box-title">AI 智能填单</div>
          <div class="ai-box-desc">用一句话描述，AI 自动生成请假单草稿</div>
        </div>
      </div>
      <form class="ai-box-input" @submit.prevent="runAiDraft">
        <input
          v-model="aiText"
          class="input"
          placeholder="例如：我下周一到周三回家参加表哥的婚礼，去乌鲁木齐"
          :disabled="aiLoading"
        />
        <button class="btn btn-primary" type="submit" :disabled="aiLoading || !aiText.trim()">
          <span v-if="aiLoading" class="spinner ai-spin"></span>
          <Icon v-else name="sparkle" :size="15" />
          {{ aiLoading ? '生成中…' : '生成' }}
        </button>
      </form>
    </div>
    <div class="card card-pad ai-box unavailable" v-else>
      <Icon name="warning" :size="16" style="color:var(--orange)" />
      <span style="font-size:13.5px;color:var(--text-2)">AI 智能填单暂不可用（服务未配置），请手动填写下方表单。</span>
    </div>

    <!-- 表单 -->
    <form class="card card-pad form-card" @submit.prevent="submit">
      <div class="field">
        <label class="field-label">请假类型</label>
        <div class="segmented">
          <button
            v-for="t in LEAVE_TYPES" :key="t.value" type="button"
            :class="{ active: form.type === t.value }"
            @click="form.type = t.value"
          >{{ t.label }}</button>
        </div>
      </div>

      <div class="grid-2">
        <div class="field">
          <label class="field-label">开始时间</label>
          <input v-model="form.startTime" type="datetime-local" class="input" />
        </div>
        <div class="field">
          <label class="field-label">结束时间</label>
          <input v-model="form.endTime" type="datetime-local" class="input" />
        </div>
      </div>
      <div v-if="days" class="days-hint">
        <Icon name="clock" :size="13" />约 {{ days }} 天
      </div>

      <div class="field">
        <label class="field-label">请假事由（5~200 字）</label>
        <textarea v-model="form.reason" class="textarea" placeholder="请简要说明请假原因" maxlength="200"></textarea>
        <span class="count">{{ form.reason.length }}/200</span>
      </div>

      <div class="grid-2">
        <div class="field">
          <label class="field-label">目的地（选填）</label>
          <input v-model="form.destination" class="input" placeholder="如：乌鲁木齐" />
        </div>
        <div class="field">
          <label class="field-label">联系电话（选填）</label>
          <input v-model="form.contactPhone" class="input" placeholder="如：13800000000" />
        </div>
      </div>

      <div class="form-actions">
        <button class="btn btn-secondary" type="button" @click="router.back()">取消</button>
        <button class="btn btn-primary btn-lg" type="submit" :disabled="submitting">
          {{ submitting ? '提交中…' : '提交申请' }}
        </button>
      </div>
    </form>
  </div>
</template>

<style scoped>
.new-leave { max-width: 640px; }

.ai-box { margin-bottom: 18px; padding: 20px 24px; }
.ai-box.unavailable { display: flex; align-items: center; gap: 10px; }
.ai-box-head { display: flex; align-items: center; gap: 12px; margin-bottom: 14px; }
.ai-badge {
  width: 34px; height: 34px; border-radius: 10px;
  background: linear-gradient(135deg, #0071e3, #af52de);
  color: #fff;
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 4px 12px rgba(90, 100, 230, .35);
  flex-shrink: 0;
}
.ai-box-title { font-size: 15px; font-weight: 600; }
.ai-box-desc { font-size: 12.5px; color: var(--text-2); }
.ai-box-input { display: flex; gap: 10px; }
.ai-spin { width: 14px; height: 14px; border-width: 2px; border-color: rgba(255,255,255,.4); border-top-color: #fff; }

.form-card { display: flex; flex-direction: column; gap: 18px; }
.grid-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
.days-hint {
  display: inline-flex; align-items: center; gap: 5px;
  font-size: 12.5px; color: var(--accent); font-weight: 500;
  margin-top: -8px;
}
.field { position: relative; }
.count { position: absolute; right: 4px; bottom: -18px; font-size: 11.5px; color: var(--text-2); }
.form-actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 8px; }
</style>
