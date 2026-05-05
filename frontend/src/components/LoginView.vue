<script setup>
import { reactive } from 'vue'

defineProps({
  errorMessage: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['submit'])

const form = reactive({
  crm: '',
  password: ''
})

function handleSubmit() {
  emit('submit', { ...form })
}
</script>

<template>
  <section class="login-shell">
    <div class="login-hero">
      <span class="hero-kicker">Acesso profissional</span>
      <h1>Busque evidências clínicas com rastreabilidade científica.</h1>
      <p class="hero-description">
        Conecte-se com CRM e senha para acessar sua área, pesquisar artigos e organizar
        evidências do PubMed e da SciELO em um fluxo simples para apoio clínico.
      </p>
    </div>

    <section class="login-card">
      <div class="search-card__header">
        <h2>Entrar</h2>
        <p>Use seu CRM profissional e sua senha para acessar a plataforma.</p>
      </div>

      <form class="search-form" @submit.prevent="handleSubmit">
        <label class="field">
          <span>CRM</span>
          <input v-model="form.crm" type="text" placeholder="Ex.: 123456-PE" autocomplete="username" />
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

        <button class="primary-button" type="submit">
          Entrar
        </button>
      </form>
    </section>
  </section>
</template>
