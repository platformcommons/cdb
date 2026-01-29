import axios from 'axios'
import { API_BASE_URL } from '@constants/const'

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
})

// Attach Authorization header if access token is present
apiClient.interceptors.request.use((config) => {
  try {
    const ls = typeof window !== 'undefined' ? window.localStorage : undefined
    const ss = typeof window !== 'undefined' ? window.sessionStorage : undefined
    const token = ls?.getItem('cdb_access_token') || ss?.getItem('cdb_access_token')
    if (token) {
      config.headers = config.headers || {}
      // Do not override if already set explicitly
      if (!('Authorization' in config.headers)) {
        ;(config.headers as any).Authorization = `Bearer ${token}`
      }
    }
  } catch {}
  return config
})

// Simple interceptor placeholder for future auth/error handling
apiClient.interceptors.response.use(
  (res) => res,
  (err) => {
    // eslint-disable-next-line no-console
    console.error('API error', err)
    return Promise.reject(err)
  }
)
