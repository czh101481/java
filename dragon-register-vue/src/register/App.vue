<template>
  <!-- 龙焰粒子背景 -->
  <FireParticles />

  <!-- 主舞台：左主视觉 + 右注册卡片 -->
  <main class="stage">
    <HeroPanel />
    <RegisterPanel login-url="login-anime.html" />
  </main>
</template>

<script setup>
import FireParticles from './components/FireParticles.vue'
import HeroPanel from './components/HeroPanel.vue'
import RegisterPanel from './components/RegisterPanel.vue'
</script>

<style scoped>
.stage{
  position:relative;z-index:1;
  width:100%;max-width:920px;
  display:grid;grid-template-columns:0.92fr 1.08fr;
  background:var(--glass);
  border:1px solid var(--glass-line);
  border-radius:var(--r);
  box-shadow:0 24px 70px rgba(0,0,0,.55), inset 0 1px 0 rgba(255,255,255,.06), 0 0 0 1px rgba(232,184,75,.06);
  backdrop-filter:blur(20px) saturate(1.15);
  -webkit-backdrop-filter:blur(20px) saturate(1.15);
  overflow:hidden;
}

@media(max-width:760px){
  .stage{grid-template-columns:1fr}
}
</style>
