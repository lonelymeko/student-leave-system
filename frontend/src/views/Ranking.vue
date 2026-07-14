<script setup>
import { ref, onMounted } from 'vue'
import { getRanking, exportLeaves } from '../api'
import Icon from '../components/Icon.vue'
import EmptyState from '../components/EmptyState.vue'
import LoadingBlock from '../components/LoadingBlock.vue'
import { toast } from '../utils/toast'

const list = ref([])
const loading = ref(false)
const exporting = ref(false)

async function load() {
  loading.value = true
  try {
    const data = await getRanking()
    list.value = Array.isArray(data) ? data : []
  } catch (e) { list.value = [] } finally { loading.value = false }
}
onMounted(load)

// 前 3 名徽章样式
const MEDALS = { 1: 'medal-gold', 2: 'medal-silver', 3: 'medal-bronze' }

// 从 Content-Disposition 解析文件名，兜底写死
function parseFilename(disposition) {
  if (!disposition) return '请假数据.xlsx'
  const star = /filename\*=(?:UTF-8'')?([^;]+)/i.exec(disposition)
  if (star && star[1]) { try { return decodeURIComponent(star[1].trim().replace(/"/g, '')) } catch (e) {} }
  const plain = /filename="?([^";]+)"?/i.exec(disposition)
  if (plain && plain[1]) return plain[1].trim()
  return '请假数据.xlsx'
}

async function doExport() {
  if (exporting.value) return
  exporting.value = true
  try {
    const res = await exportLeaves()
    const blob = new Blob([res.data], {
      type: res.data?.type || 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    })
    const filename = parseFilename(res.headers?.['content-disposition'])
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = filename
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
    toast.success('已导出请假数据')
  } catch (e) {
    if (!e?.silent) toast.error('导出失败，请稍后重试')
  } finally {
    exporting.value = false
  }
}
</script>

<template>
  <div>
    <div class="page-head">
      <div>
        <h1 class="large-title">请假排名</h1>
        <p class="page-subtitle">按有效请假次数降序（不含已撤回）</p>
      </div>
      <button class="btn btn-primary btn-sm export-btn" type="button" :disabled="exporting" @click="doExport">
        <span v-if="exporting" class="spinner ex-spin"></span>
        <Icon v-else name="inbox" :size="14" />
        {{ exporting ? '导出中…' : '导出 Excel' }}
      </button>
    </div>

    <div class="card">
      <LoadingBlock v-if="loading" />
      <EmptyState v-else-if="!list.length" icon="chart" text="暂无请假数据" />

      <table v-else class="apple-table rank-table">
        <thead>
          <tr><th style="width:80px">排名</th><th>姓名</th><th>学号</th><th>班级</th><th style="text-align:right">请假次数</th></tr>
        </thead>
        <tbody>
          <tr v-for="(row, i) in list" :key="row.studentId">
            <td>
              <span v-if="i < 3" class="medal" :class="MEDALS[i + 1]">{{ i + 1 }}</span>
              <span v-else class="rank-num">{{ i + 1 }}</span>
            </td>
            <td><b>{{ row.studentName }}</b></td>
            <td style="color:var(--text-2)">{{ row.studentNo }}</td>
            <td style="color:var(--text-2)">{{ row.className }}</td>
            <td style="text-align:right">
              <span class="count-pill" :class="{ top: i < 3 }">{{ row.leaveCount }} 次</span>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<style scoped>
.page-head { display: flex; align-items: flex-end; justify-content: space-between; margin-bottom: 22px; gap: 14px; flex-wrap: wrap; }
.export-btn { flex-shrink: 0; }
.ex-spin { width: 13px; height: 13px; border-width: 2px; border-color: rgba(255,255,255,.4); border-top-color: #fff; }

.rank-table td { vertical-align: middle; }
.rank-num { display: inline-block; width: 26px; text-align: center; color: var(--text-2); font-size: 14px; }
.medal {
  display: inline-flex; align-items: center; justify-content: center;
  width: 26px; height: 26px; border-radius: 50%;
  font-size: 13px; font-weight: 700; color: #fff;
  box-shadow: 0 3px 8px rgba(0, 0, 0, .12);
}
.medal-gold { background: linear-gradient(135deg, #ffd76a, #f0a800); }
.medal-silver { background: linear-gradient(135deg, #d8dde3, #a7afb8); }
.medal-bronze { background: linear-gradient(135deg, #e6a877, #c17b3f); }

.count-pill {
  display: inline-block;
  padding: 3px 12px;
  border-radius: var(--radius-pill);
  font-size: 13px; font-weight: 600;
  background: var(--gray-soft); color: var(--text-2);
}
.count-pill.top { background: var(--accent-soft); color: var(--accent); }
</style>
