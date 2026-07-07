<script setup>
import { ref, nextTick } from 'vue'
import Icon from './Icon.vue'
import { aiChat } from '../api'

const open = ref(false)
const input = ref('')
const sending = ref(false)
const listEl = ref(null)
const messages = ref([
  { role: 'ai', text: '你好，我是请假助手，可以回答关于学生请销假制度的问题，例如「病假超过3天需要什么材料？」' }
])

async function scrollBottom() {
  await nextTick()
  if (listEl.value) listEl.value.scrollTop = listEl.value.scrollHeight
}

async function send() {
  const text = input.value.trim()
  if (!text || sending.value) return
  messages.value.push({ role: 'me', text })
  input.value = ''
  sending.value = true
  scrollBottom()
  try {
    const data = await aiChat(text)
    messages.value.push({ role: 'ai', text: data?.reply || '（无回复）' })
  } catch (e) {
    messages.value.push({
      role: 'ai',
      text: e?.code === 5001 ? 'AI 服务暂不可用（未配置 API Key），请稍后再试或人工咨询辅导员。' : `出错了：${e.message || '请求失败'}`,
      error: true
    })
  } finally {
    sending.value = false
    scrollBottom()
  }
}
</script>

<template>
  <div class="ai-root">
    <Transition name="pop">
      <div v-if="open" class="ai-panel card">
        <div class="ai-head">
          <div class="ai-title">
            <span class="ai-avatar"><Icon name="sparkle" :size="15" /></span>
            <span>AI 助手</span>
          </div>
          <button class="ai-close" type="button" @click="open = false"><Icon name="x" :size="14" /></button>
        </div>
        <div ref="listEl" class="ai-list">
          <div v-for="(m, i) in messages" :key="i" class="ai-msg" :class="m.role">
            <div class="bubble" :class="{ error: m.error }">{{ m.text }}</div>
          </div>
          <div v-if="sending" class="ai-msg ai">
            <div class="bubble typing"><span></span><span></span><span></span></div>
          </div>
        </div>
        <form class="ai-input" @submit.prevent="send">
          <input v-model="input" class="input" placeholder="输入关于请假制度的问题…" :disabled="sending" />
          <button class="btn btn-primary ai-send" type="submit" :disabled="sending || !input.trim()">
            <Icon name="send" :size="15" />
          </button>
        </form>
      </div>
    </Transition>

    <button class="ai-fab" type="button" :class="{ active: open }" @click="open = !open" title="AI 助手">
      <Icon :name="open ? 'x' : 'sparkle'" :size="22" />
    </button>
  </div>
</template>

<style scoped>
.ai-root { position: fixed; right: 26px; bottom: 26px; z-index: 900; }
.ai-fab {
  width: 52px; height: 52px;
  border-radius: 50%;
  border: none;
  background: var(--accent);
  color: #fff;
  cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 8px 24px rgba(0, 113, 227, .4);
  transition: all .2s ease-out;
}
.ai-fab:hover { background: var(--accent-hover); transform: scale(1.06); }
.ai-fab.active { background: var(--text-1); box-shadow: 0 8px 24px rgba(0, 0, 0, .25); }

.ai-panel {
  position: absolute;
  right: 0; bottom: 66px;
  width: 360px;
  height: 480px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: rgba(255, 255, 255, .85);
}
.ai-head {
  display: flex; align-items: center; justify-content: space-between;
  padding: 14px 16px;
  border-bottom: .5px solid var(--separator);
}
.ai-title { display: flex; align-items: center; gap: 8px; font-weight: 600; font-size: 15px; }
.ai-avatar {
  width: 26px; height: 26px; border-radius: 50%;
  background: var(--accent);
  color: #fff;
  display: flex; align-items: center; justify-content: center;
}
.ai-close {
  width: 26px; height: 26px; border-radius: 50%; border: none;
  background: rgba(0, 0, 0, .06); color: var(--text-2); cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  transition: background .15s ease-out;
}
.ai-close:hover { background: rgba(0, 0, 0, .1); }

.ai-list { flex: 1; overflow-y: auto; padding: 14px; display: flex; flex-direction: column; gap: 10px; }
.ai-msg { display: flex; }
.ai-msg.me { justify-content: flex-end; }
.bubble {
  max-width: 82%;
  padding: 9px 13px;
  border-radius: 16px;
  font-size: 13.5px;
  line-height: 1.55;
  white-space: pre-wrap;
  word-break: break-word;
}
.ai-msg.ai .bubble { background: rgba(0, 0, 0, .05); color: var(--text-1); border-bottom-left-radius: 5px; }
.ai-msg.me .bubble { background: var(--accent); color: #fff; border-bottom-right-radius: 5px; }
.bubble.error { background: var(--red-soft); color: #d70015; }

.typing { display: flex; gap: 4px; align-items: center; padding: 12px 14px; }
.typing span {
  width: 6px; height: 6px; border-radius: 50%;
  background: var(--text-2);
  animation: blink 1.2s infinite;
}
.typing span:nth-child(2) { animation-delay: .2s; }
.typing span:nth-child(3) { animation-delay: .4s; }
@keyframes blink { 0%, 60%, 100% { opacity: .25; } 30% { opacity: 1; } }

.ai-input {
  display: flex; gap: 8px;
  padding: 12px 14px;
  border-top: .5px solid var(--separator);
}
.ai-send { padding: 9px 14px; }
</style>
