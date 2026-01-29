// Lightweight API client helper for configuring base URL in one place
// Usage:
//
// Base URL resolution order:
// - Vite env: import.meta.env.VITE_API_BASE_URL
// - Runtime global (optional): window.__API_BASE_URL__
// - Fallback: '' (same-origin)

let ENV_BASE = ''
try {
    // import.meta is available in Vite/ESM context
    ENV_BASE = (import.meta && import.meta.env && import.meta.env.VITE_API_BASE_URL) || ''
} catch (e) {
    ENV_BASE = ''
}

export const API_BASE_URL = (typeof window !== 'undefined' && window.__API_BASE_URL__) || ENV_BASE || ''

function joinUrl(base, path) {
    if (!base) return path
    // ensure exactly one slash between base and path
    const b = base.endsWith('/') ? base.slice(0, -1) : base
    const p = path.startsWith('/') ? path : `/${path}`
    return `${b}${p}`
}

export function apiUrl(path) {
    return joinUrl(API_BASE_URL, path)
}

import { interceptedFetch } from './tokenInterceptor'

let tokenExpiredHandler = null

export function setTokenExpiredHandler(handler) {
    tokenExpiredHandler = handler
}

export async function apiFetch(path, options = {}) {
    const response = await interceptedFetch(path, options)
    
    // Only trigger token expired handler if refresh also failed
    if (response.status === 401 && tokenExpiredHandler) {
        tokenExpiredHandler()
    }
    
    return response
}

// Axios-like tiny wrapper used by existing services
async function request(method, path, body, headers = {}) {
    const options = { method, headers: { 'Content-Type': 'application/json', ...headers } }
    if (body !== undefined && body !== null) options.body = typeof body === 'string' ? body : JSON.stringify(body)
    const res = await apiFetch(path, options)
    const contentType = res.headers.get('content-type') || ''
    const data = contentType.includes('application/json') ? await res.json() : await res.text()
    if (!res.ok) {
        const err = new Error(`HTTP ${res.status}`)
        err.response = { status: res.status, data }
        throw err
    }
    return { data, status: res.status }
}

const apiDefault = {
    get: (path, config = {}) => request('GET', path, undefined, config.headers),
    delete: (path, config = {}) => request('DELETE', path, undefined, config.headers),
    post: (path, data, config = {}) => request('POST', path, data, config.headers),
    put: (path, data, config = {}) => request('PUT', path, data, config.headers),
    patch: (path, data, config = {}) => request('PATCH', path, data, config.headers),
}

export default apiDefault
