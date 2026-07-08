<script setup>
import AppIcon from './AppIcon.vue'

defineProps({
  icon: { type: String, default: 'inbox' },
  text: { type: String, default: '暂无数据' }
})
</script>

<template>
  <view class="empty-state">
    <view class="icon-wrap">
      <AppIcon :name="icon" :size="26" color="#a1a1a6" />
    </view>
    <text style="font-size: 13.5px">{{ text }}</text>
    <slot />
  </view>
</template>
