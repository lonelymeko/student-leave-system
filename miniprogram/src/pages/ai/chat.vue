<script setup>
// AI 制度问答 — 独立聊天页（POST /ai/chat；5001 降级为提示气泡）
import { ref, nextTick } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { aiChat } from '../../api'
import AppIcon from '../../components/AppIcon.vue'
import { getToken, getUser } from '../../utils/auth'

const messages = ref([]) // {role:'user'|'ai', text}
const input = ref('')
const sending = ref(false)
const scrollInto = ref('')
const focusI = ref(false)

const SUGGESTS = [
  '病假超过3天需要什么材料？',
  '请假流程是什么样的？',
  '销假是什么意思，怎么操作？'
]

onLoad(() => {
  if (!getToken()) { uni.reLaunch({ url: '/pages/login/login' }); return }
  const name = getUser()?.realName || ''
  messages.value.push({
    role: 'ai',
    text: `你好${name ? '，' + name : ''}！我是请销假制度小助手，关于请假类型、审批流程、销假规定的问题都可以问我。`
  })
})

async function scrollBottom() {
  await nextTick()
  scrollInto.value = ''
  await nextTick()
  scrollInto.value = `msg-${messages.value.length - 1}`
}

async function send(preset) {
  const text = (preset || input.value).trim()
  if (!text || sending.value) return
  input.value = ''
  messages.value.push({ role: 'user', text })
  sending.value = true
  scrollBottom()
  try {
    const d = await aiChat(text)
    messages.value.push({ role: 'ai', text: d?.reply || '（AI 未返回内容）' })
  } catch (e) {
    if (e?.code === 5001) {
      messages.value.push({ role: 'ai', text: 'AI 服务暂不可用（未配置模型 Key），请稍后再试或咨询辅导员。' })
    } else {
      messages.value.push({ role: 'ai', text: '请求出错了，请稍后再试。' })
    }
  } finally {
    sending.value = false
    scrollBottom()
  }
}

function back() {
  uni.navigateBack({ fail: () => uni.reLaunch({ url: '/pages/mine/mine' }) })
}
</script>

<template>
  <view class="chat-page">
    <!-- 顶部 -->
    <view class="chat-nav">
      <view class="nav-back" @click="back">
        <AppIcon name="chevron-left" :size="18" color="#0071e3" />
      </view>
      <view class="nav-title-wrap">
        <view class="nav-badge"><AppIcon name="sparkle" :size="13" color="#ffffff" /></view>
        <text class="nav-title">AI 制度问答</text>
      </view>
      <view style="width: 34px" />
    </view>

    <!-- 消息区 -->
    <scroll-view scroll-y class="chat-body" :scroll-into-view="scrollInto" scroll-with-animation>
      <view v-for="(m, i) in messages" :key="i" :id="`msg-${i}`" class="msg-row" :class="m.role">
        <view v-if="m.role === 'ai'" class="msg-avatar ai-avatar">
          <AppIcon name="sparkle" :size="14" color="#ffffff" />
        </view>
        <view class="bubble" :class="m.role">{{ m.text }}</view>
      </view>
      <view v-if="sending" class="msg-row ai" id="msg-typing">
        <view class="msg-avatar ai-avatar"><AppIcon name="sparkle" :size="14" color="#ffffff" /></view>
        <view class="bubble ai typing">思考中…</view>
      </view>

      <!-- 建议问题 -->
      <view v-if="messages.length <= 1" class="suggests">
        <view v-for="s in SUGGESTS" :key="s" class="suggest-chip" @click="send(s)">{{ s }}</view>
      </view>
      <view style="height: 16px" />
    </scroll-view>

    <!-- 输入栏 -->
    <view class="chat-input-bar">
      <input
        v-model="input"
        class="input chat-input"
        :class="{ 'input-focus': focusI }"
        placeholder="输入你的问题…"
        placeholder-class="ph"
        confirm-type="send"
        @confirm="send()"
        @focus="focusI = true" @blur="focusI = false"
      />
      <view class="send-btn" :class="{ disabled: !input.trim() || sending }" @click="send()">
        <AppIcon name="send" :size="18" color="#ffffff" />
      </view>
    </view>
  </view>
</template>

<style scoped>
.chat-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--bg);
  box-sizing: border-box;
}
.chat-nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: calc(var(--status-bar-height) + 10px) 12px 10px;
  background: rgba(245, 245, 247, .92);
  backdrop-filter: saturate(180%) blur(20px);
  -webkit-backdrop-filter: saturate(180%) blur(20px);
  border-bottom: .5px solid var(--separator);
}
.nav-back { width: 34px; height: 34px; display: flex; align-items: center; justify-content: center; }
.nav-title-wrap { display: flex; align-items: center; gap: 7px; }
.nav-badge {
  width: 22px; height: 22px; border-radius: 7px;
  background: linear-gradient(135deg, #0071e3, #af52de);
  display: flex; align-items: center; justify-content: center;
}
.nav-title { font-size: 16px; font-weight: 700; }

.chat-body { flex: 1; padding: 16px 16px 0; box-sizing: border-box; min-height: 0; }
.msg-row { display: flex; gap: 8px; margin-bottom: 14px; }
.msg-row.user { justify-content: flex-end; }
.msg-avatar {
  width: 28px; height: 28px; border-radius: 9px;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
  margin-top: 2px;
}
.ai-avatar { background: linear-gradient(135deg, #0071e3, #af52de); box-shadow: 0 3px 9px rgba(90, 100, 230, .3); }
.bubble {
  max-width: 76%;
  padding: 10px 14px;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-all;
  white-space: pre-wrap;
}
.bubble.ai {
  background: rgba(255, 255, 255, .92);
  color: var(--text-1);
  border-radius: 4px 16px 16px 16px;
  box-shadow: var(--shadow-card);
}
.bubble.user {
  background: var(--accent);
  color: #ffffff;
  border-radius: 16px 4px 16px 16px;
  box-shadow: 0 4px 14px rgba(0, 113, 227, .25);
}
.bubble.typing { color: var(--text-2); }

.suggests { display: flex; flex-direction: column; gap: 9px; margin: 6px 0 0 36px; align-items: flex-start; }
.suggest-chip {
  font-size: 13px;
  color: var(--accent);
  background: var(--accent-soft);
  border-radius: 980px;
  padding: 7px 15px;
}
.suggest-chip:active { background: rgba(0, 113, 227, .18); }

.chat-input-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px calc(12px + env(safe-area-inset-bottom));
  background: rgba(255, 255, 255, .9);
  backdrop-filter: saturate(180%) blur(20px);
  -webkit-backdrop-filter: saturate(180%) blur(20px);
  border-top: .5px solid var(--separator);
}
.chat-input { flex: 1; border-radius: 980px; background: rgba(0, 0, 0, .05); }
.send-btn {
  width: 40px; height: 40px;
  border-radius: 50%;
  background: var(--accent);
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 4px 12px rgba(0, 113, 227, .35);
  flex-shrink: 0;
  transition: transform .15s ease-out;
}
.send-btn:active { transform: scale(.92); }
.send-btn.disabled { opacity: .4; }
</style>
