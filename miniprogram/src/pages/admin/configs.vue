<script setup>
// 管理员 — 系统配置：列表(configKey/description/当前值) + SheetModal 编辑 value 保存
import { ref } from 'vue'
import { onShow, onPullDownRefresh } from '@dcloudio/uni-app'
import { getConfigs, updateConfig } from '../../api'
import AppIcon from '../../components/AppIcon.vue'
import EmptyBox from '../../components/EmptyBox.vue'
import TabBar from '../../components/TabBar.vue'
import SheetModal from '../../components/SheetModal.vue'
import { getToken, getUser } from '../../utils/auth'

const list = ref([])
const loading = ref(false)

async function load() {
  if (loading.value) return
  loading.value = true
  try {
    list.value = (await getConfigs()) || []
  } catch (e) {
    list.value = []
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

// ---- 编辑弹层 ----
const editing = ref(null)
const editValue = ref('')
const saving = ref(false)

function openEdit(row) {
  editing.value = row
  editValue.value = row.configValue ?? ''
}

async function save() {
  if (saving.value) return
  saving.value = true
  try {
    await updateConfig(editing.value.configKey, editValue.value)
    uni.showToast({ title: '配置已更新', icon: 'none' })
    editing.value = null
    load()
  } catch (e) {} finally { saving.value = false }
}
</script>

<template>
  <view class="page-wrap">
    <view class="page-head">
      <view>
        <view class="large-title">系统配置</view>
        <view class="page-subtitle">请假制度、AI 供应商等运行参数</view>
      </view>
      <view class="btn btn-secondary btn-sm" @click="load">
        <AppIcon name="refresh" :size="14" color="#1d1d1f" />
        <text>刷新</text>
      </view>
    </view>

    <view class="card">
      <view v-if="loading && !list.length" class="loading-block">
        <view class="spinner" /><text>加载中…</text>
      </view>
      <EmptyBox v-else-if="!list.length" icon="gear" text="暂无配置项" />

      <template v-else>
        <view v-for="row in list" :key="row.configKey" class="group-row cfg-row" @click="openEdit(row)">
          <view class="row-main">
            <text class="cfg-key">{{ row.configKey }}</text>
            <view v-if="row.description" class="cfg-desc">{{ row.description }}</view>
            <view class="cfg-val">{{ row.configValue || '（空）' }}</view>
          </view>
          <view class="op-btn"><AppIcon name="edit" :size="15" color="#6e6e73" /></view>
        </view>
      </template>
    </view>

    <!-- 编辑弹层 -->
    <SheetModal :show="!!editing" title="编辑配置" @close="editing = null">
      <template v-if="editing">
        <view class="field" style="margin-bottom: 14px">
          <text class="field-label">配置键</text>
          <input class="input" :value="editing.configKey" disabled />
        </view>
        <view v-if="editing.description" class="field" style="margin-bottom: 14px">
          <text class="field-label">说明</text>
          <view class="cfg-desc-full">{{ editing.description }}</view>
        </view>
        <view class="field">
          <text class="field-label">配置值</text>
          <textarea v-model="editValue" class="textarea" placeholder="请输入配置值" placeholder-class="ph" />
        </view>
      </template>
      <template #footer>
        <view class="btn btn-secondary" @click="editing = null">取消</view>
        <view class="btn btn-primary" :class="{ disabled: saving }" @click="save">{{ saving ? '保存中…' : '保存' }}</view>
      </template>
    </SheetModal>

    <TabBar current="admin-configs" />
  </view>
</template>

<style scoped>
.page-head { display: flex; align-items: flex-end; justify-content: space-between; margin-bottom: 18px; }
.cfg-row { gap: 10px; align-items: center; }
.row-main { flex: 1; min-width: 0; }
.cfg-key {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 13px; font-weight: 600; color: var(--accent);
}
.cfg-desc { font-size: 12.5px; color: var(--text-2); margin-top: 3px; }
.cfg-val {
  font-size: 13.5px; color: var(--text-1); margin-top: 5px;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.cfg-desc-full { font-size: 13.5px; color: var(--text-2); line-height: 1.6; }
.op-btn {
  width: 32px; height: 32px; border-radius: 9px;
  background: rgba(0, 0, 0, .05);
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.op-btn:active { background: rgba(0, 0, 0, .09); }
</style>
