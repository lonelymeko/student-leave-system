<script setup>
// iOS 分段控件（可横向滚动，选项多时不挤压）
defineProps({
  options: { type: Array, default: () => [] }, // [{value,label}]
  modelValue: { type: String, default: '' }
})
const emit = defineEmits(['update:modelValue'])
</script>

<template>
  <scroll-view scroll-x class="seg-scroll" :show-scrollbar="false">
    <view class="segmented">
      <view
        v-for="o in options"
        :key="o.value"
        class="seg-item"
        :class="{ active: modelValue === o.value }"
        @click="emit('update:modelValue', o.value)"
      >{{ o.label }}</view>
    </view>
  </scroll-view>
</template>

<style scoped>
.seg-scroll { white-space: nowrap; width: 100%; }
</style>
