<script setup>
import { statusText, statusPill } from '../utils/constants'

defineProps({ status: { type: String, default: '' } })
</script>

<template>
  <span class="pill" :class="statusPill(status)">{{ statusText(status) }}</span>
</template>
