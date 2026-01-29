import React, { useEffect, useState } from 'react'
import { useNavigate, useLocation } from 'react-router-dom'
import { saveAccessToken, saveRefreshToken } from '../services/tokenStorage'
import { fetchMyProviders, setExecutiveContext } from '../services/authService'

function decodeJwt(token) {
  try {
    const [_h, p] = token.split('.')
    const json = atob(p.replace(/-/g, '+').replace(/_/g, '/'))
    return JSON.parse(decodeURIComponent(escape(json)))
  } catch {
    return null
  }
}

export default function AuthCallbackPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const [error, setError] = useState('')
  const [processing, setProcessing] = useState(false)
    const [providerOptions, setProviderOptions] = useState([])
    const [showProviderModal, setShowProviderModal] = useState(false)
    const [selecting, setSelecting] = useState(false)

  useEffect(() => {
    if (processing) return
    
    const run = async () => {
      setProcessing(true)
      const params = new URLSearchParams(location.search)
      const code = params.get('code')
      const state = params.get('state')
      const signup = params.get('signup')
      
      // If coming from signup page, just navigate to root
      if (signup === 'success') {
        navigate('/', { replace: true })
        return
      }
      let storedState = localStorage.getItem('oauth2_state')
      let codeVerifier = localStorage.getItem('oauth2_code_verifier')

      // Fallback: try sessionStorage if localStorage is empty
      if (!storedState) storedState = sessionStorage.getItem('oauth2_state')
      if (!codeVerifier) codeVerifier = sessionStorage.getItem('oauth2_code_verifier')

      // Debug logging
      console.log('OAuth2 callback params:', { code: !!code, state, storedState, codeVerifier: !!codeVerifier })
      console.log('localStorage check:', localStorage.getItem('oauth2_state'), localStorage.getItem('oauth2_code_verifier'))
      console.log('sessionStorage check:', sessionStorage.getItem('oauth2_state'), sessionStorage.getItem('oauth2_code_verifier'))
      
      if (!code) {
        setError('Missing authorization code')
        return
      }
      if (!state) {
        setError('Missing state parameter')
        return
      }
      if (!storedState) {
        setError('Missing stored state - session may have expired')
        return
      }
      if (!codeVerifier) {
        setError('Missing code verifier - session may have expired. Please try logging in again.')
        setTimeout(() => navigate('/login', { replace: true }), 3000)
        return
      }
      if (state !== storedState) {
        setError('State mismatch - possible CSRF attack')
        return
      }

      const authBase = import.meta.env.VITE_CDB_AUTH_URL || 'http://localhost:8083'
      const clientId = import.meta.env.VITE_OAUTH_CLIENT_ID || 'cdb_provider_registry'

      // Compute redirect URI again (must match)
      const explicitRedirect = import.meta.env.VITE_PUBLIC_REDIRECT_URI
      const base = import.meta.env.BASE_URL || '/'
      const baseNormalized = base.endsWith('/') ? base.slice(0, -1) : base
      const fallbackRedirect = `${window.location.origin}${baseNormalized || ''}/auth/callback`
      const redirectUri = (explicitRedirect && explicitRedirect.trim().length > 0) ? explicitRedirect : fallbackRedirect

      const body = new URLSearchParams({
        grant_type: 'authorization_code',
        code,
        client_id: clientId,
        code_verifier: codeVerifier,
        redirect_uri: redirectUri
      })

      const res = await fetch(`${authBase}/oauth2/token`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body
      })

      if (!res.ok) {
        let msg = 'Token exchange failed'
        try {
          const errorText = await res.text()
          console.error('Token exchange error:', res.status, errorText)
          msg = errorText || `HTTP ${res.status}: Token exchange failed`
        } catch (e) {
          console.error('Failed to read error response:', e)
        }
        setError(msg)
        return
      }

      const data = await res.json()
      
      // Clean up storage after successful token exchange
      localStorage.removeItem('oauth2_state')
      localStorage.removeItem('oauth2_code_verifier')
      sessionStorage.removeItem('oauth2_state')
      sessionStorage.removeItem('oauth2_code_verifier')
      
      // Expect: { accessToken, refresh_token?, token_type, expiresIn, id_token? }
      const expiresAt = data?.expiresIn ? Date.now() + (data.expiresIn * 1000) : null
      if (data?.accessToken) await saveAccessToken(data.accessToken, expiresAt)

      // Store minimal user marker for routing; try to take from id_token or access_token
      const claims = decodeJwt(data?.accessToken) || {}
      console.log(claims);
      const email = claims.sub
      const userId = claims.userId
      const userCtx = claims.ctx.user
      try {
        sessionStorage.setItem('cdb.user_email', email)
        sessionStorage.setItem('cdb.user_id', userId)
        sessionStorage.setItem('cdb.user', JSON.stringify({ userCtx }))
      } catch {}

      try {
        // After login, fetch provider options and set context similar to direct login
        const options = await fetchMyProviders()
        if (Array.isArray(options) && options.length > 1) {
            setProviderOptions(options)
            setShowProviderModal(true)
        } else if (Array.isArray(options) && options.length === 1) {
          await setExecutiveContext(options[0].providerCode)
          if (window.refreshAuth) window.refreshAuth()
          navigate('/dashboard', { replace: true })
        } else {
          navigate('/no-provider', { replace: true })
        }
      } catch (e) {
        console.error('Post-login flow failed', e)
        if (window.refreshAuth) window.refreshAuth()
        navigate('/login', { replace: true })
      }
    }

    run()
  }, [processing])

  const handleProviderSelect = async (providerCode) => {
    setSelecting(true)
    try {
      await setExecutiveContext(providerCode)
      setShowProviderModal(false)
      // Refresh auth state before navigation
      if (window.refreshAuth) window.refreshAuth()
      navigate('/dashboard', { replace: true })
    } catch (e) {
      setError(e.message || 'Failed to set provider context')
    } finally {
      setSelecting(false)
    }
  }

  return (
    <div className="container" style={{ padding: '32px 16px' }}>
      <h2>Signing you in…</h2>
      {error && <p style={{ color: '#dc2626' }}>{error}</p>}
      {!error && <p>Please wait while we complete authentication.</p>}
      
      {showProviderModal && (
        <div style={modalStyles.backdrop}>
          <div style={modalStyles.modal} role="dialog" aria-modal="true">
            <div style={{display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '20px'}}>
              <div style={{
                width: '40px',
                height: '40px',
                background: 'linear-gradient(135deg, var(--primary), #6366f1)',
                borderRadius: '12px',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center'
              }}>
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="white" strokeWidth="2">
                  <path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
                  <circle cx="8.5" cy="7" r="4"/>
                  <line x1="20" y1="8" x2="20" y2="14"/>
                  <line x1="23" y1="11" x2="17" y2="11"/>
                </svg>
              </div>
              <div>
                <h3 style={{margin: 0, fontSize: '20px', fontWeight: '600', color: 'var(--text)'}}>Choose Provider Context</h3>
                <p style={{margin: 0, fontSize: '14px', color: 'var(--muted)'}}>Select which provider context you want to use for this session.</p>
              </div>
            </div>
            <div style={{display:'grid', gap:'0px'}}>
              {providerOptions.map((opt, idx) => (
                <button key={idx}
                        disabled={selecting}
                        style={modalStyles.optionBtn}
                        onMouseEnter={(e) => {
                          e.target.style.borderColor = 'rgba(255,255,255,0.25)'
                          e.target.style.background = 'rgba(255,255,255,0.08)'
                          e.target.style.transform = 'translateY(-1px)'
                        }}
                        onMouseLeave={(e) => {
                          e.target.style.borderColor = 'rgba(255,255,255,0.15)'
                          e.target.style.background = 'rgba(255,255,255,0.04)'
                          e.target.style.transform = 'translateY(0px)'
                        }}
                        onClick={() => handleProviderSelect(opt.providerCode)}>
                  <div style={{display: 'flex', alignItems: 'center', gap: '12px'}}>
                    <div style={{
                      width: '32px',
                      height: '32px',
                      background: 'rgba(79,70,229,0.2)',
                      borderRadius: '8px',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      fontSize: '14px',
                      fontWeight: '700',
                      color: '#c7c9ff'
                    }}>
                      {opt.providerCode.charAt(0).toUpperCase()}
                    </div>
                    <div style={{flex: 1, textAlign: 'left'}}>
                      <div style={{fontWeight: 600, fontSize: '16px', color: 'var(--text)'}}>{opt.providerCode}</div>
                      <div style={{fontSize: '12px', color: 'var(--muted)'}}>ID: {opt.providerId ?? '—'}</div>
                    </div>
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--muted)" strokeWidth="2">
                      <polyline points="9,18 15,12 9,6"></polyline>
                    </svg>
                  </div>
                </button>
              ))}
            </div>
            <div style={{marginTop:'20px', display:'flex', justifyContent:'flex-end', gap:'12px'}}>
              <button 
                className="btn" 
                onClick={() => setShowProviderModal(false)} 
                disabled={selecting}
                style={{
                  padding: '10px 16px',
                  borderRadius: '8px',
                  border: '1px solid rgba(255,255,255,0.15)',
                  background: 'rgba(255,255,255,0.04)',
                  color: 'var(--text)',
                  cursor: 'pointer'
                }}
              >
                Cancel
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

const modalStyles = {
  backdrop: {
    position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.55)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000
  },
  modal: {
    background: 'var(--card)', padding: '24px', borderRadius: '16px', width: 'min(500px, 96vw)', boxShadow: '0 25px 50px -12px rgba(0, 0, 0, 0.25)', border: '1px solid rgba(255,255,255,0.1)', backdropFilter: 'blur(10px)'
  },
  optionBtn: {
    textAlign: 'left', border: '1px solid rgba(255,255,255,0.15)', borderRadius: '12px', padding: '14px 16px', background: 'rgba(255,255,255,0.04)', cursor: 'pointer', color: 'var(--text)', transition: 'all 0.2s ease', marginBottom: '8px'
  }
}
