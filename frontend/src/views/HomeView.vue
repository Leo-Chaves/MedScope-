<script setup>
import { computed, onBeforeUnmount, reactive, ref } from 'vue'
import AppHeader from '../components/Header.vue'
import SearchForm from '../components/SearchForm.vue'
import ResultsSection from '../components/ResultsSection.vue'
import { searchEvidence, searchEvidenceStream } from '../services/api'

const form = reactive({
  query: 'K51.9',
  context: '',
  source: 'BOTH'
})

const loading = ref(false)
const loadingMore = ref(false)
const errorMessage = ref('')
const result = ref(null)
let continueLoadingTimer = null
let searchVersion = 0
let currentSearchController = null

const helperItems = [
  { cid: 'K51.9', label: 'Retocolite ulcerativa' },
  { cid: 'E11', label: 'Diabetes tipo 2' },
  { cid: 'L40', label: 'Psoriase' },
  { cid: 'M32', label: 'Lupus' },
  { cid: 'J45', label: 'Asma' }
]

const hasResults = computed(() => (result.value?.articles?.length || 0) > 0)

async function handleSearch() {
  const currentVersion = ++searchVersion
  abortCurrentSearch()
  clearContinueLoadingTimer()
  errorMessage.value = ''
  result.value = null
  loading.value = true
  loadingMore.value = false
  currentSearchController = new AbortController()

  try {
    await searchEvidenceStream(
      {
        query: form.query,
        context: form.context,
        source: form.source,
        continueLoading: false
      },
      {
        signal: currentSearchController.signal,
        onMeta: (payload) => {
          if (currentVersion !== searchVersion) {
            return
          }
          result.value = {
            ...payload,
            articles: []
          }
        },
        onArticle: (article) => {
          if (currentVersion !== searchVersion) {
            return
          }

          if (!result.value) {
            result.value = {
              cid: form.query,
              condition: '',
              queryUsed: '',
              refreshedAt: null,
              disclaimer: '',
              articles: []
            }
          }

          const existingArticles = result.value.articles || []
          const articleKey = `${article.source}-${article.sourceId}`
          if (existingArticles.some((item) => `${item.source}-${item.sourceId}` === articleKey)) {
            return
          }

          result.value = {
            ...result.value,
            articles: [...existingArticles, article]
          }
        },
        onComplete: () => {
          if (currentVersion !== searchVersion) {
            return
          }
          scheduleContinueLoading(currentVersion)
        }
      }
    )
  } catch (error) {
    if (error?.name === 'AbortError') {
      return
    }
    result.value = null
    errorMessage.value =
      error?.message ||
      error?.response?.data?.message ||
      'Nao foi possivel concluir a busca. Verifique a disponibilidade do backend, do PubMed, da SciELO e do Ollama.'
  } finally {
    currentSearchController = null
    loading.value = false
  }
}

function applyHelper(cid) {
  form.query = cid
}

function scheduleContinueLoading(version, attempt = 1) {
  const hasContext = Boolean(form.context?.trim())
  const total = result.value?.articles?.length || 0
  const usesPubMedCacheOnly = form.source === 'PUBMED'
  if (!usesPubMedCacheOnly || hasContext || total >= 2 || attempt > 3) {
    loadingMore.value = false
    return
  }

  loadingMore.value = true
  continueLoadingTimer = window.setTimeout(async () => {
    try {
      const nextResult = await searchEvidence({
        query: result.value?.cid || form.query,
        context: '',
        source: form.source,
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

function abortCurrentSearch() {
  if (currentSearchController) {
    currentSearchController.abort()
    currentSearchController = null
  }
}

onBeforeUnmount(() => {
  abortCurrentSearch()
  clearContinueLoadingTimer()
})
</script>

<template>
  <AppHeader />

  <main class="app-main">
    <section class="hero-section">
      <div class="hero-copy">
        <span class="hero-kicker">Portal clinico informacional</span>
        <h1>Evidencias clinicas atualizadas a partir de condicoes padronizadas</h1>
        <p class="hero-description">
          Pesquise por CID, escolha a base cientifica e acrescente contexto clinico opcional
          para organizar artigos recentes, resumos em portugues e notas de cautela, com foco
          em apoio informacional para avaliacao profissional.
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
        v-model:query="form.query"
        v-model:context="form.context"
        v-model:source="form.source"
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
      :loading="loading"
      :has-results="hasResults"
      :loading-more="loadingMore"
    />
  </main>
</template>
