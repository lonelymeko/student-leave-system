<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { getAllLeaves } from '../../api'
import Icon from '../../components/Icon.vue'
import Segmented from '../../components/Segmented.vue'
import StatusPill from '../../components/StatusPill.vue'
import EmptyState from '../../components/EmptyState.vue'
import LoadingBlock from '../../components/LoadingBlock.vue'
import Pagination from '../../components/Pagination.vue'
import { typeText, fmtTime } from '../../utils/constants'

const router = useRouter()

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

async function load() {
  loading.value = true
  try {
    const data = await getAllLeaves({
      status: status.value || undefined,
      keyword: keyword.value.trim() || undefined,
      page: page.value, size
    })
    list.value = data?.records || []
    total.value = data?.total || 0
  } catch (e) { list.value = [] } finally { loading.value = false }
}

watch(status, () => { page.value = 1; load() })
onMounted(load)
function search() { page.value = 1; load() }
function changePage(p) { page.value = p; load() }
</script>

<template>
  <div>
    <div class="page-head">
      <h1 class="large-title">全部请假</h1>
      <p class="page-subtitle">全校请假记录查询</p>
    </div>

    <div class="toolbar">
      <Segmented v-model="status" :options="FILTERS" />
      <form class="search-wrap" @submit.prevent="search">
        <Icon name="search" :size="15" class="search-icon" />
        <input v-model="keyword" class="input search-input" placeholder="搜索学生 / 学号" @keyup.enter="search" />
      </form>
    </div>

    <div class="card">
      <LoadingBlock v-if="loading" />
      <EmptyState v-else-if="!list.length" text="没有符合条件的请假记录" />

      <template v-else>
        <table class="apple-table">
          <thead>
            <tr><th>学生</th><th>类型</th><th>起止时间</th><th>天数</th><th>事由</th><th>状态</th><th>审批人</th><th>提交时间</th><th></th></tr>
          </thead>
          <tbody>
            <tr v-for="row in list" :key="row.id" style="cursor:pointer" @click="router.push(`/leave/${row.id}`)">
              <td>
                <b>{{ row.studentName }}</b>
                <div style="font-size:12px;color:var(--text-2)">{{ row.studentNo }} · {{ row.className }}</div>
              </td>
              <td>{{ row.typeText || typeText(row.type) }}</td>
              <td style="font-size:13px">{{ fmtTime(row.startTime) }}<br>→ {{ fmtTime(row.endTime) }}</td>
              <td>{{ row.days }}</td>
              <td class="reason-cell">{{ row.reason }}</td>
              <td><StatusPill :status="row.status" /></td>
              <td>{{ row.approverName || '-' }}</td>
              <td style="font-size:13px;color:var(--text-2)">{{ fmtTime(row.createTime) }}</td>
              <td><Icon name="chevron-right" :size="15" style="color:#c7c7cc" /></td>
            </tr>
          </tbody>
        </table>
        <Pagination :total="total" :page="page" :size="size" @change="changePage" />
      </template>
    </div>
  </div>
</template>

<style scoped>
.page-head { margin-bottom: 20px; }
.toolbar {
  display: flex; align-items: center; justify-content: space-between;
  gap: 14px; margin-bottom: 16px; flex-wrap: wrap;
}
.search-wrap { position: relative; width: 260px; }
.search-icon { position: absolute; left: 12px; top: 50%; transform: translateY(-50%); color: var(--text-2); }
.search-input { padding-left: 34px; border-radius: var(--radius-pill); }
.reason-cell { max-width: 180px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
</style>
