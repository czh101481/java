<template>
  <!-- 角色立绘（赛璐璐风 SVG 锚点，自 login-anime.html 原样迁移） -->
  <svg
    width="220"
    height="250"
    viewBox="0 0 220 250"
    fill="none"
    xmlns="http://www.w3.org/2000/svg"
    aria-label="anime character"
  >
    <defs>
      <linearGradient id="hair" x1="0" y1="0" x2="1" y2="1">
        <stop offset="0" stop-color="#FFB3C8" /><stop offset="1" stop-color="#7EC8E3" />
      </linearGradient>
      <linearGradient id="cloth" x1="0" y1="0" x2="0" y2="1">
        <stop offset="0" stop-color="#FFFFFF" /><stop offset="1" stop-color="#E9F6FB" />
      </linearGradient>
      <radialGradient id="eye" cx="0.5" cy="0.4" r="0.6">
        <stop offset="0" stop-color="#9BE0F2" /><stop offset="1" stop-color="#3F7FB0" />
      </radialGradient>
    </defs>
    <!-- 后发 -->
    <path d="M60 95 C50 40 170 40 160 95 C175 130 168 175 150 200 C140 160 80 160 70 200 C52 175 45 130 60 95Z" fill="url(#hair)" />
    <!-- 身体/衣 -->
    <path d="M78 175 C78 160 142 160 142 175 L150 250 L70 250 Z" fill="url(#cloth)" />
    <path d="M108 168 L120 188 L96 188 Z" fill="#FFD6E2" />
    <!-- 脸 -->
    <ellipse cx="110" cy="115" rx="40" ry="44" fill="#FFE6D5" />
    <!-- 前发/刘海 -->
    <path d="M70 100 C72 60 148 60 150 100 C140 82 128 92 120 84 C114 96 106 96 100 84 C92 92 80 82 70 100Z" fill="url(#hair)" />
    <path d="M110 72 C104 88 100 96 96 104 C104 98 110 98 110 98 C110 98 116 98 124 104 C120 96 116 88 110 72Z" fill="url(#hair)" />
    <!-- 侧发尾 -->
    <path d="M70 110 C58 140 60 175 78 178 C72 150 74 130 84 116 Z" fill="url(#hair)" />
    <path d="M150 110 C162 140 160 175 142 178 C148 150 146 130 136 116 Z" fill="url(#hair)" />
    <!-- 眼睛 -->
    <ellipse cx="94" cy="118" rx="9" ry="12" fill="url(#eye)" />
    <ellipse cx="126" cy="118" rx="9" ry="12" fill="url(#eye)" />
    <circle cx="91" cy="113" r="3" fill="#fff" /><circle cx="123" cy="113" r="3" fill="#fff" />
    <path d="M86 124 q8 4 16 0" stroke="#3F7FB0" stroke-width="1.6" fill="none" stroke-linecap="round" />
    <path d="M118 124 q8 4 16 0" stroke="#3F7FB0" stroke-width="1.6" fill="none" stroke-linecap="round" />
    <!-- 腮红 -->
    <ellipse cx="84" cy="130" rx="6" ry="3.5" fill="#FFB3C8" opacity=".7" />
    <ellipse cx="136" cy="130" rx="6" ry="3.5" fill="#FFB3C8" opacity=".7" />
    <!-- 嘴 -->
    <path d="M104 134 q6 5 12 0" stroke="#E58aa0" stroke-width="2" fill="none" stroke-linecap="round" />
  </svg>
</template>
