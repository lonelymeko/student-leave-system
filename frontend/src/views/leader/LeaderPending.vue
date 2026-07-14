<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getLeaderPending, approve, aiApprovalAdvice } from '../../api'
import Icon from '../../components/Icon.vue'
import StatusPill from '../../components/StatusPill.vue'
import EmptyState from '../../components/EmptyState.vue'
import LoadingBlock from '../../components/LoadingBlock.vue'
import Pagination from '../../components/Pagination.vue'
import Modal from '../../components/Modal.vue'
import { typeText, fmtTime, RISK_MAP } from '../../utils/constants'
import { toast } from '../../utils/toast'

const router = useRouter()

const page = ref(1)
const size = 10
const total = ref(0)
const list = ref([])
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const data = await getLeaderPending({ page: page.value, size })
    list.value = data?.records || []
    total.value = data?.total || 0
  } catch (e) { list.value = [] } finally { loading.value = false }
}
onMounted(load)
function changePage(p) { page.value = p; load() }

// ---- 审批弹窗 ----
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
      toast.warning('AI 服务暂不可用')
    }
  } finally {
    adviceLoading.value = false
  }
}

async function doApprove() {
  if (action.value === 'REJECT' && !comment.value.trim()) {
    toast.warning('驳回时必须填写审批意见')
    return
  }
  submitting.value = true
  try {
    await approve(target.value.id, { action: action.value, comment: comment.value.trim() })
    toast.success(action.value === 'APPROVE' ? '已通过，该请假单已进入请假中' : '已驳回')
    target.value = null
    load()
  } catch (e) {} finally { submitting.value = false }
}
</script>

<template>
  <div>
    <div class="page-head">
      <div>
        <h1 class="large-title">二级审批</h1>
        <p class="page-subtitle">超出天数上限、待副书记复核的请假申请</p>
      </div>
      <button class="btn btn-secondary btn-sm" type="button" @click="load">
        <Icon name="refresh" :size="14" />刷新
      </button>
    </div>

    <div class="card">
      <LoadingBlock v-if="loading" />
      <EmptyState v-else-if="!list.length" icon="check" text="太棒了，暂无待复核申请" />

      <template v-else>
        <div class="group-list">
          <div v-for="row in list" :key="row.id" class="group-row clickable pending-row" @click="router.push(`/leave/${row.id}`)">
            <div class="row-main">
              <div class="row-line1">
                <span class="row-student">{{ row.studentName }}</span>
                <span class="row-class">{{ row.className }}</span>
                <StatusPill :status="row.status" />
              </div>
              <div class="row-reason">
                <b>{{ row.typeText || typeText(row.type) }}</b> · {{ row.reason }}
              </div>
              <div class="row-meta">
                <span class="meta-item"><Icon name="calendar" :size="13" />{{ fmtTime(row.startTime) }} → {{ fmtTime(row.endTime) }}（{{ row.days }} 天）</span>
                <span class="meta-item"><Icon name="user" :size="13" />辅导员：{{ row.approverName || '-' }}</span>
              </div>
            </div>
            <div class="row-actions" @click.stop>
              <button class="btn btn-sm btn-primary" type="button" @click="openApprove(row)">
                <Icon name="edit" :size="14" />复核
              </button>
              <Icon name="chevron-right" :size="16" style="color:#c7c7cc" />
            </div>
          </div>
        </div>
        <Pagination :total="total" :page="page" :size="size" @change="changePage" />
      </template>
    </div>

    <!-- 审批弹窗 -->
    <Modal :show="!!target" title="二级审批（副书记复核）" width="520px" @close="target = null">
      <template v-if="target">
        <div class="ap-summary">
          <div class="ap-line"><span class="k">学生</span><span>{{ target.studentName }}（{{ target.className }}）</span></div>
          <div class="ap-line"><span class="k">类型</span><span>{{ target.typeText || typeText(target.type) }}</span></div>
          <div class="ap-line"><span class="k">时间</span><span>{{ fmtTime(target.startTime, true) }} → {{ fmtTime(target.endTime, true) }}（{{ target.days }} 天）</span></div>
          <div class="ap-line"><span class="k">事由</span><span>{{ target.reason }}</span></div>
          <div class="ap-line" v-if="target.destination"><span class="k">目的地</span><span>{{ target.destination }}</span></div>
          <div class="ap-line"><span class="k">辅导员</span><span>{{ target.approverName || '-' }} 已初审通过</span></div>
        </div>

        <!-- AI 建议 -->
        <div class="ai-advice">
          <button v-if="!advice && !adviceUnavailable" class="btn btn-sm ai-btn" type="button" :disabled="adviceLoading" @click="loadAdvice">
            <span v-if="adviceLoading" class="spinner ai-spin"></span>
            <Icon v-else name="sparkle" :size="14" />
            {{ adviceLoading ? 'AI 分析中…' : 'AI 建议' }}
          </button>
          <div v-else-if="adviceUnavailable" class="advice-card unavailable">
            <Icon name="warning" :size="14" style="color:var(--orange)" />AI 服务暂不可用，请人工判断。
          </div>
          <div v-else class="advice-card">
            <div class="advice-head">
              <span class="advice-title"><Icon name="sparkle" :size="14" />AI 审批建议</span>
              <span class="pill" :class="RISK_MAP[advice.riskLevel]?.pill || 'pill-gray'">
                {{ RISK_MAP[advice.riskLevel]?.text || advice.riskLevel }}
              </span>
            </div>
            <p class="advice-text">{{ advice.advice }}</p>
          </div>
        </div>

        <div class="field" style="margin-bottom:14px">
          <label class="field-label">审批结果</label>
          <div class="segmented">
            <button type="button" :class="{ active: action === 'APPROVE' }" @click="action = 'APPROVE'">通过</button>
            <button type="button" :class="{ active: action === 'REJECT' }" @click="action = 'REJECT'">驳回</button>
          </div>
        </div>
        <div class="field">
          <label class="field-label">审批意见{{ action === 'REJECT' ? '（驳回必填）' : '（选填）' }}</label>
          <textarea v-model="comment" class="textarea" rows="3" :placeholder="action === 'APPROVE' ? '如：同意，注意安全' : '请说明驳回原因'"></textarea>
        </div>
      </template>
      <template #footer>
        <button class="btn btn-secondary" type="button" @click="target = null">取消</button>
        <button class="btn" :class="action === 'APPROVE' ? 'btn-primary' : 'btn-danger'" type="button" :disabled="submitting" @click="doApprove">
          {{ submitting ? '提交中…' : (action === 'APPROVE' ? '确认通过' : '确认驳回') }}
        </button>
      </template>
    </Modal>
  </div>
</template>

<style scoped>
.page-head { display: flex; align-items: flex-end; justify-content: space-between; margin-bottom: 22px; }
.pending-row { gap: 14px; }
.row-main { flex: 1; min-width: 0; }
.row-line1 { display: flex; align-items: center; gap: 10px; margin-bottom: 3px; }
.row-student { font-size: 15px; font-weight: 600; }
.row-class { font-size: 12.5px; color: var(--text-2); }
.row-reason { font-size: 13.5px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; margin-bottom: 5px; }
.row-meta { display: flex; gap: 16px; flex-wrap: wrap; }
.meta-item { display: inline-flex; align-items: center; gap: 4px; font-size: 12.5px; color: var(--text-2); }
.row-actions { display: flex; align-items: center; gap: 8px; flex-shrink: 0; }

.ap-summary {
  background: rgba(0, 0, 0, .035);
  border-radius: var(--radius-md);
  padding: 14px 16px;
  display: flex; flex-direction: column; gap: 7px;
  font-size: 13.5px;
  margin-bottom: 14px;
}
.ap-line { display: flex; gap: 12px; }
.ap-line .k { color: var(--text-2); flex-shrink: 0; width: 44px; }

.ai-advice { margin-bottom: 16px; }
.ai-btn {
  background: linear-gradient(135deg, rgba(0, 113, 227, .1), rgba(175, 82, 222, .1));
  color: var(--accent);
  font-weight: 600;
}
.ai-btn:hover:not(:disabled) { background: linear-gradient(135deg, rgba(0, 113, 227, .16), rgba(175, 82, 222, .16)); }
.ai-spin { width: 13px; height: 13px; border-width: 2px; }
.advice-card {
  background: linear-gradient(135deg, rgba(0, 113, 227, .06), rgba(175, 82, 222, .06));
  border: .5px solid rgba(0, 113, 227, .15);
  border-radius: var(--radius-md);
  padding: 13px 15px;
}
.advice-card.unavailable {
  display: flex; align-items: center; gap: 8px;
  font-size: 13px; color: var(--text-2);
  background: var(--orange-soft); border-color: rgba(255, 149, 0, .2);
}
.advice-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 7px; }
.advice-title { display: inline-flex; align-items: center; gap: 6px; font-size: 13px; font-weight: 700; color: var(--accent); }
.advice-text { font-size: 13.5px; line-height: 1.65; color: var(--text-1); white-space: pre-wrap; }
</style>
