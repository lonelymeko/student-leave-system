<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getNotifications, readNotification } from '../api'
import { useNotifyStore } from '../stores/notify'
import Icon from '../components/Icon.vue'
import EmptyState from '../components/EmptyState.vue'
import LoadingBlock from '../components/LoadingBlock.vue'
import Pagination from '../components/Pagination.vue'
import { fmtTime } from '../utils/constants'

const router = useRouter()
const notify = useNotifyStore()

const page = ref(1)
const size = 10
const total = ref(0)
const list = ref([])
const loading = ref(false)
const marking = ref(false)

async function load() {
  loading.value = true
  try {
    const data = await getNotifications({ page: page.value, size })
    list.value = data?.records || []
    total.value = data?.total || 0
  } catch (e) { list.value = [] } finally { loading.value = false }
}

onMounted(() => { load(); notify.refresh() })
function changePage(p) { page.value = p; load() }

// 点击单条：标记已读 → 跳转关联业务
async function open(row) {
  if (!row.isRead) {
    try {
      await readNotification(row.id)
      row.isRead = 1
      notify.decrement(1)
    } catch (e) { /* 已提示 */ }
  }
  if (row.bizType === 'APPROVAL_RESULT' && row.bizId) {
    router.push(`/leave/${row.bizId}`)
  }
}

// 全部已读
async function markAll() {
  const unread = list.value.filter(r => !r.isRead)
  if (!unread.length) return
  marking.value = true
  try {
    for (const r of unread) {
      await readNotification(r.id)
      r.isRead = 1
    }
    notify.refresh()
  } catch (e) { /* 已提示 */ } finally { marking.value = false }
}
</script>

<template>
  <div>
    <div class="page-head">
      <div>
        <h1 class="large-title">消息通知</h1>
        <p class="page-subtitle">审批结果与销假进度提醒</p>
      </div>
      <button
        v-if="list.some(r => !r.isRead)"
        class="btn btn-secondary btn-sm" type="button" :disabled="marking" @click="markAll"
      >
        <Icon name="check" :size="15" />全部已读
      </button>
    </div>

    <div class="card">
      <LoadingBlock v-if="loading" />
      <EmptyState v-else-if="!list.length" icon="bell" text="暂无通知" />

      <template v-else>
        <div class="group-list">
          <div
            v-for="row in list"
            :key="row.id"
            class="group-row clickable notify-row"
            :class="{ unread: !row.isRead }"
            @click="open(row)"
          >
            <span class="dot" :class="{ on: !row.isRead }"></span>
            <div class="notify-main">
              <div class="notify-line1">
                <span class="notify-title">{{ row.title }}</span>
                <span class="notify-time">{{ fmtTime(row.createTime, true) }}</span>
              </div>
              <div class="notify-content">{{ row.content }}</div>
            </div>
            <Icon
              v-if="row.bizType === 'APPROVAL_RESULT' && row.bizId"
              name="chevron-right" :size="16" style="color:#c7c7cc;flex-shrink:0"
            />
          </div>
        </div>
        <Pagination :total="total" :page="page" :size="size" @change="changePage" />
      </template>
    </div>
  </div>
</template>

<style scoped>
.page-head { display: flex; align-items: flex-end; justify-content: space-between; margin-bottom: 22px; }

.notify-row { gap: 12px; align-items: flex-start; }
.notify-row.unread { background: var(--accent-soft); }
.notify-row.unread:hover { background: rgba(0, 113, 227, .14); }

.dot {
  width: 8px; height: 8px; border-radius: 50%;
  margin-top: 6px; flex-shrink: 0;
  background: transparent;
}
.dot.on { background: var(--accent); box-shadow: 0 0 0 3px var(--accent-soft); }

.notify-main { flex: 1; min-width: 0; }
.notify-line1 { display: flex; align-items: baseline; justify-content: space-between; gap: 12px; }
.notify-title { font-size: 14.5px; font-weight: 600; }
.notify-time { font-size: 12px; color: var(--text-2); white-space: nowrap; flex-shrink: 0; }
.notify-content {
  font-size: 13.5px; color: var(--text-2); margin-top: 3px; line-height: 1.5;
}
</style>
