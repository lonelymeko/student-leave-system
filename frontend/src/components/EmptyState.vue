<script setup>
import Icon from './Icon.vue'

defineProps({
  icon: { type: String, default: 'inbox' },
  text: { type: String, default: '暂无数据' }
})
</script>

<template>
  <div class="empty-state">
    <div class="icon-wrap"><Icon :name="icon" :size="26" /></div>
    <span style="font-size:13.5px">{{ text }}</span>
    <slot />
  </div>
</template>
