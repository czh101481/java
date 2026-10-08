<template>
  <!-- 主视觉：龙渊 -->
  <section class="hero">
    <div class="brand"><span class="dot"></span> 龙渊 · Draconic</div>
    <div class="hero-art">
      <DragonEmblem />
    </div>
    <div>
      <h2>缔结契约<br />觉醒你的龙血</h2>
      <p>数步之间，成就专属名号 ✦</p>
    </div>
  </section>
</template>

<script setup>
import DragonEmblem from './DragonEmblem.vue'
</script>

<style scoped>
.hero{
  position:relative;padding:40px 32px;
  display:flex;flex-direction:column;justify-content:space-between;
  background:
    radial-gradient(80% 60% at 50% 30%, rgba(232,184,75,.10), transparent 70%),
    linear-gradient(160deg, rgba(91,200,245,.16), rgba(224,83,61,.18) 60%, rgba(18,24,42,.55));
  border-right:1px solid var(--glass-line);
}
.brand{
  display:flex;align-items:center;gap:10px;
  font-family:"Cinzel","Noto Serif SC",serif;font-weight:700;font-size:18px;
  letter-spacing:.08em;color:#F3E6C4;
}
.brand .dot{
  width:26px;height:26px;border-radius:8px;
  background:linear-gradient(135deg,var(--accent),var(--accent-3));
  box-shadow:0 0 14px rgba(232,184,75,.55);
}
.hero-art{display:grid;place-items:center;flex:1;padding:18px 0}
.hero h2{font-family:"Noto Serif SC",serif;font-size:27px;font-weight:700;line-height:1.3;letter-spacing:.02em;color:#F4EEDC}
.hero p{margin-top:8px;color:var(--ink-2);font-size:14px}

@media(max-width:760px){
  .hero{display:none}
}
</style>
