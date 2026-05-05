<script setup>
defineProps({
  compact: {
    type: Boolean,
    default: false
  },
  tone: {
    type: String,
    default: 'default'
  }
})
</script>

<template>
  <div class="brand-lockup" :class="[`brand-lockup--${tone}`, { 'brand-lockup--compact': compact }]">
    <div class="brand-lockup__symbol" aria-hidden="true">
      <svg viewBox="0 0 64 64" role="img" focusable="false">
        <defs>
          <linearGradient id="brand-gradient" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stop-color="#2f7cf6" />
            <stop offset="100%" stop-color="#0d3e8a" />
          </linearGradient>
        </defs>
        <rect x="6" y="6" width="52" height="52" rx="18" fill="url(#brand-gradient)" />
        <path
          d="M22 32h20M32 22v20"
          stroke="#ffffff"
          stroke-width="4.2"
          stroke-linecap="round"
        />
        <circle cx="32" cy="32" r="17" fill="none" stroke="rgba(255,255,255,0.3)" stroke-width="2.4" />
        <path
          d="M43.8 43.8L51 51"
          stroke="#dce8ff"
          stroke-width="4.2"
          stroke-linecap="round"
        />
      </svg>
    </div>

    <div class="brand-lockup__text">
      <strong>MedScope</strong>
      <span>Evidências clínicas com leitura profissional</span>
    </div>
  </div>
</template>
