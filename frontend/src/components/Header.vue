<script setup>
defineProps({
  professionalName: {
    type: String,
    default: ''
  }
})

defineEmits(['logout'])
</script>

<template>
  <header class="topbar">
    <div class="topbar__content">
      <div>
        <a href="/home" class="brand">MedScope</a>
        <p class="brand-subtitle">Plataforma de evidências clínicas</p>
      </div>

      <div class="topbar__actions">
        <div class="topbar__identity">
          <span class="topbar__identity-label">Conectado como</span>
          <strong>{{ professionalName }}</strong>
        </div>
        <button type="button" class="topbar__button" @click="$emit('logout')">
          Sair
        </button>
      </div>
    </div>
  </header>
</template>
