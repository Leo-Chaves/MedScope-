<script setup>
import { computed, onBeforeUnmount, reactive, ref } from 'vue'
import AppHeader from './components/Header.vue'
import SearchForm from './components/SearchForm.vue'
import ResultsSection from './components/ResultsSection.vue'
import { searchEvidence } from './services/api'

const form = reactive({
  cid: 'F41.1',
  context: ''
})

const loading = ref(false)
const loadingMore = ref(false)
const errorMessage = ref('')
const result = ref(null)
let continueLoadingTimer = null
let searchVersion = 0

const helperItems = [
  { cid: 'F41.1', label: 'Ansiedade generalizada' },
  { cid: 'M54.5', label: 'Dor lombar' },
  { cid: 'E11', label: 'Diabetes tipo 2' },
  { cid: 'K51.9', label: 'Retocolite ulcerativa' }
]

const hasResults = computed(() => (result.value?.articles?.length || 0) > 0)

async function handleSearch() {
  const currentVersion = ++searchVersion
  clearContinueLoadingTimer()
  errorMessage.value = ''
  loading.value = true
  loadingMore.value = false

  try {
    result.value = await searchEvidence({
      cid: form.cid,
      context: form.context,
      continueLoading: false
    })
    scheduleContinueLoading(currentVersion)
  } catch (error) {
    result.value = null
    errorMessage.value =
      error?.response?.data?.message ||
      'Não foi possível concluir a busca. Verifique a disponibilidade do backend, do PubMed e do Ollama.'
  } finally {
    loading.value = false
  }
}

function applyHelper(cid) {
  form.cid = cid
}

function scheduleContinueLoading(version, attempt = 1) {
  const hasContext = Boolean(form.context?.trim())
  const total = result.value?.articles?.length || 0
  if (hasContext || total >= 2 || attempt > 3) {
    loadingMore.value = false
    return
  }

  loadingMore.value = true
  continueLoadingTimer = window.setTimeout(async () => {
    try {
      const nextResult = await searchEvidence({
        cid: result.value?.cid || form.cid,
        context: '',
        continueLoading: true
      })

      if (version !== searchVersion) {
        return
      }

      const nextTotal = nextResult?.articles?.length || 0
      const currentTotal = result.value?.articles?.length || 0
      if (nextTotal >= currentTotal) {
        result.value = nextResult
      }

      scheduleContinueLoading(version, attempt + 1)
    } catch {
      loadingMore.value = false
    }
  }, 15000)
}

function clearContinueLoadingTimer() {
  if (continueLoadingTimer) {
    window.clearTimeout(continueLoadingTimer)
    continueLoadingTimer = null
  }
}

onBeforeUnmount(clearContinueLoadingTimer)
</script>

<template>
  <div class="app-shell">
    <AppHeader />

    <main class="app-main">
      <section class="hero-section">
        <div class="hero-copy">
          <span class="hero-kicker">Portal clínico informacional</span>
          <h1>Evidências clínicas atualizadas a partir de condições padronizadas</h1>
          <p class="hero-description">
            Pesquise por CID e acrescente contexto clínico opcional para organizar
            artigos recentes, resumos em português e notas de cautela, com foco em
            apoio informacional para avaliação profissional.
          </p>

          <div class="helper-list">
            <button
              v-for="item in helperItems"
              :key="item.cid"
              type="button"
              class="helper-chip"
              @click="applyHelper(item.cid)"
            >
              <strong>{{ item.cid }}</strong>
              <span>{{ item.label }}</span>
            </button>
          </div>
        </div>

        <SearchForm
          v-model:cid="form.cid"
          v-model:context="form.context"
          :loading="loading"
          @submit="handleSearch"
        />
      </section>

      <section v-if="errorMessage" class="message-card">
        <h2>Falha na busca</h2>
        <p>{{ errorMessage }}</p>
      </section>

      <ResultsSection
        v-if="result"
        :result="result"
        :has-results="hasResults"
        :loading-more="loadingMore"
      />
    </main>
  </div>
</template>
