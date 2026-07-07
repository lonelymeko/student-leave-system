<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as echarts from 'echarts/core'
import { PieChart, LineChart } from 'echarts/charts'
import { TooltipComponent, LegendComponent, GridComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { getStatsOverview } from '../../api'
import Icon from '../../components/Icon.vue'
import LoadingBlock from '../../components/LoadingBlock.vue'
import EmptyState from '../../components/EmptyState.vue'

echarts.use([PieChart, LineChart, TooltipComponent, LegendComponent, GridComponent, CanvasRenderer])

const loading = ref(true)
const failed = ref(false)
const stats = ref(null)

const pieEl = ref(null)
const lineEl = ref(null)
let pieChart = null
let lineChart = null

const FONT = '-apple-system, BlinkMacSystemFont, "SF Pro Text", "PingFang SC", sans-serif'
const COLORS = ['#0071e3', '#34c759', '#ff9500', '#af52de', '#ff3b30', '#30b0c7']

function renderCharts() {
  const s = stats.value
  if (!s) return

  if (pieEl.value) {
    pieChart = echarts.init(pieEl.value)
    pieChart.setOption({
      color: COLORS,
      tooltip: { trigger: 'item', textStyle: { fontFamily: FONT } },
      legend: { bottom: 0, icon: 'circle', itemWidth: 9, textStyle: { fontFamily: FONT, color: '#6e6e73', fontSize: 12 } },
      series: [{
        type: 'pie',
        radius: ['46%', '68%'],
        center: ['50%', '44%'],
        itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
        label: { show: false },
        data: (s.typeDist || []).map(d => ({ name: d.name, value: d.count }))
      }]
    })
  }

  if (lineEl.value) {
    lineChart = echarts.init(lineEl.value)
    const trend = s.monthTrend || []
    lineChart.setOption({
      tooltip: { trigger: 'axis', textStyle: { fontFamily: FONT } },
      grid: { left: 40, right: 20, top: 24, bottom: 30 },
      xAxis: {
        type: 'category',
        data: trend.map(d => d.month),
        axisLine: { lineStyle: { color: 'rgba(0,0,0,.12)' } },
        axisTick: { show: false },
        axisLabel: { fontFamily: FONT, color: '#6e6e73', fontSize: 11.5 }
      },
      yAxis: {
        type: 'value',
        minInterval: 1,
        splitLine: { lineStyle: { color: 'rgba(0,0,0,.06)' } },
        axisLabel: { fontFamily: FONT, color: '#6e6e73', fontSize: 11.5 }
      },
      series: [{
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 7,
        data: trend.map(d => d.count),
        lineStyle: { width: 2.5, color: '#0071e3' },
        itemStyle: { color: '#0071e3', borderColor: '#fff', borderWidth: 2 },
        areaStyle: {
          color: {
            type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(0,113,227,.22)' },
              { offset: 1, color: 'rgba(0,113,227,0)' }
            ]
          }
        }
      }]
    })
  }
}

function handleResize() {
  pieChart?.resize()
  lineChart?.resize()
}

onMounted(async () => {
  try {
    stats.value = await getStatsOverview()
    loading.value = false
    await nextTick()
    renderCharts()
    window.addEventListener('resize', handleResize)
  } catch (e) {
    failed.value = true
  } finally {
    loading.value = false
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  pieChart?.dispose()
  lineChart?.dispose()
})
</script>

<template>
  <div>
    <div class="page-head">
      <h1 class="large-title">统计看板</h1>
      <p class="page-subtitle">全校请假数据总览</p>
    </div>

    <LoadingBlock v-if="loading" />
    <div v-else-if="failed" class="card"><EmptyState icon="warning" text="统计数据加载失败" /></div>

    <template v-else-if="stats">
      <!-- 数字卡片 -->
      <div class="stat-grid">
        <div class="card stat-card">
          <div class="stat-icon" style="background:var(--accent-soft);color:var(--accent)"><Icon name="doc" :size="20" /></div>
          <div class="stat-num">{{ stats.totalCount ?? 0 }}</div>
          <div class="stat-label">请假总数</div>
        </div>
        <div class="card stat-card">
          <div class="stat-icon" style="background:var(--orange-soft);color:var(--orange)"><Icon name="clock" :size="20" /></div>
          <div class="stat-num">{{ stats.pendingCount ?? 0 }}</div>
          <div class="stat-label">待审批</div>
        </div>
        <div class="card stat-card">
          <div class="stat-icon" style="background:var(--green-soft);color:var(--green)"><Icon name="calendar" :size="20" /></div>
          <div class="stat-num">{{ stats.approvedCount ?? 0 }}</div>
          <div class="stat-label">请假中</div>
        </div>
        <div class="card stat-card">
          <div class="stat-icon" style="background:var(--purple-soft);color:var(--purple)"><Icon name="check" :size="20" /></div>
          <div class="stat-num">{{ stats.completedCount ?? 0 }}</div>
          <div class="stat-label">已完成</div>
        </div>
      </div>

      <!-- 图表 -->
      <div class="chart-grid">
        <div class="card chart-card">
          <div class="chart-title">请假类型分布</div>
          <div ref="pieEl" class="chart-body"></div>
        </div>
        <div class="card chart-card">
          <div class="chart-title">近 6 个月请假趋势</div>
          <div ref="lineEl" class="chart-body"></div>
        </div>
      </div>

      <!-- 状态分布 -->
      <div class="card card-pad" v-if="stats.statusDist?.length" style="margin-top:20px">
        <div class="chart-title" style="padding:0 0 14px">状态分布</div>
        <div class="status-dist">
          <div v-for="d in stats.statusDist" :key="d.status" class="status-item">
            <span class="status-name">{{ d.name }}</span>
            <span class="status-count">{{ d.count }}</span>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.page-head { margin-bottom: 22px; }

.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 18px;
  margin-bottom: 20px;
}
@media (max-width: 1000px) { .stat-grid { grid-template-columns: repeat(2, 1fr); } }
.stat-card { padding: 20px 22px; }
.stat-icon {
  width: 40px; height: 40px;
  border-radius: 11px;
  display: flex; align-items: center; justify-content: center;
  margin-bottom: 14px;
}
.stat-num { font-size: 32px; font-weight: 700; letter-spacing: -.6px; line-height: 1.1; }
.stat-label { font-size: 13px; color: var(--text-2); margin-top: 3px; }

.chart-grid {
  display: grid;
  grid-template-columns: 5fr 7fr;
  gap: 18px;
}
@media (max-width: 900px) { .chart-grid { grid-template-columns: 1fr; } }
.chart-card { padding: 20px 22px; }
.chart-title { font-size: 15px; font-weight: 700; padding-bottom: 6px; }
.chart-body { height: 280px; }

.status-dist { display: flex; gap: 12px; flex-wrap: wrap; }
.status-item {
  display: flex; align-items: center; gap: 10px;
  background: rgba(0, 0, 0, .035);
  border-radius: var(--radius-pill);
  padding: 7px 16px;
}
.status-name { font-size: 13px; color: var(--text-2); }
.status-count { font-size: 15px; font-weight: 700; }
</style>
