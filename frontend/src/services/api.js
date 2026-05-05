import axios from 'axios'

function defaultApiBaseUrl() {
  if (window.location.hostname.endsWith('.loca.lt')) {
    return 'https://medscope-api.loca.lt'
  }

  if (window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1') {
    return 'http://localhost:8080'
  }

  return `http://${window.location.hostname}:8080`
}

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || defaultApiBaseUrl(),
  headers: {
    'Content-Type': 'application/json'
  }
})

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || defaultApiBaseUrl()

export async function searchEvidence(payload) {
  const response = await api.post('/api/search', payload)
  return response.data
}

export async function searchEvidenceStream(payload, handlers = {}) {
  const response = await fetch(`${apiBaseUrl}/api/search/stream`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(payload),
    signal: handlers.signal
  })

  if (!response.ok) {
    let errorMessage = 'Não foi possível iniciar a busca.'
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

      const event = JSON.parse(trimmed)
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
  }

  if (buffer.trim()) {
    const event = JSON.parse(buffer.trim())
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
}
