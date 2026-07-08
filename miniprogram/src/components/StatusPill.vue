<script setup>
import { computed } from 'vue'
import { statusText, statusPill } from '../utils/constants'

const props = defineProps({
  status: { type: String, default: '' }
})

const text = computed(() => statusText(props.status))
const pill = computed(() => statusPill(props.status))
</script>

<template>
  <view class="pill" :class="pill">{{ text }}</view>
</template>
