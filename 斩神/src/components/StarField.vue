<script setup>
import { ref, onMounted, onUnmounted } from 'vue'

const cv = ref(null)
let raf = 0
let ctx
let stars = []
let W = 0
let H = 0
const reduce = window.matchMedia('(prefers-reduced-motion: reduce)').matches

function resize() {
  const c = cv.value
  if (!c) return
  const dpr = Math.min(window.devicePixelRatio || 1, 2)
  W = c.clientWidth
  H = c.clientHeight
  c.width = W * dpr
  c.height = H * dpr
  ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
  const n = Math.min(170, Math.max(60, Math.floor((W * H) / 9000)))
  stars = Array.from({ length: n }, () => ({
    x: Math.random() * W,
    y: Math.random() * H,
    r: Math.random() * 1.5 + 0.3,
    a: Math.random(),
    s: Math.random() * 0.02 + 0.004,
    vy: Math.random() * 0.2 + 0.04,
    c: Math.random() < 0.15 ? '#ffd56b' : (Math.random() < 0.5 ? '#5fe9ff' : '#cfe3ff')
  }))
}

function frame() {
  ctx.clearRect(0, 0, W, H)
  for (const st of stars) {
    st.a += st.s
    if (st.a > 1 || st.a < 0) st.s *= -1
    st.y += st.vy
    if (st.y > H) { st.y = 0; st.x = Math.random() * W }
    ctx.globalAlpha = Math.abs(st.a)
    ctx.beginPath()
    ctx.arc(st.x, st.y, st.r, 0, 6.283)
    ctx.fillStyle = st.c
    ctx.shadowBlur = 6
    ctx.shadowColor = st.c
    ctx.fill()
  }
  ctx.globalAlpha = 1
  ctx.shadowBlur = 0
  raf = requestAnimationFrame(frame)
}

onMounted(() => {
  ctx = cv.value.getContext('2d')
  resize()
  window.addEventListener('resize', resize)
  if (reduce) { frame(); cancelAnimationFrame(raf) } else { frame() }
})
onUnmounted(() => {
  cancelAnimationFrame(raf)
  window.removeEventListener('resize', resize)
})
</script>

<template>
  <canvas ref="cv" class="stars" aria-hidden="true"></canvas>
</template>
