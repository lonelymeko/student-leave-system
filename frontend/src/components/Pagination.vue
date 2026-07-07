<script setup>
import { computed } from 'vue'
import Icon from './Icon.vue'

const props = defineProps({
  total: { type: Number, default: 0 },
  page: { type: Number, default: 1 },
  size: { type: Number, default: 10 }
})
const emit = defineEmits(['change'])

const pages = computed(() => Math.max(1, Math.ceil(props.total / props.size)))
</script>

<template>
  <div v-if="total > 0" class="pager">
    <span class="pager-info">共 {{ total }} 条 · 第 {{ page }}/{{ pages }} 页</span>
    <div class="pager-btns">
      <button type="button" :disabled="page <= 1" @click="emit('change', page - 1)">
        <Icon name="chevron-left" :size="15" />
      </button>
      <button type="button" :disabled="page >= pages" @click="emit('change', page + 1)">
        <Icon name="chevron-right" :size="15" />
      </button>
    </div>
  </div>
</template>

<style scoped>
.pager {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px;
}
.pager-info { font-size: 13px; color: var(--text-2); }
.pager-btns { display: flex; gap: 8px; }
.pager-btns button {
  width: 30px; height: 30px;
  border-radius: 50%;
  border: none;
  background: rgba(0, 0, 0, .05);
  color: var(--text-1);
  cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  transition: all .15s ease-out;
}
.pager-btns button:hover:not(:disabled) { background: var(--accent-soft); color: var(--accent); }
.pager-btns button:disabled { opacity: .35; cursor: not-allowed; }
</style>
