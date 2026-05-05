<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import BrandLockup from './BrandLockup.vue'
import { clearAuthSession, getAuthToken } from '../services/api'

const router = useRouter()

const professional = computed(() => decodeProfessional(getAuthToken()))

function decodeProfessional(token) {
  if (!token) {
    return {
      name: 'Profissional de saude',
      crm: ''
    }
  }

  try {
    const base64Payload = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')
    const paddedPayload = base64Payload.padEnd(Math.ceil(base64Payload.length / 4) * 4, '=')
    const payload = JSON.parse(window.atob(paddedPayload))
    return {
      name: payload.name || 'Profissional de saude',
      crm: payload.crm || ''
    }
  } catch {
    return {
      name: 'Profissional de saude',
      crm: ''
    }
  }
}

function handleLogout() {
  clearAuthSession()
  router.push('/login')
}
</script>

<template>
  <header class="topbar">
    <div class="topbar__content">
      <a href="/" class="brand-link" aria-label="MedScope">
        <BrandLockup compact />
      </a>

      <div class="topbar__actions">
        <div class="topbar__identity">
          <span class="topbar__identity-label">Conectado como</span>
          <strong>{{ professional.name }}</strong>
          <span v-if="professional.crm" class="topbar__identity-label">{{ professional.crm }}</span>
        </div>
        <button type="button" class="topbar__button" @click="handleLogout">
          Sair
        </button>
      </div>
    </div>
  </header>
</template>
