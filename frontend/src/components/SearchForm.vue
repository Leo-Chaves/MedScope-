<script setup>
const cid = defineModel('cid')
const context = defineModel('context')

defineProps({
  loading: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['submit'])
</script>

<template>
  <section class="search-card">
    <div class="search-card__header">
      <h2>Nova busca</h2>
      <p>Insira um CID válido e complemente com contexto clínico quando necessário.</p>
    </div>

    <form class="search-form" @submit.prevent="emit('submit')">
      <label class="field">
        <span>CID</span>
        <input v-model="cid" type="text" placeholder="Ex.: F41.1" maxlength="10" />
      </label>

      <label class="field">
        <span>Contexto clínico opcional</span>
        <textarea
          v-model="context"
          rows="5"
          placeholder="Ex.: adultos com resposta parcial, acompanhamento ambulatorial"
        ></textarea>
      </label>

      <button class="primary-button" type="submit" :disabled="loading">
        <span v-if="loading" class="button-spinner" aria-hidden="true"></span>
        <span>{{ loading ? 'Buscando evidências...' : 'Buscar evidências' }}</span>
      </button>
    </form>
  </section>
</template>
