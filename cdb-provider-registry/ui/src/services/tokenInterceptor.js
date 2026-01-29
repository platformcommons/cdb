import { getAccessToken, getRefreshToken, saveAccessToken, saveRefreshToken, clearAllTokens } from './tokenStorage'
import { apiUrl } from './api'

const AUTH_SERVICE_PREFIX = '/cdb-auth-registry'
const AUTH_MODE = (import.meta?.env?.VITE_AUTH_MODE || 'oauth2').toLowerCase()
let isRefreshing = false
let refreshPromise = null

export async function refreshAccessToken() {
  if (isRefreshing && refreshPromise) {
    return refreshPromise
  }

  isRefreshing = true
  refreshPromise = performRefresh()
  
  try {
    const result = await refreshPromise
    return result
  } finally {
    isRefreshing = false
    refreshPromise = null
  }
}

async function performRefresh() {
  const refreshToken = await getRefreshToken()
  if (!refreshToken) {
    throw new Error('No refresh token available')
  }

  if (AUTH_MODE === 'direct') {
    const response = await fetch(apiUrl(`${AUTH_SERVICE_PREFIX}/api/v1/auth/refresh`), {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken })
    })

    if (!response.ok) {
      clearAllTokens()
      throw new Error('Token refresh failed')
    }

    const data = await response.json()
    const expiresAt = data?.expiresIn ? Date.now() + (data.expiresIn * 1000) : null
    
    if (data?.accessToken) {
      await saveAccessToken(data.accessToken, expiresAt)
    }
    if (data?.refreshToken) {
      await saveRefreshToken(data.refreshToken)
    }

    return data.accessToken
  } else {
    // OAuth2 refresh flow
    const authBase = import.meta.env.VITE_CDB_AUTH_URL || 'http://localhost:8083'
    const clientId = import.meta.env.VITE_OAUTH_CLIENT_ID || 'cdb_provider_registry'
    const body = new URLSearchParams({
      grant_type: 'refresh_token',
      refresh_token: refreshToken,
      client_id: clientId
    })
    const response = await fetch(`${authBase}/oauth2/token`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body
    })

    if (!response.ok) {
      clearAllTokens()
      throw new Error('Token refresh failed')
    }

    const data = await response.json()
    const expiresAt = data?.expires_in ? Date.now() + (data.expires_in * 1000) : null
    if (data?.access_token) await saveAccessToken(data.access_token, expiresAt)
    if (data?.refresh_token) await saveRefreshToken(data.refresh_token)
    return data.access_token
  }
}

export async function interceptedFetch(path, options = {}) {
  const url = apiUrl(path)
  let token = await getAccessToken()
  
  const headers = new Headers(options.headers || {})
  if (token && !headers.has('Authorization')) {
    headers.set('Authorization', `Bearer ${token}`)
  }

  let response = await fetch(url, { ...options, headers })

  // If 401 and we have a refresh token, try to refresh
  if (response.status === 401 && await getRefreshToken()) {
    try {
      token = await refreshAccessToken()
      if (token) {
        headers.set('Authorization', `Bearer ${token}`)
        response = await fetch(url, { ...options, headers })
      }
    } catch (error) {
      console.warn('Token refresh failed:', error)
      // Let the original 401 response be handled by the caller
    }
  }

  return response
}