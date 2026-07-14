<script setup>
import { ref, onMounted } from 'vue'
import { getConfigs, updateConfig } from '../../api'
import Icon from '../../components/Icon.vue'
import EmptyState from '../../components/EmptyState.vue'
import LoadingBlock from '../../components/LoadingBlock.vue'
import Modal from '../../components/Modal.vue'
import { fmtTime } from '../../utils/constants'
import { toast } from '../../utils/toast'

const list = ref([])
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    list.value = (await getConfigs()) || []
  } catch (e) { list.value = [] } finally { loading.value = false }
}
onMounted(load)

// ---- 编辑弹窗 ----
const editing = ref(null) // 当前编辑的配置行
const editValue = ref('')
const saving = ref(false)

function openEdit(row) {
  editing.value = row
  editValue.value = row.configValue ?? ''
}

async function save() {
  saving.value = true
  try {
    await updateConfig(editing.value.configKey, editValue.value)
    toast.success('配置已更新')
    editing.value = null
    load()
  } catch (e) { /* 已提示 */ } finally { saving.value = false }
}
</script>

<template>
  <div>
    <div class="page-head">
      <div>
        <h1 class="large-title">系统配置</h1>
        <p class="page-subtitle">管理请假制度、AI 供应商等运行参数</p>
      </div>
    </div>

    <div class="card">
      <LoadingBlock v-if="loading" />
      <EmptyState v-else-if="!list.length" icon="settings" text="暂无配置项" />

      <table v-else class="apple-table">
        <thead>
          <tr>
            <th style="width:200px">配置键</th>
            <th>说明</th>
            <th>当前值</th>
            <th style="width:120px">更新时间</th>
            <th style="text-align:right;width:80px">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in list" :key="row.id">
            <td><b><code class="ckey">{{ row.configKey }}</code></b></td>
            <td>{{ row.description || '-' }}</td>
            <td class="cval">{{ row.configValue || '-' }}</td>
            <td style="color:var(--text-2);white-space:nowrap">{{ fmtTime(row.updateTime) }}</td>
            <td>
              <div class="row-ops">
                <button class="op-btn" type="button" title="编辑" @click="openEdit(row)">
                  <Icon name="edit" :size="15" />
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 编辑弹窗 -->
    <Modal :show="!!editing" title="编辑配置" width="480px" @close="editing = null">
      <template v-if="editing">
        <div class="field" style="margin-bottom:14px">
          <label class="field-label">配置键</label>
          <input class="input" :value="editing.configKey" disabled />
        </div>
        <div class="field" v-if="editing.description" style="margin-bottom:14px">
          <label class="field-label">说明</label>
          <p style="font-size:13.5px;color:var(--text-2);line-height:1.6">{{ editing.description }}</p>
        </div>
        <div class="field">
          <label class="field-label">配置值</label>
          <textarea v-model="editValue" class="textarea" rows="4" placeholder="请输入配置值"></textarea>
        </div>
      </template>
      <template #footer>
        <button class="btn btn-secondary" type="button" @click="editing = null">取消</button>
        <button class="btn btn-primary" type="button" :disabled="saving" @click="save">
          {{ saving ? '保存中…' : '保存' }}
        </button>
      </template>
    </Modal>
  </div>
</template>

<style scoped>
.page-head { display: flex; align-items: flex-end; justify-content: space-between; margin-bottom: 20px; }
.ckey { font-family: ui-monospace, SFMono-Regular, Menlo, monospace; font-size: 13px; color: var(--accent); }
.cval {
  max-width: 380px;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
  color: var(--text-1);
}
.row-ops { display: flex; gap: 6px; justify-content: flex-end; }
.op-btn {
  width: 29px; height: 29px;
  border-radius: 8px;
  border: none;
  background: rgba(0, 0, 0, .05);
  color: var(--text-2);
  cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  transition: all .15s ease-out;
}
.op-btn:hover { background: var(--accent-soft); color: var(--accent); }
</style>
