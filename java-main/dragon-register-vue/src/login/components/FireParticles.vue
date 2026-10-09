<template>
  <canvas id="fx" ref="canvasEl"></canvas>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'

/**
 * 龙焰火星粒子：上升 + 辉光 + 轻微正弦横摆
 * 与原页面 #fx canvas 动画逻辑一致（46 颗粒子 / lighter 混合 / shadowBlur 12）
 */
const canvasEl = ref(null)

let ctx = null
let W = 0
let H = 0
let parts = []
let rafId = 0

function resize() {
  const cv = canvasEl.value
  if (!cv) return
  W = cv.width = window.innerWidth
  H = cv.height = window.innerHeight
  parts = Array.from({ length: 46 }, () => ({
    x: Math.random() * W,
    y: Math.random() * H,
    r: Math.random() * 2 + 0.8,
    vy: -(Math.random() * 0.6 + 0.25),
    vx: (Math.random() - 0.5) * 0.3,
    a: Math.random() * 0.5 + 0.3,
    hue: Math.random() < 0.55 ? '232,184,75' : Math.random() < 0.5 ? '224,83,61' : '91,200,245'
  }))
}

function tick() {
  if (!ctx) return
  ctx.clearRect(0, 0, W, H)
  ctx.globalCompositeOperation = 'lighter'
  for (const p of parts) {
    p.y += p.vy
    p.x += p.vx + Math.sin(p.y / 30) * 0.2
    if (p.y < -10) {
      p.y = H + 10
      p.x = Math.random() * W
    }
    ctx.beginPath()
    ctx.arc(p.x, p.y, p.r, 0, 7)
    ctx.shadowBlur = 12
    ctx.shadowColor = `rgba(${p.hue},${p.a})`
    ctx.fillStyle = `rgba(${p.hue},${p.a})`
    ctx.fill()
  }
  ctx.shadowBlur = 0
  ctx.globalCompositeOperation = 'source-over'
  rafId = requestAnimationFrame(tick)
}

onMounted(() => {
  const cv = canvasEl.value
  if (!cv) return
  ctx = cv.getContext('2d')
  window.addEventListener('resize', resize)
  resize()
  tick()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  if (rafId) cancelAnimationFrame(rafId)
  ctx = null
})
</script>
