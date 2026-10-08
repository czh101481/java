<template>
  <canvas id="fx" ref="canvasEl"></canvas>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'

/**
 * 樱花花瓣 + 光斑粒子：向下飘落 + 正弦横摆
 * 与原页面 #fx canvas 动画逻辑一致（34 颗 / 无辉光 / 40% 为花瓣形）
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
  parts = Array.from({ length: 34 }, () => ({
    x: Math.random() * W,
    y: Math.random() * H,
    r: Math.random() * 3 + 1.5,
    vy: Math.random() * 0.5 + 0.25,
    vx: (Math.random() - 0.5) * 0.4,
    a: Math.random() * 0.5 + 0.2,
    hue: Math.random() < 0.5 ? '255,111,145' : '126,200,227',
    petal: Math.random() < 0.4
  }))
}

function tick() {
  if (!ctx) return
  ctx.clearRect(0, 0, W, H)
  for (const p of parts) {
    p.y += p.vy
    p.x += p.vx + Math.sin(p.y / 40) * 0.3
    if (p.y > H + 10) {
      p.y = -10
      p.x = Math.random() * W
    }
    ctx.beginPath()
    if (p.petal) {
      // 花瓣形
      ctx.ellipse(p.x, p.y, p.r, p.r * 0.5, p.y / 30, 0, 7)
    } else {
      ctx.arc(p.x, p.y, p.r, 0, 7)
    }
    ctx.fillStyle = `rgba(${p.hue},${p.a})`
    ctx.fill()
  }
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
