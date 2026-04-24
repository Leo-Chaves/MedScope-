import axios from 'axios'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
  headers: {
    'Content-Type': 'application/json'
  }
})

export async function searchEvidence(payload) {
  const response = await api.post('/api/search', payload)
  return response.data
}
