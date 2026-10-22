<template>
  <!-- 暗黑魔神风背景层：法相剪影 / 星盘 / 电光 / 骨刺符文 / 星尘 / 技能粒子爆发 -->
  <!-- 纯装饰：aria-hidden + pointer-events:none，不拦截任何交互 -->
  <div class="dbg" aria-hidden="true">
    <!-- 法相剪影：巨大魔影，压迫感 -->
    <svg class="dbg-avatar" viewBox="0 0 400 620" preserveAspectRatio="xMidYMid meet">
      <defs>
        <linearGradient id="dbgFade" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stop-color="#5c1226" stop-opacity="0.62" />
          <stop offset="45%" stop-color="#2a0714" stop-opacity="0.4" />
          <stop offset="100%" stop-color="#0d0207" stop-opacity="0" />
        </linearGradient>
        <radialGradient id="dbgEye" cx="50%" cy="50%" r="50%">
          <stop offset="0%" stop-color="#ff3b6b" stop-opacity="0.95" />
          <stop offset="100%" stop-color="#ff3b6b" stop-opacity="0" />
        </radialGradient>
      </defs>

      <!-- 斗篷主体 -->
      <path d="M200 148 L150 198 L108 306 L78 620 L322 620 L292 306 L250 198 Z" fill="url(#dbgFade)" />
      <!-- 肩甲骨刺 -->
      <path d="M150 198 L88 164 L124 236 Z" fill="url(#dbgFade)" />
      <path d="M250 198 L312 164 L276 236 Z" fill="url(#dbgFade)" />
      <!-- 头颅 -->
      <circle cx="200" cy="124" r="42" fill="url(#dbgFade)" />
      <!-- 双角 -->
      <path d="M166 96 L128 26 L180 72 Z" fill="url(#dbgFade)" />
      <path d="M234 96 L272 26 L220 72 Z" fill="url(#dbgFade)" />
      <!-- 双目（血光） -->
      <ellipse cx="184" cy="122" rx="9" ry="5" fill="url(#dbgEye)" class="dbg-eye" />
      <ellipse cx="216" cy="122" rx="9" ry="5" fill="url(#dbgEye)" class="dbg-eye" />
    </svg>

    <!-- 中心星盘（万法归宗） -->
    <svg class="dbg-astrolabe" viewBox="0 0 600 600" preserveAspectRatio="xMidYMid meet">
      <defs>
        <radialGradient id="dbgCore" cx="50%" cy="50%" r="50%">
          <stop offset="0%" stop-color="#ff3b6b" stop-opacity="0.42" />
          <stop offset="40%" stop-color="#6a1030" stop-opacity="0.22" />
          <stop offset="100%" stop-color="#2a0714" stop-opacity="0" />
        </radialGradient>
      </defs>
      <circle cx="300" cy="300" r="292" fill="url(#dbgCore)" />

      <g class="dbg-spin-slow">
        <circle class="ring" cx="300" cy="300" r="248" />
        <circle class="ring dash" cx="300" cy="300" r="226" />
        <polygon class="poly" points="480,300 390,456 210,456 120,300 210,144 390,144" />
      </g>
      <g class="dbg-spin-rev">
        <circle class="ring" cx="300" cy="300" r="186" />
        <polygon class="poly thin" points="420,300 360,404 240,404 180,300 240,196 360,196" />
      </g>
      <g>
        <line
          v-for="n in 24"
          :key="'sp' + n"
          class="spoke"
          x1="300" y1="52" x2="300" y2="80"
          :transform="`rotate(${(n - 1) * 15} 300 300)`"
        />
      </g>
      <circle class="ring inner" cx="300" cy="300" r="92" />
      <circle class="dbg-core" cx="300" cy="300" r="14" />
    </svg>

    <!-- 电光：左右各一道，间歇闪现 -->
    <svg class="dbg-arc a1" viewBox="0 0 300 600" preserveAspectRatio="none">
      <path class="bolt" vector-effect="non-scaling-stroke"
        d="M60 0 L96 96 L52 168 L104 250 L58 336 L112 424 L64 512 L110 600" />
    </svg>
    <svg class="dbg-arc a2" viewBox="0 0 300 600" preserveAspectRatio="none">
      <path class="bolt b2" vector-effect="non-scaling-stroke"
        d="M240 0 L206 104 L250 176 L196 258 L242 344 L188 432 L236 520 L192 600" />
    </svg>

    <!-- 骨刺符文：左右垂落 -->
    <div class="dbg-chain left">
      <span v-for="(r, i) in runes" :key="'l' + i" :style="{ animationDelay: i * 0.14 + 's' }">{{ r }}</span>
    </div>
    <div class="dbg-chain right">
      <span v-for="(r, i) in runes" :key="'r' + i" :style="{ animationDelay: i * 0.14 + 's' }">{{ r }}</span>
    </div>

    <!-- 技能粒子爆发：自中心向外迸射 -->
    <div class="dbg-burst">
      <i
        v-for="(b, i) in bursts"
        :key="'b' + i"
        :style="{ '--a': b.a + 'deg', animationDelay: b.d + 's', animationDuration: b.dur + 's' }"
      ></i>
    </div>

    <!-- 星尘：缓慢上浮 -->
    <div class="dbg-dust">
      <i
        v-for="(p, i) in dust"
        :key="'d' + i"
        :style="{
          left: p.x + '%', top: p.y + '%',
          width: p.s + 'px', height: p.s + 'px',
          opacity: p.o, animationDelay: p.d + 's', animationDuration: p.dur + 's'
        }"
      ></i>
    </div>
  </div>
</template>

<script setup>

// 骨刺符文（卢恩风格，配合暗黑魔神主题）
const runes = ['ᚦ', 'ᛉ', 'ᚨ', 'ᛝ', 'ᛟ', 'ᚱ', 'ᛞ', 'ᛒ', 'ᚦ', 'ᛉ']

// 星尘：一次性随机，避免每次渲染重排跳动
const dust = Array.from({ length: 44 }, () => ({
  x: (Math.random() * 100).toFixed(2),
  y: (Math.random() * 100).toFixed(2),
  s: (1 + Math.random() * 2.2).toFixed(1),
  o: (0.2 + Math.random() * 0.45).toFixed(2),
  d: (Math.random() * 12).toFixed(2),
  dur: (9 + Math.random() * 12).toFixed(1)
}))

// 技能粒子爆发：18 道，角度均分，延迟与时长错开
const bursts = Array.from({ length: 18 }, (_, i) => ({
  a: (i * 20).toFixed(0),
  d: (Math.random() * 3.4).toFixed(2),
  dur: (2.8 + Math.random() * 1.6).toFixed(1)
}))
</script>

<style scoped>
.dbg {
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  overflow: hidden;
}

/* ===== 法相剪影 ===== */
.dbg-avatar {
  position: absolute;
  left: 50%;
  top: 50%;
  width: 760px;
  height: 1178px;
  transform: translate(-50%, -46%);
  opacity: 0.5;
  animation: dbgBreathe 9s ease-in-out infinite;
}
@keyframes dbgBreathe {
  0%, 100% { transform: translate(-50%, -46%) scale(1); opacity: 0.42; }
  50%      { transform: translate(-50%, -46%) scale(1.045); opacity: 0.62; }
}
.dbg-eye { animation: dbgEye 3.4s ease-in-out infinite; }
@keyframes dbgEye {
  0%, 100% { opacity: 0.35; }
  50%      { opacity: 1; }
}

/* ===== 中心星盘 ===== */
.dbg-astrolabe {
  position: absolute;
  left: 50%;
  top: 50%;
  width: 640px;
  height: 640px;
  transform: translate(-50%, -50%);
  opacity: 0.55;
}
.ring { fill: none; stroke: rgba(255, 59, 107, 0.28); stroke-width: 1; }
.ring.dash { stroke-dasharray: 10 14; stroke: rgba(179, 24, 58, 0.4); }
.ring.inner { stroke: rgba(217, 176, 102, 0.22); }
.poly { fill: none; stroke: rgba(255, 59, 107, 0.22); stroke-width: 1; }
.poly.thin { stroke: rgba(122, 31, 61, 0.35); }
.spoke { stroke: rgba(255, 59, 107, 0.4); stroke-width: 1.5; }
.dbg-core {
  fill: #ff3b6b;
  filter: none;
  animation: dbgPulse 3s ease-in-out infinite;
}
@keyframes dbgPulse {
  0%, 100% { opacity: 0.35; }
  50%      { opacity: 0.95; }
}
.dbg-spin-slow { transform-origin: 300px 300px; animation: dbgSpin 72s linear infinite; }
.dbg-spin-rev  { transform-origin: 300px 300px; animation: dbgSpinRev 48s linear infinite; }
@keyframes dbgSpin { to { transform: rotate(360deg); } }
@keyframes dbgSpinRev { to { transform: rotate(-360deg); } }

/* ===== 电光 ===== */
.dbg-arc {
  position: absolute;
  top: 0;
  height: 100%;
  width: 42%;
  opacity: 0.6;
}
.dbg-arc.a1 { left: 0; }
.dbg-arc.a2 { right: 0; }
.bolt {
  fill: none;
  stroke: rgba(255, 59, 107, 0.5);
  stroke-width: 1.6;
  stroke-dasharray: 260 620;
  animation: dbgBolt 6s ease-in-out infinite;
}
.bolt.b2 {
  stroke: rgba(179, 24, 58, 0.55);
  animation-delay: 2.6s;
}
@keyframes dbgBolt {
  0%, 62%, 100% { stroke-dashoffset: 880; opacity: 0; }
  68%           { stroke-dashoffset: 0; opacity: 1; }
  76%           { stroke-dashoffset: -880; opacity: 0; }
}

/* ===== 骨刺符文 ===== */
.dbg-chain {
  position: absolute;
  top: 0;
  height: 100%;
  width: 62px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 26px;
}
.dbg-chain.left { left: 14px; }
.dbg-chain.right { right: 14px; }
.dbg-chain span {
  font-size: 17px;
  color: rgba(255, 59, 107, 0.5);
  text-shadow: 0 0 10px rgba(179, 24, 58, 0.7);
  animation: dbgRune 5.5s ease-in-out infinite;
}
@keyframes dbgRune {
  0%, 100% { transform: translateY(0); opacity: 0.22; }
  50%      { transform: translateY(-9px); opacity: 0.85; }
}

/* ===== 技能粒子爆发 ===== */
.dbg-burst {
  position: absolute;
  left: 50%;
  top: 50%;
  width: 0;
  height: 0;
}
.dbg-burst i {
  position: absolute;
  left: 0;
  top: 0;
  width: 4px;
  height: 4px;
  margin: -2px 0 0 -2px;
  border-radius: 50%;
  background: #ff3b6b;
  box-shadow: 0 0 8px rgba(255, 59, 107, 0.9);
  transform-origin: 0 0;
  opacity: 0;
  animation-name: dbgBurst;
  animation-timing-function: cubic-bezier(0.15, 0.7, 0.3, 1);
  animation-iteration-count: infinite;
}
@keyframes dbgBurst {
  0%   { transform: rotate(var(--a)) translateX(40px) scale(1.4); opacity: 0; }
  12%  { opacity: 0.95; }
  70%  { opacity: 0.35; }
  100% { transform: rotate(var(--a)) translateX(330px) scale(0.2); opacity: 0; }
}

/* ===== 星尘 ===== */
.dbg-dust { position: absolute; inset: 0; }
.dbg-dust i {
  position: absolute;
  border-radius: 50%;
  background: rgba(232, 220, 200, 0.75);
  animation-name: dbgRise;
  animation-timing-function: linear;
  animation-iteration-count: infinite;
}
@keyframes dbgRise {
  0%   { transform: translateY(0); opacity: 0; }
  20%  { opacity: 0.7; }
  100% { transform: translateY(-180px); opacity: 0; }
}

@media (max-width: 720px) {
  .dbg-avatar { width: 480px; height: 744px; }
  .dbg-astrolabe { width: 420px; height: 420px; }
  .dbg-chain { display: none; }
}
</style>
