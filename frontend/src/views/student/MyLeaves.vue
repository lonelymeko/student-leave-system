<script setup>
import { ref, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getMyLeaves, revokeLeave, cancelApply } from '../../api'
import Icon from '../../components/Icon.vue'
import Segmented from '../../components/Segmented.vue'
import StatusPill from '../../components/StatusPill.vue'
import EmptyState from '../../components/EmptyState.vue'
import LoadingBlock from '../../components/LoadingBlock.vue'
import Pagination from '../../components/Pagination.vue'
import Modal from '../../components/Modal.vue'
import { typeText, fmtTime } from '../../utils/constants'
import { toast } from '../../utils/toast'

const router = useRouter()

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

async function load() {
  loading.value = true
  try {
    const data = await getMyLeaves({ status: status.value || undefined, page: page.value, size })
    list.value = data?.records || []
    total.value = data?.total || 0
  } catch (e) {
    list.value = []
  } finally {
    loading.value = false
  }
}

watch(status, () => { page.value = 1; load() })
onMounted(load)

function changePage(p) { page.value = p; load() }

// ---- 撤回 ----
const revokeTarget = ref(null)
const revoking = ref(false)
async function doRevoke() {
  revoking.value = true
  try {
    await revokeLeave(revokeTarget.value.id)
    toast.success('已撤回')
    revokeTarget.value = null
    load()
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
  canceling.value = true
  try {
    await cancelApply(cancelTarget.value.id, { note: cancelNote.value.trim() })
    toast.success('销假申请已提交，等待辅导员确认')
    cancelTarget.value = null
    load()
  } catch (e) {} finally { canceling.value = false }
}
</script>

<template>
  <div>
    <div class="page-head">
      <div>
        <h1 class="large-title">我的请假</h1>
        <p class="page-subtitle">查看和管理你的请假申请</p>
      </div>
      <button class="btn btn-primary" type="button" @click="router.push('/student/new')">
        <Icon name="plus" :size="16" />新建请假
      </button>
    </div>

    <Segmented v-model="status" :options="FILTERS" style="margin-bottom:18px" />

    <div class="card">
      <LoadingBlock v-if="loading" />
      <EmptyState v-else-if="!list.length" text="还没有请假记录">
        <button class="btn btn-text btn-sm" type="button" @click="router.push('/student/new')">去新建一条</button>
      </EmptyState>

      <template v-else>
        <div class="group-list">
          <div
            v-for="row in list"
            :key="row.id"
            class="group-row clickable leave-row"
            @click="router.push(`/leave/${row.id}`)"
          >
            <div class="row-main">
              <div class="row-line1">
                <span class="row-type">{{ row.typeText || typeText(row.type) }}</span>
                <StatusPill :status="row.status" />
              </div>
              <div class="row-reason">{{ row.reason }}</div>
              <div class="row-meta">
                <span class="meta-item"><Icon name="calendar" :size="13" />{{ fmtTime(row.startTime) }} → {{ fmtTime(row.endTime) }}</span>
                <span class="meta-item"><Icon name="clock" :size="13" />{{ row.days }} 天</span>
                <span class="meta-item" v-if="row.destination"><Icon name="pin" :size="13" />{{ row.destination }}</span>
              </div>
            </div>
            <div class="row-actions" @click.stop>
              <button v-if="row.status === 'PENDING'" class="btn btn-sm btn-danger-text" type="button" @click="revokeTarget = row">
                <Icon name="undo" :size="14" />撤回
              </button>
              <button v-if="row.status === 'APPROVED'" class="btn btn-sm btn-primary" type="button" @click="openCancel(row)">
                <Icon name="check" :size="14" />申请销假
              </button>
              <Icon name="chevron-right" :size="16" style="color:#c7c7cc" />
            </div>
          </div>
        </div>
        <Pagination :total="total" :page="page" :size="size" @change="changePage" />
      </template>
    </div>

    <!-- 撤回确认 -->
    <Modal :show="!!revokeTarget" title="撤回申请" width="380px" @close="revokeTarget = null">
      <p style="color:var(--text-2);font-size:14px">确定撤回这条请假申请吗？撤回后不可恢复，如需请假请重新提交。</p>
      <template #footer>
        <button class="btn btn-secondary" type="button" @click="revokeTarget = null">取消</button>
        <button class="btn btn-danger" type="button" :disabled="revoking" @click="doRevoke">确认撤回</button>
      </template>
    </Modal>

    <!-- 销假申请 -->
    <Modal :show="!!cancelTarget" title="申请销假" width="420px" @close="cancelTarget = null">
      <p style="color:var(--text-2);font-size:13.5px;margin-bottom:14px">
        已返校？提交销假申请后，辅导员确认即完成本次请假。
      </p>
      <div class="field">
        <label class="field-label">销假说明</label>
        <textarea v-model="cancelNote" class="textarea" placeholder="如：已于15日晚返校" rows="3"></textarea>
      </div>
      <template #footer>
        <button class="btn btn-secondary" type="button" @click="cancelTarget = null">取消</button>
        <button class="btn btn-primary" type="button" :disabled="canceling" @click="doCancelApply">提交销假</button>
      </template>
    </Modal>
  </div>
</template>

<style scoped>
.page-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 22px;
}
.leave-row { gap: 14px; }
.row-main { flex: 1; min-width: 0; }
.row-line1 { display: flex; align-items: center; gap: 10px; margin-bottom: 3px; }
.row-type { font-size: 15px; font-weight: 600; }
.row-reason {
  font-size: 13.5px; color: var(--text-1);
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
  margin-bottom: 5px;
}
.row-meta { display: flex; gap: 16px; flex-wrap: wrap; }
.meta-item { display: inline-flex; align-items: center; gap: 4px; font-size: 12.5px; color: var(--text-2); }
.row-actions { display: flex; align-items: center; gap: 8px; flex-shrink: 0; }
</style>
