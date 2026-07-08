<script setup>
// iOS 风底部动作面板（毛玻璃降级：高透明白 + 阴影）
defineProps({
  show: { type: Boolean, default: false },
  title: { type: String, default: '' }
})
const emit = defineEmits(['close'])
</script>

<template>
  <view v-if="show">
    <view class="sheet-mask" @click="emit('close')" />
    <view class="sheet">
      <view class="sheet-grabber" />
      <view v-if="title" class="sheet-title">{{ title }}</view>
      <scroll-view scroll-y class="sheet-body">
        <slot />
      </scroll-view>
      <view class="sheet-footer">
        <slot name="footer" />
      </view>
    </view>
  </view>
</template>
