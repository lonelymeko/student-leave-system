<script setup>
// 请假次数排名（LEADER / ADMIN 全部学生；TEACHER 仅名下学生）
// 出参 [{ studentId, studentName, studentNo, className, leaveCount }]，已按次数降序（排除 REVOKED）。
import { ref } from 'vue'
import { onShow, onPullDownRefresh } from '@dcloudio/uni-app'
import { getRanking } from '../../api'
import AppIcon from '../../components/AppIcon.vue'
import EmptyBox from '../../components/EmptyBox.vue'
import TabBar from '../../components/TabBar.vue'
import { getToken } from '../../utils/auth'

const list = ref([])
const loading = ref(false)

async function load() {
  if (loading.value) return
  loading.value = true
  try {
    const data = await getRanking()
    list.value = Array.isArray(data) ? data : (data?.records || [])
  } catch (e) {
    list.value = []
  } finally {
    loading.value = false
    uni.stopPullDownRefresh()
  }
}

onShow(() => {
  if (!getToken()) { uni.reLaunch({ url: '/pages/login/login' }); return }
  load()
})
onPullDownRefresh(load)

// 前 3 名奖牌色（十六进制，用于内联样式/圆点）
const MEDAL = ['#f0b429', '#a0a6ad', '#c98548']
const rankColor = i => MEDAL[i] || '#c7c7cc'
</script>

<template>
  <view class="page-wrap">
    <view class="page-head">
      <view>
        <view class="large-title">请假次数排名</view>
        <view class="page-subtitle">全部学生累计请假次数（不含已撤回）</view>
      </view>
      <view class="btn btn-secondary btn-sm" @click="load">
        <AppIcon name="refresh" :size="14" color="#1d1d1f" />
        <text>刷新</text>
      </view>
    </view>

    <view class="card">
      <view v-if="loading && !list.length" class="loading-block">
        <view class="spinner" />
        <text>加载中…</text>
      </view>
      <EmptyBox v-else-if="!list.length" icon="trophy" text="暂无排名数据" />

      <template v-else>
        <view
          v-for="(row, i) in list"
          :key="row.studentId || row.studentNo || i"
          class="group-row rank-row"
          :class="{ top: i < 3 }"
        >
          <view class="rank-badge" :style="{ background: i < 3 ? rankColor(i) : 'rgba(0,0,0,.05)', color: i < 3 ? '#ffffff' : 'var(--text-2)' }">
            {{ i + 1 }}
          </view>
          <view class="rank-main">
            <view class="rank-line1">
              <text class="rank-name">{{ row.studentName }}</text>
              <text class="rank-no">{{ row.studentNo }}</text>
            </view>
            <view class="rank-class">{{ row.className || '-' }}</view>
          </view>
          <view class="rank-count">
            <text class="count-num">{{ row.leaveCount }}</text>
            <text class="count-unit">次</text>
          </view>
        </view>
      </template>
    </view>

    <TabBar current="ranking" />
  </view>
</template>

<style scoped>
.page-head { display: flex; align-items: flex-end; justify-content: space-between; margin-bottom: 18px; }
.rank-row { gap: 12px; align-items: center; }
.rank-row.top { background: linear-gradient(90deg, rgba(240, 180, 41, .06), transparent); }
.rank-badge {
  width: 30px; height: 30px; border-radius: 9px;
  display: flex; align-items: center; justify-content: center;
  font-size: 14px; font-weight: 700;
  flex-shrink: 0;
}
.rank-main { flex: 1; min-width: 0; }
.rank-line1 { display: flex; align-items: baseline; gap: 8px; }
.rank-name { font-size: 15px; font-weight: 600; }
.rank-no { font-size: 12px; color: var(--text-2); }
.rank-class { font-size: 12.5px; color: var(--text-2); margin-top: 2px; }
.rank-count { display: flex; align-items: baseline; gap: 2px; flex-shrink: 0; }
.count-num { font-size: 20px; font-weight: 700; color: var(--accent); }
.count-unit { font-size: 12px; color: var(--text-2); }
</style>
