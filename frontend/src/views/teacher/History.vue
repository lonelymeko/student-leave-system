<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getApprovalHistory } from '../../api'
import Icon from '../../components/Icon.vue'
import StatusPill from '../../components/StatusPill.vue'
import EmptyState from '../../components/EmptyState.vue'
import LoadingBlock from '../../components/LoadingBlock.vue'
import Pagination from '../../components/Pagination.vue'
import { typeText, fmtTime } from '../../utils/constants'

const router = useRouter()

const page = ref(1)
const size = 10
const total = ref(0)
const list = ref([])
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const data = await getApprovalHistory({ page: page.value, size })
    list.value = data?.records || []
    total.value = data?.total || 0
  } catch (e) { list.value = [] } finally { loading.value = false }
}
onMounted(load)
function changePage(p) { page.value = p; load() }
</script>

<template>
  <div>
    <div class="page-head">
      <h1 class="large-title">审批历史</h1>
      <p class="page-subtitle">我处理过的请假单</p>
    </div>

    <div class="card">
      <LoadingBlock v-if="loading" />
      <EmptyState v-else-if="!list.length" icon="history" text="还没有处理过请假单" />

      <template v-else>
        <table class="apple-table">
          <thead>
            <tr>
              <th>学生</th><th>类型</th><th>起止时间</th><th>天数</th><th>事由</th><th>状态</th><th>处理时间</th><th></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in list" :key="row.id" style="cursor:pointer" @click="router.push(`/leave/${row.id}`)">
              <td><b>{{ row.studentName }}</b><div style="font-size:12px;color:var(--text-2)">{{ row.className }}</div></td>
              <td>{{ row.typeText || typeText(row.type) }}</td>
              <td style="font-size:13px">{{ fmtTime(row.startTime) }}<br>→ {{ fmtTime(row.endTime) }}</td>
              <td>{{ row.days }}</td>
              <td class="reason-cell">{{ row.reason }}</td>
              <td><StatusPill :status="row.status" /></td>
              <td style="font-size:13px;color:var(--text-2)">{{ fmtTime(row.approveTime || row.completeTime || row.createTime) }}</td>
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
.page-head { margin-bottom: 22px; }
.reason-cell { max-width: 200px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
</style>
