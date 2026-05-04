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

export async function searchEvidence(payload) {
  const response = await api.post('/api/search', payload)
  return response.data
}
