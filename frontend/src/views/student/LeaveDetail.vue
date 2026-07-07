<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getLeaveDetail, revokeLeave, cancelApply } from '../../api'
import { useAuthStore } from '../../stores/auth'
import Icon from '../../components/Icon.vue'
import StatusPill from '../../components/StatusPill.vue'
import LoadingBlock from '../../components/LoadingBlock.vue'
import EmptyState from '../../components/EmptyState.vue'
import Modal from '../../components/Modal.vue'
import { typeText, fmtTime, ACTION_MAP } from '../../utils/constants'
import { toast } from '../../utils/toast'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const detail = ref(null)
const loading = ref(true)
const failed = ref(false)

async function load() {
  loading.value = true
  failed.value = false
  try {
    detail.value = await getLeaveDetail(route.params.id)
  } catch (e) {
    failed.value = true
  } finally {
    loading.value = false
  }
}
onMounted(load)

const actionInfo = a => ACTION_MAP[a] || { text: a, color: 'var(--text-2)' }

// 学生自己的操作
const showRevoke = ref(false)
const revoking = ref(false)
async function doRevoke() {
  revoking.value = true
  try {
    await revokeLeave(detail.value.id)
    toast.success('已撤回')
    showRevoke.value = false
    load()
  } catch (e) {} finally { revoking.value = false }
}

const showCancel = ref(false)
const cancelNote = ref('')
const canceling = ref(false)
async function doCancelApply() {
  canceling.value = true
  try {
    await cancelApply(detail.value.id, { note: cancelNote.value.trim() })
    toast.success('销假申请已提交')
    showCancel.value = false
    load()
  } catch (e) {} finally { canceling.value = false }
}
</script>

<template>
  <div class="detail-page">
    <button class="btn btn-text btn-sm back" type="button" @click="router.back()">
      <Icon name="chevron-left" :size="15" />返回
    </button>

    <LoadingBlock v-if="loading" />
    <div v-else-if="failed" class="card"><EmptyState icon="warning" text="加载失败或无权查看该请假单" /></div>

    <template v-else-if="detail">
      <div class="page-head">
        <div>
          <h1 class="large-title">{{ detail.typeText || typeText(detail.type) }}申请</h1>
          <p class="page-subtitle">{{ detail.studentName }} · {{ detail.className || '-' }} · 提交于 {{ fmtTime(detail.createTime, true) }}</p>
        </div>
        <StatusPill :status="detail.status" style="font-size:14px;padding:5px 16px" />
      </div>

      <div class="detail-grid">
        <!-- 基本信息 -->
        <div class="card">
          <div class="section-title">请假信息</div>
          <div class="group-list">
            <div class="group-row info-row"><span class="k">请假类型</span><span class="v">{{ detail.typeText || typeText(detail.type) }}</span></div>
            <div class="group-row info-row"><span class="k">开始时间</span><span class="v">{{ fmtTime(detail.startTime, true) }}</span></div>
            <div class="group-row info-row"><span class="k">结束时间</span><span class="v">{{ fmtTime(detail.endTime, true) }}</span></div>
            <div class="group-row info-row"><span class="k">请假天数</span><span class="v">{{ detail.days }} 天</span></div>
            <div class="group-row info-row"><span class="k">请假事由</span><span class="v">{{ detail.reason }}</span></div>
            <div class="group-row info-row" v-if="detail.destination"><span class="k">目的地</span><span class="v">{{ detail.destination }}</span></div>
            <div class="group-row info-row" v-if="detail.contactPhone"><span class="k">联系电话</span><span class="v">{{ detail.contactPhone }}</span></div>
            <div class="group-row info-row" v-if="detail.approverName"><span class="k">审批人</span><span class="v">{{ detail.approverName }}</span></div>
            <div class="group-row info-row" v-if="detail.approveComment"><span class="k">审批意见</span><span class="v">{{ detail.approveComment }}</span></div>
            <div class="group-row info-row" v-if="detail.cancelNote"><span class="k">销假说明</span><span class="v">{{ detail.cancelNote }}</span></div>
          </div>

          <div v-if="auth.role === 'STUDENT' && ['PENDING', 'APPROVED'].includes(detail.status)" class="detail-actions">
            <button v-if="detail.status === 'PENDING'" class="btn btn-danger-text" type="button" @click="showRevoke = true">
              <Icon name="undo" :size="15" />撤回申请
            </button>
            <button v-if="detail.status === 'APPROVED'" class="btn btn-primary" type="button" @click="showCancel = true">
              <Icon name="check" :size="15" />申请销假
            </button>
          </div>
        </div>

        <!-- 审批时间线 -->
        <div class="card">
          <div class="section-title">审批时间线</div>
          <div class="timeline" v-if="detail.records?.length">
            <div v-for="(r, i) in detail.records" :key="i" class="tl-item">
              <div class="tl-rail">
                <span class="tl-dot" :style="{ background: actionInfo(r.action).color }"></span>
                <span v-if="i < detail.records.length - 1" class="tl-line"></span>
              </div>
              <div class="tl-body">
                <div class="tl-head">
                  <span class="tl-action" :style="{ color: actionInfo(r.action).color }">{{ r.actionText || actionInfo(r.action).text }}</span>
                  <span class="tl-time">{{ fmtTime(r.createTime, true) }}</span>
                </div>
                <div class="tl-operator">{{ r.operatorName }}</div>
                <div v-if="r.comment" class="tl-comment">{{ r.comment }}</div>
              </div>
            </div>
          </div>
          <EmptyState v-else icon="clock" text="暂无审批记录" />
        </div>
      </div>
    </template>

    <!-- 撤回确认 -->
    <Modal :show="showRevoke" title="撤回申请" width="380px" @close="showRevoke = false">
      <p style="color:var(--text-2);font-size:14px">确定撤回这条请假申请吗？撤回后不可恢复。</p>
      <template #footer>
        <button class="btn btn-secondary" type="button" @click="showRevoke = false">取消</button>
        <button class="btn btn-danger" type="button" :disabled="revoking" @click="doRevoke">确认撤回</button>
      </template>
    </Modal>

    <!-- 销假申请 -->
    <Modal :show="showCancel" title="申请销假" width="420px" @close="showCancel = false">
      <div class="field">
        <label class="field-label">销假说明</label>
        <textarea v-model="cancelNote" class="textarea" placeholder="如：已于15日晚返校" rows="3"></textarea>
      </div>
      <template #footer>
        <button class="btn btn-secondary" type="button" @click="showCancel = false">取消</button>
        <button class="btn btn-primary" type="button" :disabled="canceling" @click="doCancelApply">提交销假</button>
      </template>
    </Modal>
  </div>
</template>

<style scoped>
.detail-page { max-width: 980px; }
.back { margin-bottom: 12px; margin-left: -12px; }
.page-head {
  display: flex; align-items: flex-end; justify-content: space-between;
  margin-bottom: 22px;
}
.detail-grid {
  display: grid;
  grid-template-columns: 1fr 380px;
  gap: 20px;
  align-items: start;
}
@media (max-width: 900px) { .detail-grid { grid-template-columns: 1fr; } }

.section-title {
  font-size: 15px; font-weight: 700;
  padding: 18px 20px 8px;
}
.info-row { justify-content: space-between; gap: 20px; }
.info-row .k { font-size: 13.5px; color: var(--text-2); flex-shrink: 0; }
.info-row .v { font-size: 14px; font-weight: 500; text-align: right; }
.detail-actions {
  display: flex; justify-content: flex-end; gap: 10px;
  padding: 14px 20px 18px;
  border-top: .5px solid var(--separator);
}

.timeline { padding: 10px 20px 22px; }
.tl-item { display: flex; gap: 14px; }
.tl-rail { display: flex; flex-direction: column; align-items: center; }
.tl-dot {
  width: 11px; height: 11px; border-radius: 50%;
  margin-top: 5px;
  box-shadow: 0 0 0 3.5px rgba(0, 0, 0, .05);
  flex-shrink: 0;
}
.tl-line { width: 2px; flex: 1; background: var(--separator); margin: 5px 0 1px; min-height: 18px; }
.tl-body { padding-bottom: 18px; flex: 1; min-width: 0; }
.tl-head { display: flex; align-items: baseline; justify-content: space-between; gap: 10px; }
.tl-action { font-size: 14px; font-weight: 600; }
.tl-time { font-size: 12px; color: var(--text-2); white-space: nowrap; }
.tl-operator { font-size: 12.5px; color: var(--text-2); margin-top: 1px; }
.tl-comment {
  margin-top: 7px;
  font-size: 13px;
  background: rgba(0, 0, 0, .04);
  border-radius: 10px;
  padding: 8px 12px;
  color: var(--text-1);
}
</style>
