<template>
  <!-- 灵气粒子画布 -->
  <canvas ref="qi" id="qi"></canvas>

  <div class="fog"></div>

  <!-- 背景魔纹法阵 -->
  <div class="sigil">
    <svg viewBox="0 0 200 200" fill="none" stroke="#b066ff" stroke-width="0.6">
      <circle cx="100" cy="100" r="95" />
      <circle cx="100" cy="100" r="78" />
      <circle cx="100" cy="100" r="40" />
      <polygon points="100,5 162,135 38,135" />
      <polygon points="100,195 38,65 162,65" />
      <g stroke="#ff3b6b">
        <line x1="100" y1="5" x2="100" y2="195" />
        <line x1="5" y1="100" x2="195" y2="100" />
        <line x1="33" y1="33" x2="167" y2="167" />
        <line x1="167" y1="33" x2="33" y2="167" />
      </g>
    </svg>
  </div>

  <!-- 路由视图：登录 / 注册 / 魔域中枢 -->
  <router-view />

  <div class="whisper">「 顺 逆 由 心 ， 魔 非 魔 ， 道 非 道 」</div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'

const qi = ref(null)
let raf = null
let particles = []
let w = 0, h = 0
let ctx = null
const colors = ['#ff3b6b', '#b066ff', '#d9b066', '#6a2c9c']

function resize() {
  const canvas = qi.value
  if (!canvas) return
  w = canvas.width = window.innerWidth
  h = canvas.height = window.innerHeight
}

function makeParticles() {
  particles = []
  const count = Math.min(140, Math.floor(w * h / 12000))
  for (let i = 0; i < count; i++) {
    particles.push({
      x: Math.random() * w,
      y: Math.random() * h,
      r: Math.random() * 2 + 0.4,
      vy: -(Math.random() * 0.5 + 0.15),
      vx: (Math.random() - 0.5) * 0.3,
      a: Math.random() * 0.6 + 0.2,
      c: colors[Math.floor(Math.random() * colors.length)]
    })
  }
}

function draw() {
  if (!ctx) return
  ctx.clearRect(0, 0, w, h)
  for (const p of particles) {
    p.y += p.vy
    p.x += p.vx
    if (p.y < -5) { p.y = h + 5; p.x = Math.random() * w }
    if (p.x < -5) p.x = w + 5
    if (p.x > w + 5) p.x = -5
    ctx.beginPath()
    ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2)
    ctx.fillStyle = p.c
    ctx.globalAlpha = p.a
    ctx.shadowBlur = 8
    ctx.shadowColor = p.c
    ctx.fill()
  }
  ctx.globalAlpha = 1
  ctx.shadowBlur = 0
  raf = requestAnimationFrame(draw)
}

onMounted(() => {
  const canvas = qi.value
  ctx = canvas.getContext('2d')
  resize()
  makeParticles()
  draw()
  window.addEventListener('resize', resize)
  window.addEventListener('resize', makeParticles)
})

onBeforeUnmount(() => {
  if (raf) cancelAnimationFrame(raf)
  window.removeEventListener('resize', resize)
  window.removeEventListener('resize', makeParticles)
})
</script>
