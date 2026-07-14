<script setup>
// 管理员 — 统计看板：数字卡 + 类型分布(带百分比条形) + 状态分布 + 近6月趋势(CSS 柱状)
// 图表用轻量 CSS 方案（横向条形 / 竖向柱形），不引 ECharts。
import { ref, computed } from 'vue'
import { onShow, onPullDownRefresh } from '@dcloudio/uni-app'
import { getStatsOverview } from '../../api'
import AppIcon from '../../components/AppIcon.vue'
import EmptyBox from '../../components/EmptyBox.vue'
import TabBar from '../../components/TabBar.vue'
import { exportLeavesFile } from '../../utils/export'
import { getToken, getUser } from '../../utils/auth'

const loading = ref(false)
const failed = ref(false)
const stats = ref(null)

// 类型分布配色（与 Web 端一致）
const TYPE_COLORS = ['#0071e3', '#34c759', '#ff9500', '#af52de', '#ff3b30', '#30b0c7']

async function load() {
  if (loading.value) return
  loading.value = true
  failed.value = false
  try {
    stats.value = await getStatsOverview()
  } catch (e) {
    failed.value = true
    stats.value = null
  } finally {
    loading.value = false
    uni.stopPullDownRefresh()
  }
}

onShow(() => {
  if (!getToken()) { uni.reLaunch({ url: '/pages/login/login' }); return }
  if (getUser()?.role !== 'ADMIN') { uni.reLaunch({ url: '/pages/login/login' }); return }
  load()
})
onPullDownRefresh(load)

// 数字卡
const cards = computed(() => {
  const s = stats.value || {}
  return [
    { key: 'total', num: s.totalCount ?? 0, label: '请假总数', icon: 'doc', tint: 'blue' },
    { key: 'pending', num: s.pendingCount ?? 0, label: '待审批', icon: 'clock', tint: 'orange' },
    { key: 'approved', num: s.approvedCount ?? 0, label: '请假中', icon: 'calendar', tint: 'green' },
    { key: 'completed', num: s.completedCount ?? 0, label: '已完成', icon: 'check', tint: 'purple' }
  ]
})

// 类型分布 —> 带百分比的条形
const typeBars = computed(() => {
  const list = stats.value?.typeDist || []
  const sum = list.reduce((a, d) => a + (d.count || 0), 0) || 1
  return list.map((d, i) => ({
    name: d.name,
    count: d.count || 0,
    pct: Math.round(((d.count || 0) / sum) * 100),
    color: TYPE_COLORS[i % TYPE_COLORS.length]
  }))
})

// 状态分布 pill
const statusDist = computed(() => stats.value?.statusDist || [])

// 近6月趋势 —> 竖向柱形
const trend = computed(() => {
  const list = stats.value?.monthTrend || []
  const max = Math.max(1, ...list.map(d => d.count || 0))
  return list.map(d => ({
    month: (d.month || '').slice(5), // "2026-07" -> "07"
    count: d.count || 0,
    h: Math.round(((d.count || 0) / max) * 100)
  }))
})

function goLeaves() {
  uni.navigateTo({ url: '/pages/admin/leaves' })
}

// 导出
const exporting = ref(false)
async function doExport() {
  if (exporting.value) return
  exporting.value = true
  try {
    await exportLeavesFile('')
  } catch (e) {} finally { exporting.value = false }
}
</script>

<template>
  <view class="page-wrap">
    <view class="page-head">
      <view>
        <view class="large-title">统计看板</view>
        <view class="page-subtitle">全校请假数据总览</view>
      </view>
      <view class="btn btn-secondary btn-sm" :class="{ disabled: exporting }" @click="doExport">
        <AppIcon name="download" :size="14" color="#1d1d1f" />
        <text>{{ exporting ? '导出中…' : '导出Excel' }}</text>
      </view>
    </view>

    <view v-if="loading && !stats" class="card">
      <view class="loading-block"><view class="spinner" /><text>加载中…</text></view>
    </view>
    <view v-else-if="failed" class="card">
      <EmptyBox icon="warning" text="统计数据加载失败" />
    </view>

    <template v-else-if="stats">
      <!-- 数字卡 -->
      <view class="stat-grid">
        <view v-for="c in cards" :key="c.key" class="card stat-card">
          <view class="stat-icon" :class="'tint-' + c.tint">
            <AppIcon :name="c.icon" :size="18" :color="'#ffffff'" />
          </view>
          <view class="stat-num">{{ c.num }}</view>
          <view class="stat-label">{{ c.label }}</view>
        </view>
      </view>

      <!-- 类型分布 -->
      <view class="card card-pad section">
        <view class="section-title">请假类型分布</view>
        <EmptyBox v-if="!typeBars.length" text="暂无数据" />
        <view v-else class="bar-list">
          <view v-for="b in typeBars" :key="b.name" class="bar-row">
            <view class="bar-head">
              <text class="bar-name">{{ b.name }}</text>
              <text class="bar-val">{{ b.count }} · {{ b.pct }}%</text>
            </view>
            <view class="bar-track">
              <view class="bar-fill" :style="{ width: b.pct + '%', background: b.color }" />
            </view>
          </view>
        </view>
      </view>

      <!-- 近6月趋势 -->
      <view class="card card-pad section">
        <view class="section-title">近 6 个月请假趋势</view>
        <EmptyBox v-if="!trend.length" text="暂无数据" />
        <view v-else class="trend">
          <view v-for="t in trend" :key="t.month" class="trend-col">
            <text class="trend-count">{{ t.count }}</text>
            <view class="trend-bar-wrap">
              <view class="trend-bar" :style="{ height: Math.max(t.h, 4) + '%' }" />
            </view>
            <text class="trend-month">{{ t.month }}</text>
          </view>
        </view>
      </view>

      <!-- 状态分布 -->
      <view class="card card-pad section" v-if="statusDist.length">
        <view class="section-title">状态分布</view>
        <view class="status-dist">
          <view v-for="d in statusDist" :key="d.status" class="status-item">
            <text class="status-name">{{ d.name }}</text>
            <text class="status-count">{{ d.count }}</text>
          </view>
        </view>
      </view>

      <!-- 全部请假入口 -->
      <view class="card entry-card" @click="goLeaves">
        <view class="group-row entry-row">
          <view class="entry-badge"><AppIcon name="list" :size="16" color="#ffffff" /></view>
          <view class="entry-main">
            <view class="entry-title">全部请假</view>
            <view class="entry-desc">查询全校请假记录 · 按状态筛选</view>
          </view>
          <AppIcon name="chevron-right" :size="16" color="#c7c7cc" />
        </view>
      </view>
    </template>

    <TabBar current="admin-dashboard" />
  </view>
</template>

<style scoped>
.page-head { display: flex; align-items: flex-end; justify-content: space-between; margin-bottom: 18px; }

.stat-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin-bottom: 16px;
}
.stat-card { padding: 16px 16px 15px; }
.stat-icon {
  width: 36px; height: 36px;
  border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  margin-bottom: 12px;
}
.tint-blue { background: var(--accent); }
.tint-orange { background: var(--orange); }
.tint-green { background: var(--green); }
.tint-purple { background: var(--purple); }
.stat-num { font-size: 28px; font-weight: 700; letter-spacing: -.6px; line-height: 1.1; }
.stat-label { font-size: 12.5px; color: var(--text-2); margin-top: 2px; }

.section { margin-bottom: 16px; padding: 18px 18px 20px; }
.section-title { font-size: 15px; font-weight: 700; margin-bottom: 16px; }

/* 类型分布条形 */
.bar-list { display: flex; flex-direction: column; gap: 15px; }
.bar-head { display: flex; align-items: baseline; justify-content: space-between; margin-bottom: 7px; }
.bar-name { font-size: 13.5px; font-weight: 600; }
.bar-val { font-size: 12px; color: var(--text-2); }
.bar-track { height: 8px; border-radius: 4px; background: rgba(0, 0, 0, .06); overflow: hidden; }
.bar-fill { height: 100%; border-radius: 4px; transition: width .4s ease-out; }

/* 趋势柱形 */
.trend { display: flex; align-items: flex-end; gap: 8px; height: 150px; padding-top: 6px; }
.trend-col { flex: 1; display: flex; flex-direction: column; align-items: center; height: 100%; }
.trend-count { font-size: 11.5px; color: var(--text-2); margin-bottom: 5px; font-weight: 600; }
.trend-bar-wrap { flex: 1; width: 100%; display: flex; align-items: flex-end; justify-content: center; }
.trend-bar {
  width: 60%; max-width: 26px;
  border-radius: 5px 5px 0 0;
  background: linear-gradient(180deg, #0071e3, #3d97f0);
  min-height: 4px;
  transition: height .4s ease-out;
}
.trend-month { font-size: 11.5px; color: var(--text-2); margin-top: 7px; }

/* 状态分布 */
.status-dist { display: flex; gap: 10px; flex-wrap: wrap; }
.status-item {
  display: flex; align-items: center; gap: 8px;
  background: rgba(0, 0, 0, .04);
  border-radius: var(--radius-pill);
  padding: 6px 14px;
}
.status-name { font-size: 12.5px; color: var(--text-2); }
.status-count { font-size: 14px; font-weight: 700; }

/* 全部请假入口 */
.entry-card:active { background: rgba(0, 0, 0, .02); }
.entry-row { gap: 12px; }
.entry-badge {
  width: 34px; height: 34px; border-radius: 10px;
  background: linear-gradient(135deg, #0071e3, #30b0c7);
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 4px 12px rgba(0, 113, 227, .35);
  flex-shrink: 0;
}
.entry-main { flex: 1; min-width: 0; }
.entry-title { font-size: 15px; font-weight: 600; }
.entry-desc { font-size: 12px; color: var(--text-2); }
</style>
