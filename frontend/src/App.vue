<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import AppHeader from './components/Header.vue'
import LoginView from './components/LoginView.vue'
import SearchForm from './components/SearchForm.vue'
import ResultsSection from './components/ResultsSection.vue'
import { searchEvidence, searchEvidenceStream } from './services/api'

const SESSION_KEY = 'medscope.session'

const route = ref(resolveRoute(window.location.pathname))
const session = ref(readSession())

const form = reactive({
  cid: 'K51.9',
  context: '',
  source: 'BOTH'
})

const loading = ref(false)
const loadingMore = ref(false)
const errorMessage = ref('')
const result = ref(null)
const authError = ref('')
let continueLoadingTimer = null
let searchVersion = 0
let currentSearchController = null

const helperItems = [
  { cid: 'K51.9', label: 'Retocolite ulcerativa' },
  { cid: 'E11', label: 'Diabetes tipo 2' },
  { cid: 'L40', label: 'Psoríase' },
  { cid: 'M32', label: 'Lúpus' },
  { cid: 'J45', label: 'Asma' }
]

const hasResults = computed(() => (result.value?.articles?.length || 0) > 0)
const isAuthenticated = computed(() => Boolean(session.value))
const currentProfessional = computed(() => session.value?.name || 'Profissional de saúde')

function resolveRoute(pathname) {
  return pathname === '/home' ? '/home' : '/'
}

function readSession() {
  try {
    const raw = window.localStorage.getItem(SESSION_KEY)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

function writeSession(value) {
  if (value) {
    window.localStorage.setItem(SESSION_KEY, JSON.stringify(value))
  } else {
    window.localStorage.removeItem(SESSION_KEY)
  }
  session.value = value
}

function navigate(path, options = {}) {
  const nextRoute = resolveRoute(path)
  if (window.location.pathname !== nextRoute) {
    const method = options.replace ? 'replaceState' : 'pushState'
    window.history[method]({}, '', nextRoute)
  }
  route.value = nextRoute
}

function ensureRouteAccess() {
  const wantsHome = route.value === '/home'
  if (wantsHome && !isAuthenticated.value) {
    navigate('/', { replace: true })
    return
  }
  if (!wantsHome && isAuthenticated.value) {
    navigate('/home', { replace: true })
  }
}

function handlePopState() {
  route.value = resolveRoute(window.location.pathname)
  ensureRouteAccess()
}

function formatProfessionalName(crm) {
  const normalized = crm.trim().toUpperCase()
  return `CRM ${normalized}`
}

function handleLogin(credentials) {
  const crm = credentials.crm?.trim()
  const password = credentials.password?.trim()

  if (!crm || !password) {
    authError.value = 'Informe CRM e senha para acessar a plataforma.'
    return
  }

  writeSession({
    crm,
    name: formatProfessionalName(crm)
  })
  authError.value = ''
  navigate('/home')
}

function handleLogout() {
  abortCurrentSearch()
  clearContinueLoadingTimer()
  writeSession(null)
  result.value = null
  errorMessage.value = ''
  authError.value = ''
  navigate('/', { replace: true })
}

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
        cid: form.cid,
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
              cid: form.cid,
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
      'Não foi possível concluir a busca. Verifique a disponibilidade do backend, do PubMed, da SciELO e do Ollama.'
  } finally {
    currentSearchController = null
    loading.value = false
  }
}

function applyHelper(cid) {
  form.cid = cid
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
        cid: result.value?.cid || form.cid,
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

onMounted(() => {
  ensureRouteAccess()
  window.addEventListener('popstate', handlePopState)
})

onBeforeUnmount(() => {
  abortCurrentSearch()
  clearContinueLoadingTimer()
  window.removeEventListener('popstate', handlePopState)
})
</script>

<template>
  <div class="app-shell">
    <AppHeader
      v-if="route === '/home' && isAuthenticated"
      :professional-name="currentProfessional"
      @logout="handleLogout"
    />

    <main v-if="route === '/'" class="auth-layout">
      <LoginView :error-message="authError" @submit="handleLogin" />
    </main>

    <main v-else class="app-main">
      <section class="hero-section">
        <div class="hero-copy">
          <span class="hero-kicker">Portal clínico informacional</span>
          <h1>Evidências clínicas atualizadas a partir de condições padronizadas</h1>
          <p class="hero-description">
            Pesquise por CID, escolha a base científica e acrescente contexto clínico opcional
            para organizar artigos recentes, resumos em português e notas de cautela, com foco
            em apoio informacional para avaliação profissional.
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
  </div>
</template>
