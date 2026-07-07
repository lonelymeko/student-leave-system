<script setup>
defineProps({
  options: { type: Array, required: true }, // [{value, label}]
  modelValue: { type: [String, Number], default: '' }
})
const emit = defineEmits(['update:modelValue'])
</script>

<template>
  <div class="segmented">
    <button
      v-for="opt in options"
      :key="opt.value"
      type="button"
      :class="{ active: modelValue === opt.value }"
      @click="emit('update:modelValue', opt.value)"
    >{{ opt.label }}</button>
  </div>
</template>
