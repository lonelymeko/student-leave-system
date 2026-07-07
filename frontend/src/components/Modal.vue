<script setup>
import Icon from './Icon.vue'

defineProps({
  show: Boolean,
  title: { type: String, default: '' },
  width: { type: String, default: '480px' },
  closable: { type: Boolean, default: true }
})
const emit = defineEmits(['close'])
</script>

<template>
  <Teleport to="body">
    <Transition name="fade">
      <div v-if="show" class="modal-mask" @click.self="closable && emit('close')">
        <Transition name="pop" appear>
          <div class="modal-card" :style="{ maxWidth: width }">
            <div class="modal-head">
              <h3>{{ title }}</h3>
              <button v-if="closable" class="modal-close" type="button" @click="emit('close')">
                <Icon name="x" :size="15" />
              </button>
            </div>
            <div class="modal-body">
              <slot />
            </div>
            <div v-if="$slots.footer" class="modal-footer">
              <slot name="footer" />
            </div>
          </div>
        </Transition>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, .32);
  backdrop-filter: blur(6px);
  -webkit-backdrop-filter: blur(6px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 24px;
}
.modal-card {
  width: 100%;
  background: rgba(255, 255, 255, .86);
  backdrop-filter: var(--blur);
  -webkit-backdrop-filter: var(--blur);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-pop);
  border: .5px solid rgba(255, 255, 255, .7);
  max-height: 88vh;
  display: flex;
  flex-direction: column;
}
.modal-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px 12px;
}
.modal-head h3 { font-size: 19px; font-weight: 700; letter-spacing: -.2px; }
.modal-close {
  width: 28px; height: 28px;
  border-radius: 50%;
  border: none;
  background: rgba(0, 0, 0, .06);
  color: var(--text-2);
  cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  transition: all .15s ease-out;
}
.modal-close:hover { background: rgba(0, 0, 0, .1); color: var(--text-1); }
.modal-body { padding: 8px 24px 20px; overflow-y: auto; }
.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 14px 24px 20px;
  border-top: .5px solid var(--separator);
}
</style>
