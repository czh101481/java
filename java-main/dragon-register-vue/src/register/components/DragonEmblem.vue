<template>
  <!-- 主视觉：龙渊徽记（SVG 原样迁移） -->
  <svg
    width="240"
    height="240"
    viewBox="0 0 240 240"
    fill="none"
    xmlns="http://www.w3.org/2000/svg"
    aria-label="dragon emblem"
  >
    <defs>
      <radialGradient id="med" cx="0.5" cy="0.42" r="0.6">
        <stop offset="0" stop-color="#243056" /><stop offset="1" stop-color="#0A0E1C" />
      </radialGradient>
      <linearGradient id="gold" x1="0" y1="0" x2="1" y2="1">
        <stop offset="0" stop-color="#F6D98A" /><stop offset="1" stop-color="#C8941F" />
      </linearGradient>
      <linearGradient id="dragon" x1="0" y1="0" x2="1" y2="1">
        <stop offset="0" stop-color="#2C3656" /><stop offset="1" stop-color="#101626" />
      </linearGradient>
      <radialGradient id="eye" cx="0.5" cy="0.4" r="0.7">
        <stop offset="0" stop-color="#BFF3FF" /><stop offset="0.5" stop-color="#5BC8F5" /><stop offset="1" stop-color="#1C6FA0" />
      </radialGradient>
    </defs>

    <circle cx="120" cy="120" r="98" fill="url(#med)" stroke="rgba(232,184,75,.35)" stroke-width="2" />
    <circle cx="120" cy="120" r="98" fill="none" stroke="rgba(91,200,245,.12)" stroke-width="6" stroke-dasharray="2 10" />

    <path
      d="M120 40 C 168 46, 196 84, 192 128 C 189 162, 160 188, 120 188 C 80 188, 51 162, 48 128 C 45 92, 70 58, 110 46"
      fill="none" stroke="url(#gold)" stroke-width="9" stroke-linecap="round" opacity=".9"
    />
    <path
      d="M120 40 C 168 46, 196 84, 192 128 C 189 162, 160 188, 120 188 C 80 188, 51 162, 48 128 C 45 92, 70 58, 110 46"
      fill="none" stroke="rgba(255,240,200,.25)" stroke-width="2"
    />

    <g transform="translate(96,96)">
      <path
        d="M58 96 C 50 70, 70 52, 92 56 C 104 58, 110 64, 118 66
           C 140 70, 165 86, 182 104 C 178 110, 168 110, 160 108
           L 150 112 C 156 116, 158 122, 152 126 L 138 122
           C 132 128, 128 132, 120 132 C 96 136, 78 128, 70 116
           C 60 110, 56 104, 58 96 Z"
        fill="url(#dragon)" stroke="url(#gold)" stroke-width="2.5"
      />
      <path d="M70 60 C 55 40, 40 34, 30 38 C 42 44, 50 56, 64 66 Z" fill="url(#dragon)" stroke="url(#gold)" stroke-width="2" />
      <path d="M92 56 C 88 36, 94 26, 104 24 C 100 36, 98 50, 98 60 Z" fill="url(#dragon)" stroke="url(#gold)" stroke-width="2" />
      <ellipse cx="108" cy="90" rx="8" ry="10" fill="url(#eye)" />
      <ellipse cx="108" cy="90" rx="2.4" ry="9" fill="#0A1424" />
      <path d="M150 112 l4 9 l4 -8 Z" fill="#F3E6C4" />
      <path d="M138 122 l3 8 l4 -7 Z" fill="#F3E6C4" />
    </g>

    <circle cx="120" cy="120" r="13" fill="url(#eye)" opacity=".9" />
    <circle cx="120" cy="120" r="13" fill="none" stroke="rgba(232,184,75,.6)" stroke-width="1.5" />
  </svg>
</template>
