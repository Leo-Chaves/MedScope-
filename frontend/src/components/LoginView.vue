<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { loginProfessional, setAuthSession } from '../services/api'

const router = useRouter()

const form = reactive({
  email: '',
  password: ''
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
    setAuthSession(authResponse)
    router.push('/')
  } catch (error) {
    errorMessage.value =
      error?.response?.data?.message ||
      error?.message ||
      'Nao foi possivel entrar. Verifique seu e-mail e senha.'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="auth-layout">
    <section class="login-shell">
      <div class="login-hero">
        <span class="hero-kicker">Acesso profissional</span>
        <h1>Busque evidencias clinicas com rastreabilidade cientifica.</h1>
        <p class="hero-description">
          Conecte-se com e-mail e senha para acessar sua area, pesquisar artigos e organizar
          evidencias do PubMed e da SciELO em um fluxo simples para apoio clinico.
        </p>
      </div>

      <section class="login-card">
        <div class="search-card__header">
          <h2>Entrar</h2>
          <p>Use seu e-mail profissional e sua senha para acessar a plataforma.</p>
        </div>

        <form class="search-form" @submit.prevent="handleSubmit">
          <label class="field">
            <span>E-mail</span>
            <input
              v-model="form.email"
              type="email"
              placeholder="voce@clinica.com"
              autocomplete="username"
            />
          </label>

          <label class="field">
            <span>Senha</span>
            <input
              v-model="form.password"
              type="password"
              placeholder="Digite sua senha"
              autocomplete="current-password"
            />
          </label>

          <p v-if="errorMessage" class="login-error">{{ errorMessage }}</p>

          <button class="primary-button" type="submit" :disabled="loading">
            <span v-if="loading" class="button-spinner" aria-hidden="true"></span>
            <span>{{ loading ? 'Entrando...' : 'Entrar' }}</span>
          </button>
        </form>
      </section>
    </section>
  </main>
</template>
