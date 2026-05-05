<script setup>
const cid = defineModel('cid')
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
      <p>Insira um CID válido, escolha a fonte e complemente com contexto clínico quando necessário.</p>
    </div>

    <form class="search-form" @submit.prevent="emit('submit')">
      <label class="field">
        <span>CID</span>
        <input v-model="cid" type="text" placeholder="Ex.: F41.1" maxlength="10" />
      </label>

      <div class="field">
        <span>Fonte científica</span>
        <div class="source-toggle" role="radiogroup" aria-label="Fonte científica">
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
