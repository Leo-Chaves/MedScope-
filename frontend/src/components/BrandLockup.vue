<script setup>
defineProps({
  compact: {
    type: Boolean,
    default: false
  },
  showSymbol: {
    type: Boolean,
    default: true
  },
  tone: {
    type: String,
    default: 'default'
  }
})
</script>

<template>
  <div class="brand-lockup" :class="[`brand-lockup--${tone}`, { 'brand-lockup--compact': compact }]">
    <div v-if="showSymbol" class="brand-lockup__symbol" aria-hidden="true">
      <img src="/images/logo.png" alt="" class="brand-lockup__logo" />
    </div>

    <div class="brand-lockup__text">
      <strong>MedScope</strong>
      <span>Evidências clínicas com leitura profissional</span>
    </div>
  </div>
</template>
