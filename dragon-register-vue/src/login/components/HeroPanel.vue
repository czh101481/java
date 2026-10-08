<template>
  <!-- 主视觉锚点 -->
  <section class="hero">
    <div class="brand"><span class="dot"></span> Sakura Pass</div>
    <div class="hero-art">
      <AnimeCharacter />
    </div>
    <div>
      <h2>欢迎回到<br />你的二次元空间</h2>
      <p>登录后继续你的冒险 ✦</p>
    </div>
  </section>
</template>

<script setup>
import AnimeCharacter from './AnimeCharacter.vue'
</script>

<style scoped>
.hero{
  position:relative;padding:40px 32px;
  display:flex;flex-direction:column;justify-content:space-between;
  background:
    radial-gradient(80% 60% at 50% 30%, rgba(255,255,255,.6), transparent 70%),
    linear-gradient(160deg, rgba(126,200,227,.35), rgba(255,111,145,.30) 60%, rgba(168,230,207,.35));
  border-right:1px solid var(--glass-line);
}
.brand{display:flex;align-items:center;gap:10px;font-weight:700;font-size:18px;letter-spacing:.04em}
.brand .dot{
  width:26px;height:26px;border-radius:9px;
  background:linear-gradient(135deg,var(--accent),var(--accent-2));
  box-shadow:0 4px 12px rgba(255,111,145,.4);
}
.hero-art{display:grid;place-items:center;flex:1;padding:18px 0}
.hero h2{font-size:26px;font-weight:700;line-height:1.3;letter-spacing:.01em}
.hero p{margin-top:8px;color:var(--ink-2);font-size:14px}

@media(max-width:760px){
  .hero{display:none}
}
</style>
