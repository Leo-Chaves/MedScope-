<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import BrandLockup from './BrandLockup.vue'
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
      'Não foi possível fazer login. Verifique seu e-mail e senha.'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="auth-layout">
    <section class="login-shell">
      <div class="login-hero">
        <BrandLockup tone="light" />
        <h1>Busque evidências clínicas com rastreabilidade científica.</h1>
        <p class="hero-description">
          Conecte-se com seu e-mail e sua senha para acessar a plataforma, pesquisar artigos e
          organizar evidências do PubMed e da SciELO em um fluxo claro para apoio clínico.
        </p>

        <div class="login-hero__panel">
          <div class="login-hero__metric">
            <strong>PubMed + SciELO</strong>
            <span>Fontes integradas para consulta médica com contexto clínico opcional.</span>
          </div>
          <div class="login-hero__metric">
            <strong>Leitura em português</strong>
            <span>Resumos estruturados para triagem rápida e avaliação profissional.</span>
          </div>
        </div>
      </div>

      <section class="login-card">
        <div class="search-card__header search-card__header--login">
          <h2>Login</h2>
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

          <button class="primary-button primary-button--login" type="submit" :disabled="loading">
            <span v-if="loading" class="button-spinner" aria-hidden="true"></span>
            <span>{{ loading ? 'Fazendo login...' : 'Login' }}</span>
          </button>
        </form>

        <p class="login-card__support">
          Ambiente destinado a profissionais de saúde para consulta informacional e apoio à
          decisão clínica.
        </p>
      </section>
    </section>
  </main>
</template>
