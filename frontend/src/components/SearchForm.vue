<script setup>
const query = defineModel('query')
const context = defineModel('context')
const source = defineModel('source')

defineProps({
  loading: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['submit'])

const sourceOptions = [
  { value: 'BOTH', label: 'Todos' },
  { value: 'PUBMED', label: 'PubMed' },
  { value: 'SCIELO', label: 'SciELO' }
]
</script>

<template>
  <section class="search-card">
    <div class="search-card__header">
      <h2>Nova busca</h2>
      <p>Insira um CID ou condicao em portugues, escolha a fonte e complemente com contexto clinico quando necessario.</p>
    </div>

    <form class="search-form" @submit.prevent="emit('submit')">
      <label class="field">
        <span>CID ou condicao em portugues</span>
        <input v-model="query" type="text" placeholder="Ex: F41.1 ou ansiedade generalizada" />
      </label>

      <div class="field">
        <span>Fonte cientifica</span>
        <div class="source-toggle" role="radiogroup" aria-label="Fonte cientifica">
          <button
            v-for="option in sourceOptions"
            :key="option.value"
            type="button"
            class="source-toggle__option"
            :class="{ 'source-toggle__option--active': source === option.value }"
            :aria-pressed="source === option.value"
            @click="source = option.value"
          >
            {{ option.label }}
          </button>
        </div>
      </div>

      <label class="field">
        <span>Contexto clinico opcional</span>
        <textarea
          v-model="context"
          rows="5"
          placeholder="Ex.: adultos com resposta parcial, acompanhamento ambulatorial"
        ></textarea>
      </label>

      <button class="primary-button" type="submit" :disabled="loading">
        <span v-if="loading" class="button-spinner" aria-hidden="true"></span>
        <span>{{ loading ? 'Buscando evidencias...' : 'Buscar evidencias' }}</span>
      </button>
    </form>
  </section>
</template>
