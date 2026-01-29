import { apiFetch } from './api'
import { saveAccessToken, saveRefreshToken, clearAllTokens } from './tokenStorage'

// Auth service for login/logout flows
// Calls Auth Registry via Gateway route prefix /cdb-auth-registry

const AUTH_SERVICE_PREFIX = '/cdb-auth-registry'
const USER_KEY = 'cdb.user'

export function getCurrentUser() {
  try {
    const raw = sessionStorage.getItem(USER_KEY)
    if (!raw) return null
    return JSON.parse(raw)
  } catch {
    return null
  }
}

function setCurrentUser(user) {
  if (!user) return
  sessionStorage.setItem(USER_KEY, JSON.stringify(user))
}

function clearCurrentUser() {
  sessionStorage.removeItem(USER_KEY)
}

export async function login(username, password) {
  const res = await apiFetch(`${AUTH_SERVICE_PREFIX}/api/v1/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password })
  })
  if (!res.ok) {
    let msg = 'Login failed'
    try { msg = (await res.text()) || msg } catch {}
    throw new Error(msg)
  }
  const data = await res.json() // TokenResponse { accessToken, refreshToken, tokenType, expiresIn }
  const expiresAt = data?.expiresIn ? Date.now() + (data.expiresIn * 1000) : null
  if (data?.accessToken) {
    await saveAccessToken(data.accessToken, expiresAt)
  }
  if (data?.refreshToken) {
    await saveRefreshToken(data.refreshToken)
  }
  // store minimal user info for UI (we use username as email id in login form)
  if (username) setCurrentUser({ username })
  return data
}

export async function fetchMyProviders() {
  const res = await apiFetch(`${AUTH_SERVICE_PREFIX}/api/v1/auth/my-providers`)
  if (!res.ok) {
    throw new Error('Failed to fetch provider contexts')
  }
  return res.json() // [{providerId, providerCode}]
}

export async function setExecutiveContext(providerCode, roleCodes = [], authorityCodes = []) {
  const res = await apiFetch(`${AUTH_SERVICE_PREFIX}/api/v1/auth/context`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ providerCode, roleCodes, authorityCodes })
  })
  if (!res.ok) {
    let msg = 'Failed to set context'
    try { msg = (await res.text()) || msg } catch {}
    throw new Error(msg)
  }
  const data = await res.json()
  const expiresAt = data?.expiresIn ? Date.now() + (data.expiresIn * 1000) : null
  if (data?.accessToken) {
    await saveAccessToken(data.accessToken, expiresAt)
  }
  if (data?.refreshToken) {
    await saveRefreshToken(data.refreshToken)
  }
  return data
}

export function logout() {
  clearAllTokens()
  clearCurrentUser()
  if (window.refreshAuth) window.refreshAuth()
}

export async function refreshToken() {
  const { refreshAccessToken } = await import('./tokenInterceptor')
  return refreshAccessToken()
}

export function isAuthenticated() {
  // lightweight check based on stored user marker
  return !!getCurrentUser()
}
