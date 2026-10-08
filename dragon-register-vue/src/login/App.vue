<template>
  <!-- 轻量粒子背景：樱花花瓣 + 光斑 -->
  <PetalParticles />

  <!-- 第一级：主视觉锚点 + 登录卡片 -->
  <main class="stage">
    <HeroPanel />
    <LoginPanel />
  </main>
</template>

<script setup>
import PetalParticles from './components/PetalParticles.vue'
import HeroPanel from './components/HeroPanel.vue'
import LoginPanel from './components/LoginPanel.vue'
</script>

<style scoped>
.stage{
  position:relative;z-index:1;
  width:100%;max-width:920px;
  display:grid;grid-template-columns:0.92fr 1.08fr;
  background:var(--glass);
  border:1px solid var(--glass-line);
  border-radius:var(--r);
  box-shadow:0 20px 60px rgba(120,90,130,.18), inset 0 1px 0 rgba(255,255,255,.6);
  backdrop-filter:blur(18px) saturate(1.2);
  -webkit-backdrop-filter:blur(18px) saturate(1.2);
  overflow:hidden;
}

@media(max-width:760px){
  .stage{grid-template-columns:1fr}
}
</style>
