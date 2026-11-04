<script setup>
/**
 * 头像大图浮层：双击头像时打开，点击空白处或按 Esc 收起。
 * avatar 传原始值即可（预设 key / /uploads 路径 / 空），内部统一走 renderAvatar。
 */
import { computed, watch, onBeforeUnmount } from 'vue'
import { renderAvatar } from '../config/avatars'

const props = defineProps({
  open: { type: Boolean, default: false },
  avatar: { type: String, default: null },
  name: { type: String, default: '' }
})
const emit = defineEmits(['close'])

const view = computed(() => renderAvatar(props.avatar, props.name))

function onKey(e) {
  if (e.key === 'Escape') emit('close')
}

watch(
  () => props.open,
  (v) => {
    if (v) window.addEventListener('keydown', onKey)
    else window.removeEventListener('keydown', onKey)
  }
)
onBeforeUnmount(() => window.removeEventListener('keydown', onKey))
</script>

<template>
  <transition name="avfade">
    <div class="avatar-viewer" v-if="open" @click.self="emit('close')">
      <div class="av-frame" :style="view.style">
        <img v-if="view.img" :src="view.img" :alt="name" />
        <template v-else>{{ view.char }}</template>
      </div>
      <div class="av-name" v-if="name">{{ name }}</div>
      <div class="av-tip">点击空白处或按 Esc 收起</div>
    </div>
  </transition>
</template>

<style scoped>
.avatar-viewer {
  position: fixed;
  inset: 0;
  z-index: 400;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 18px;
  cursor: zoom-out;
  background: radial-gradient(circle at 50% 45%, rgba(46, 7, 22, 0.84), rgba(3, 2, 6, 0.95));
  backdrop-filter: blur(6px);
}
.av-frame {
  width: min(46vw, 300px);
  height: min(46vw, 300px);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: min(23vw, 146px);
  line-height: 1;
  color: #fff;
  overflow: hidden;
  background: radial-gradient(circle at 35% 30%, var(--violet-glow), #1a030a 80%);
  border: 2px solid rgba(217, 176, 102, 0.65);
  box-shadow: 0 0 42px rgba(255, 59, 107, 0.5), 0 0 96px rgba(176, 102, 255, 0.32);
  text-shadow: 0 0 26px rgba(255, 255, 255, 0.55);
  animation: avPop 0.32s cubic-bezier(0.2, 1.4, 0.4, 1);
}
.av-frame img { width: 100%; height: 100%; object-fit: cover; display: block; }
.av-name {
  font-size: 18px; letter-spacing: 6px; color: var(--bone);
  text-shadow: 0 0 14px var(--crimson-glow);
}
.av-tip { font-size: 11px; letter-spacing: 2px; color: rgba(232, 220, 200, 0.45); }
@keyframes avPop {
  from { transform: scale(0.82); opacity: 0; }
  to { transform: scale(1); opacity: 1; }
}
.avfade-enter-active, .avfade-leave-active { transition: opacity 0.2s ease; }
.avfade-enter-from, .avfade-leave-to { opacity: 0; }
</style>
