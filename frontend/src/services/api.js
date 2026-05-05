import axios from 'axios'

export const TOKEN_KEY = 'medscope.token'
export const PROFESSIONAL_KEY = 'medscope.professional'

function defaultApiBaseUrl() {
  if (window.location.hostname.endsWith('.loca.lt')) {
    return 'https://medscope-api.loca.lt'
  }

  if (window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1') {
    return 'http://localhost:8080'
  }

  return `http://${window.location.hostname}:8080`
}

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || defaultApiBaseUrl()

const api = axios.create({
  baseURL: apiBaseUrl,
  headers: {
    'Content-Type': 'application/json'
  }
})

export function getAuthToken() {
  return window.localStorage.getItem(TOKEN_KEY)
}

export function setAuthSession(authResponse) {
  window.localStorage.setItem(TOKEN_KEY, authResponse.token)
  window.localStorage.setItem(PROFESSIONAL_KEY, JSON.stringify({
    name: authResponse.name,
    crm: authResponse.crm
  }))
}

export function clearAuthSession() {
  window.localStorage.removeItem(TOKEN_KEY)
  window.localStorage.removeItem(PROFESSIONAL_KEY)
}

api.interceptors.request.use((config) => {
  const token = getAuthToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error?.response?.status === 401) {
      clearAuthSession()
      if (window.location.pathname !== '/login') {
        window.location.assign('/login')
      }
    }
    return Promise.reject(error)
  }
)

export async function loginProfessional(payload) {
  const response = await api.post('/api/auth/login', payload)
  return response.data
}

export async function getTopCids() {
  const response = await api.get('/api/stats/top-cids')
  return response.data
}

export async function searchEvidence(payload) {
  const response = await api.post('/api/search', payload)
  return response.data
}

export async function searchEvidenceStream(payload, handlers = {}) {
  const token = getAuthToken()
  const response = await fetch(`${apiBaseUrl}/api/search/stream`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {})
    },
    body: JSON.stringify(payload),
    signal: handlers.signal
  })

  if (!response.ok) {
    if (response.status === 401) {
      clearAuthSession()
      if (window.location.pathname !== '/login') {
        window.location.assign('/login')
      }
    }

    let errorMessage = 'Nao foi possivel iniciar a busca.'
    try {
      const errorBody = await response.json()
      errorMessage = errorBody?.message || errorMessage
    } catch {
      // noop
    }
    throw new Error(errorMessage)
  }

  const reader = response.body?.getReader()
  const decoder = new TextDecoder()
  let buffer = ''

  while (reader) {
    const { done, value } = await reader.read()
    if (done) {
      break
    }

    buffer += decoder.decode(value, { stream: true })
    const lines = buffer.split('\n')
    buffer = lines.pop() || ''

    for (const line of lines) {
      const trimmed = line.trim()
      if (!trimmed) {
        continue
      }

      dispatchStreamEvent(JSON.parse(trimmed), handlers)
    }
  }

  if (buffer.trim()) {
    dispatchStreamEvent(JSON.parse(buffer.trim()), handlers)
  }
}

function dispatchStreamEvent(event, handlers) {
  if (event.type === 'meta') {
    handlers.onMeta?.(event.payload)
  } else if (event.type === 'article') {
    handlers.onArticle?.(event.payload)
  } else if (event.type === 'complete') {
    handlers.onComplete?.()
  } else if (event.type === 'error') {
    throw new Error(event.message || 'Falha durante a busca.')
  }
}
