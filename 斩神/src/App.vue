<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import StarField from './components/StarField.vue'
import LoginCard from './components/LoginCard.vue'
import hero from './assets/hero.jpg'

const heroEl = ref(null)

function onMove(e) {
  const dx = e.clientX / window.innerWidth - 0.5
  const dy = e.clientY / window.innerHeight - 0.5
  if (heroEl.value) heroEl.value.style.transform = `translate(${dx * 18}px, ${dy * 18}px)`
  const s = document.createElement('div')
  s.className = 'cursor-star'
  s.style.left = (e.clientX - 3) + 'px'
  s.style.top = (e.clientY - 3) + 'px'
  document.body.appendChild(s)
  setTimeout(() => s.remove(), 700)
}

onMounted(() => window.addEventListener('mousemove', onMove))
onUnmounted(() => window.removeEventListener('mousemove', onMove))
</script>

<template>
  <div class="aurora" aria-hidden="true"></div>
  <StarField />
  <div class="stage">
    <div class="hero-wrap" ref="heroEl">
      <div class="hero">
        <img class="hero-img" :src="hero" alt="林七夜 星辰刀" />
        <div class="blade" aria-hidden="true"></div>
      </div>
    </div>
    <LoginCard />
  </div>
  <div class="foot">© 黄昏精神病院 · 诸神黄昏观测站 · 同人创作 Demo</div>
</template>
