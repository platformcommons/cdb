// Token storage with encryption in sessionStorage
// Key: 'cdb.access_token'
// Uses Web Crypto API (AES-GCM) to encrypt/decrypt tokens in-browser.

const ACCESS_TOKEN_KEY = 'cdb.access_token'
const REFRESH_TOKEN_KEY = 'cdb.refresh_token'
const ALGO = 'AES-GCM'

function getPassphrase() {
  // Derive a pseudo secret from origin + a static string (not secure for prod, but better than plaintext)
  const origin = typeof window !== 'undefined' ? window.location.origin : 'node'
  return `${origin}::cdb-ui-secret-v1`
}

async function deriveKey(passphrase, saltBytes) {
  const enc = new TextEncoder()
  const keyMaterial = await crypto.subtle.importKey(
    'raw', enc.encode(passphrase), { name: 'PBKDF2' }, false, ['deriveKey']
  )
  return crypto.subtle.deriveKey(
    {
      name: 'PBKDF2',
      salt: saltBytes,
      iterations: 100_000,
      hash: 'SHA-256'
    },
    keyMaterial,
    { name: ALGO, length: 256 },
    false,
    ['encrypt', 'decrypt']
  )
}

function b64encode(bytes) {
  if (typeof window === 'undefined') return Buffer.from(bytes).toString('base64')
  return btoa(String.fromCharCode(...new Uint8Array(bytes)))
}

function b64decode(b64) {
  if (typeof window === 'undefined') return new Uint8Array(Buffer.from(b64, 'base64'))
  const binary = atob(b64)
  const bytes = new Uint8Array(binary.length)
  for (let i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i)
  return bytes
}

async function saveToken(key, token, expiresAtEpochMs) {
  if (typeof window === 'undefined' || !window.crypto?.subtle) {
    sessionStorage.setItem(key, token)
    return
  }
  const enc = new TextEncoder()
  const iv = crypto.getRandomValues(new Uint8Array(12))
  const salt = crypto.getRandomValues(new Uint8Array(16))
  const cryptoKey = await deriveKey(getPassphrase(), salt)
  const cipher = await crypto.subtle.encrypt(
    { name: ALGO, iv },
    cryptoKey,
    enc.encode(token)
  )
  const payload = {
    v: 1,
    iv: b64encode(iv),
    salt: b64encode(salt),
    ct: b64encode(cipher),
    exp: expiresAtEpochMs || null
  }
  sessionStorage.setItem(key, JSON.stringify(payload))
}

export async function saveAccessToken(token, expiresAtEpochMs) {
  await saveToken(ACCESS_TOKEN_KEY, token, expiresAtEpochMs)
}

export async function saveRefreshToken(token) {
  await saveToken(REFRESH_TOKEN_KEY, token)
}

async function getToken(key, checkExpiry = false) {
  const raw = sessionStorage.getItem(key)
  if (!raw) return null
  try {
    // plain token for non-WebCrypto fallback
    if (!raw.startsWith('{')) return raw
    const payload = JSON.parse(raw)
    if (checkExpiry && payload.exp && Date.now() > payload.exp) {
      sessionStorage.removeItem(key)
      return null
    }
    if (!(window.crypto?.subtle)) return null
    const iv = b64decode(payload.iv)
    const salt = b64decode(payload.salt)
    const ct = b64decode(payload.ct)
    const cryptoKey = await deriveKey(getPassphrase(), salt)
    const plainBuf = await crypto.subtle.decrypt(
      { name: ALGO, iv },
      cryptoKey,
      ct
    )
    const dec = new TextDecoder()
    return dec.decode(plainBuf)
  } catch (e) {
    console.warn(`Failed to decrypt token from ${key}, clearing`, e)
    sessionStorage.removeItem(key)
    return null
  }
}

export async function getAccessToken() {
  return getToken(ACCESS_TOKEN_KEY, true)
}

export async function getRefreshToken() {
  return getToken(REFRESH_TOKEN_KEY)
}

export function clearAccessToken() {
  sessionStorage.removeItem(ACCESS_TOKEN_KEY)
}

export function clearRefreshToken() {
  sessionStorage.removeItem(REFRESH_TOKEN_KEY)
}

export function clearAllTokens() {
  clearAccessToken()
  clearRefreshToken()
}
