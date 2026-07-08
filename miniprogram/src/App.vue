<script>
export default {
  onLaunch() {},
  onShow() {},
  onHide() {}
}
</script>

<style>
/* ============================================================
   Apple HIG 设计令牌 — 移植自 Web 端 frontend/src/styles/main.css
   小程序无 :root，令牌统一挂在 page 选择器上
   ============================================================ */
page {
  --font-stack: -apple-system, BlinkMacSystemFont, "SF Pro Text", "PingFang SC", "Helvetica Neue", sans-serif;
  --accent: #0071e3;            /* Apple 蓝 */
  --accent-hover: #0077ed;
  --accent-soft: rgba(0, 113, 227, .1);
  --bg: #f5f5f7;                /* Apple 浅灰背景 */
  --surface: rgba(255, 255, 255, .86);  /* 毛玻璃卡片（小程序降级：高透明白 + 阴影） */
  --surface-solid: #ffffff;
  --text-1: #1d1d1f;
  --text-2: #6e6e73;
  --separator: rgba(0, 0, 0, .08);
  --radius-lg: 18px;
  --radius-md: 12px;
  --radius-pill: 980px;
  --shadow-card: 0 4px 24px rgba(0, 0, 0, .06);
  --shadow-pop: 0 12px 48px rgba(0, 0, 0, .18);

  --green: #34c759;
  --green-soft: rgba(52, 199, 89, .14);
  --orange: #ff9500;
  --orange-soft: rgba(255, 149, 0, .14);
  --red: #ff3b30;
  --red-soft: rgba(255, 59, 48, .12);
  --purple: #af52de;
  --purple-soft: rgba(175, 82, 222, .12);
  --teal: #30b0c7;
  --teal-soft: rgba(48, 176, 199, .14);
  --gray-soft: rgba(110, 110, 115, .12);
  --fill-input: rgba(0, 0, 0, .04);

  font-family: var(--font-stack);
  background: var(--bg);
  color: var(--text-1);
  font-size: 15px;
  line-height: 1.5;
}

/* ---------- Typography ---------- */
.large-title {
  font-size: 32px;
  font-weight: 700;
  letter-spacing: -0.4px;
  color: var(--text-1);
  line-height: 1.25;
}
.page-subtitle { color: var(--text-2); font-size: 14px; margin-top: 4px; }

/* ---------- 页面骨架 ---------- */
.page-wrap {
  padding: calc(var(--status-bar-height) + 14px) 18px 110px;
  min-height: 100vh;
  box-sizing: border-box;
}
.subnav {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 8px;
  margin-left: -6px;
  color: var(--accent);
  font-size: 15px;
  font-weight: 500;
  padding: 6px 6px;
}

/* ---------- 按钮（自绘，胶囊） ---------- */
.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  font-size: 15px;
  font-weight: 500;
  padding: 9px 20px;
  border-radius: var(--radius-pill);
  transition: all .18s ease-out;
  white-space: nowrap;
}
.btn.disabled { opacity: .45; }
.btn-primary { background: var(--accent); color: #ffffff; }
.btn-primary:active { transform: scale(.97); background: var(--accent-hover); }
.btn-secondary { background: rgba(0, 0, 0, .05); color: var(--text-1); }
.btn-secondary:active { background: rgba(0, 0, 0, .08); }
.btn-text { background: transparent; color: var(--accent); padding: 9px 12px; }
.btn-danger { background: var(--red); color: #ffffff; }
.btn-danger-text { background: transparent; color: var(--red); }
.btn-green { background: var(--green); color: #ffffff; }
.btn-sm { font-size: 13px; padding: 6px 14px; }
.btn-lg { font-size: 16px; padding: 13px 28px; }
.btn-block { width: 100%; }

/* ---------- 输入 ---------- */
.field { display: flex; flex-direction: column; gap: 6px; }
.field-label { font-size: 13px; font-weight: 600; color: var(--text-2); }
.input, .textarea {
  font-family: var(--font-stack);
  font-size: 15px;
  color: var(--text-1);
  background: var(--fill-input);
  border: 1.5px solid transparent;
  border-radius: var(--radius-md);
  padding: 10px 14px;
  box-sizing: border-box;
  width: 100%;
  min-height: 44px;
}
.input-focus {
  border-color: var(--accent);
  background: var(--surface-solid);
  box-shadow: 0 0 0 4px var(--accent-soft);
}
.textarea { min-height: 88px; height: 96px; }
.ph { color: #a1a1a6; }

/* picker 触发行 */
.picker-box {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--fill-input);
  border-radius: var(--radius-md);
  padding: 10px 14px;
  min-height: 44px;
  box-sizing: border-box;
  font-size: 15px;
}
.picker-box .val { color: var(--text-1); font-weight: 500; }
.picker-box .val.empty { color: #a1a1a6; font-weight: 400; }

/* ---------- 分段控件 ---------- */
.segmented {
  display: inline-flex;
  background: rgba(0, 0, 0, .055);
  border-radius: 10px;
  padding: 2.5px;
}
.segmented .seg-item {
  font-size: 13.5px;
  font-weight: 500;
  color: var(--text-1);
  border-radius: 8px;
  padding: 6px 14px;
  white-space: nowrap;
  transition: all .18s ease-out;
}
.segmented .seg-item.active {
  background: var(--surface-solid);
  box-shadow: 0 2px 8px rgba(0, 0, 0, .12);
  font-weight: 600;
}

/* ---------- 状态徽章 ---------- */
.pill {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  font-weight: 600;
  padding: 3px 11px;
  border-radius: var(--radius-pill);
  white-space: nowrap;
}
.pill-orange { background: var(--orange-soft); color: #c76b00; }
.pill-green { background: var(--green-soft); color: #1d8a3e; }
.pill-red { background: var(--red-soft); color: #d70015; }
.pill-gray { background: var(--gray-soft); color: var(--text-2); }
.pill-blue { background: var(--accent-soft); color: var(--accent); }
.pill-purple { background: var(--purple-soft); color: #8c30c9; }
.pill-teal { background: var(--teal-soft); color: #1f7f92; }

/* ---------- 卡片 ---------- */
.card {
  background: var(--surface);
  backdrop-filter: saturate(180%) blur(20px);
  -webkit-backdrop-filter: saturate(180%) blur(20px);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
  border: .5px solid rgba(255, 255, 255, .6);
}
.card-pad { padding: 20px; }

/* ---------- 分组列表 ---------- */
.group-row {
  display: flex;
  align-items: center;
  padding: 14px 18px;
  position: relative;
}
.group-row + .group-row::before {
  content: '';
  position: absolute;
  left: 18px; right: 0; top: 0;
  height: .5px;
  background: var(--separator);
}

/* ---------- 空态 / 加载 ---------- */
.empty-state {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  gap: 10px; padding: 52px 0; color: var(--text-2);
}
.empty-state .icon-wrap {
  width: 56px; height: 56px; border-radius: 50%;
  background: rgba(0, 0, 0, .045);
  display: flex; align-items: center; justify-content: center;
}
.loading-block {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  gap: 12px; padding: 52px 0; color: var(--text-2); font-size: 13.5px;
}
@keyframes mp-spin { to { transform: rotate(360deg); } }
.spinner {
  width: 22px; height: 22px;
  border: 2.5px solid rgba(0, 0, 0, .1);
  border-top-color: var(--accent);
  border-radius: 50%;
  animation: mp-spin .7s linear infinite;
}
.spinner.white { border-color: rgba(255,255,255,.35); border-top-color: #ffffff; width: 15px; height: 15px; border-width: 2px; }

/* ---------- 弹层（底部动作面板，iOS 风） ---------- */
.sheet-mask {
  position: fixed;
  left: 0; top: 0; right: 0; bottom: 0;
  background: rgba(0, 0, 0, .32);
  z-index: 1000;
  animation: mask-in .2s ease-out;
}
@keyframes mask-in { from { opacity: 0; } to { opacity: 1; } }
.sheet {
  position: fixed;
  left: 0; right: 0; bottom: 0;
  background: rgba(255, 255, 255, .96);
  backdrop-filter: saturate(180%) blur(20px);
  -webkit-backdrop-filter: saturate(180%) blur(20px);
  border-radius: 20px 20px 0 0;
  box-shadow: var(--shadow-pop);
  z-index: 1001;
  padding: 18px 20px calc(20px + env(safe-area-inset-bottom));
  animation: sheet-in .25s cubic-bezier(.32, .72, .35, 1.05);
  max-height: 82vh;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
}
@keyframes sheet-in { from { transform: translateY(40%); opacity: .4; } to { transform: translateY(0); opacity: 1; } }
.sheet-grabber {
  width: 36px; height: 5px; border-radius: 3px;
  background: rgba(0, 0, 0, .15);
  margin: 0 auto 12px;
}
.sheet-title { font-size: 17px; font-weight: 700; text-align: center; margin-bottom: 14px; }
.sheet-body { flex: 1; overflow-y: auto; }
.sheet-footer { display: flex; gap: 10px; margin-top: 16px; }
.sheet-footer .btn { flex: 1; padding-top: 12px; padding-bottom: 12px; }
</style>
