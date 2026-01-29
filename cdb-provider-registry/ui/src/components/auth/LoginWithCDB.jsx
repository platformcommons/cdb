import React from 'react'
import { generateCodeVerifier, generateCodeChallenge } from '../../services/pkce'

export default function LoginWithCDB() {
  const handleLogin = async () => {
    const authBase = import.meta.env.VITE_CDB_AUTH_URL || 'http://localhost:8083'
    const clientId = import.meta.env.VITE_OAUTH_CLIENT_ID || 'cdb_provider_registry'

    // Compute redirect URI: prefer explicit env var, fallback to origin + base
    const explicitRedirect = import.meta.env.VITE_PUBLIC_REDIRECT_URI
    const base = import.meta.env.BASE_URL || '/'
    const baseNormalized = base.endsWith('/') ? base.slice(0, -1) : base
    const fallbackRedirect = `${window.location.origin}${baseNormalized || ''}/auth/callback`
    const redirectUri = (explicitRedirect && explicitRedirect.trim().length > 0) ? explicitRedirect : fallbackRedirect

    // State and PKCE params
    const state = Math.random().toString(36).slice(2)
    const codeVerifier = generateCodeVerifier()
    const codeChallenge = await generateCodeChallenge(codeVerifier)

    // Persist in both localStorage and sessionStorage for callback verification
    localStorage.setItem('oauth2_state', state)
    localStorage.setItem('oauth2_code_verifier', codeVerifier)
    sessionStorage.setItem('oauth2_state', state)
    sessionStorage.setItem('oauth2_code_verifier', codeVerifier)

    const params = new URLSearchParams({
      response_type: 'code',
      client_id: clientId,
      redirect_uri: redirectUri,
      scope: 'read write',
      state,
      code_challenge: codeChallenge,
      code_challenge_method: 'S256'
    })

    const authUrl = `${authBase}/oauth2/authorize?${params.toString()}`
    window.location.assign(authUrl)
  }

  return (
    <button className="login-btn" onClick={handleLogin}>
      Sign in with CDB
    </button>
  )
}
