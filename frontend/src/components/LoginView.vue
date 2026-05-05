<script setup>
import { reactive, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import BrandLockup from './BrandLockup.vue'
import { loginProfessional, setAuthSession } from '../services/api'

const router = useRouter()

const form = reactive({
  email: '',
  password: '',
  remember: true
})

const loading = ref(false)
const errorMessage = ref('')

async function handleSubmit() {
  errorMessage.value = ''
  loading.value = true

  try {
    const authResponse = await loginProfessional({
      email: form.email,
      password: form.password
    })
    setAuthSession(authResponse, { remember: form.remember })
    router.push('/app')
  } catch {
    errorMessage.value = 'Email ou senha invalidos'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="auth-page">
    <div class="auth-page__backdrop" aria-hidden="true"></div>

    <div class="auth-page__content">
      <RouterLink class="auth-back" to="/">
        <svg viewBox="0 0 20 20" aria-hidden="true" focusable="false">
          <path
            d="M11.75 4.25L6 10l5.75 5.75M6.5 10h8"
            fill="none"
            stroke="currentColor"
            stroke-linecap="round"
            stroke-linejoin="round"
            stroke-width="1.8"
          />
        </svg>
        <span>Voltar para Home</span>
      </RouterLink>

      <section class="auth-card" aria-labelledby="login-title">
        <div class="auth-card__brand">
          <BrandLockup :show-symbol="false" />
        </div>

        <div class="auth-card__header">
          <h1 id="login-title">Acesse sua conta</h1>
          <p>Entre para buscar evidencias clinicas com IA</p>
        </div>

        <form class="auth-form" @submit.prevent="handleSubmit">
          <label class="auth-field">
            <span>Email</span>
            <input
              v-model="form.email"
              type="email"
              name="email"
              placeholder="voce@clinica.com"
              autocomplete="username"
              autofocus
              required
            />
          </label>

          <label class="auth-field">
            <span>Senha</span>
            <input
              v-model="form.password"
              type="password"
              name="password"
              placeholder="Digite sua senha"
              autocomplete="current-password"
              required
            />
          </label>

          <div class="auth-row">
            <label class="auth-checkbox">
              <input v-model="form.remember" type="checkbox" name="remember" />
              <span>Lembrar de mim</span>
            </label>

            <a class="auth-inline-link" href="/">Esqueci minha senha</a>
          </div>

          <p v-if="errorMessage" class="auth-error" role="alert" aria-live="polite">
            {{ errorMessage }}
          </p>

          <button class="auth-submit" type="submit" :disabled="loading">
            <span v-if="loading" class="button-spinner" aria-hidden="true"></span>
            <span>{{ loading ? 'Entrando...' : 'Entrar' }}</span>
          </button>
        </form>

        <div class="auth-divider" aria-hidden="true">
          <span></span>
          <strong>ou</strong>
          <span></span>
        </div>

        <RouterLink class="auth-secondary" to="/">Criar conta</RouterLink>
      </section>
    </div>
  </main>
</template>
