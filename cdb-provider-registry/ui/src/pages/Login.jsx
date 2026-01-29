import React, { useEffect, useState } from 'react'
import { Link, useNavigate, useLocation } from 'react-router-dom'

import { login, fetchMyProviders, setExecutiveContext } from '../services/authService'
import LoginWithCDB from '../components/auth/LoginWithCDB.jsx'

export default function Login() {
  const AUTH_MODE = (import.meta?.env?.VITE_AUTH_MODE || 'oauth2').toLowerCase()
  const navigate = useNavigate()
  const location = useLocation()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [isLoading, setIsLoading] = useState(false)
  const [showPassword, setShowPassword] = useState(false)
  const [error, setError] = useState('')
  const [info, setInfo] = useState('')
  const [providerOptions, setProviderOptions] = useState([])
  const [showProviderModal, setShowProviderModal] = useState(false)
  const [selecting, setSelecting] = useState(false)

  useEffect(() => {
    // Prefill email if coming from signup
    const state = location?.state
    if (state?.registered && state?.email) {
      setEmail(state.email)
      setInfo('Registration successful. Please sign in to continue.')
      // clear state so refresh doesn't keep message
      navigate(location.pathname, { replace: true })
    }
  }, [])

  const submit = async (e) => {
    e.preventDefault()
    setError('')
    setIsLoading(true)
    try {
      await login(email, password)
      // After login, fetch provider options
      const options = await fetchMyProviders()
      if (Array.isArray(options) && options.length > 1) {
        setProviderOptions(options)
        setShowProviderModal(true)
      } else if (Array.isArray(options) && options.length === 1) {
        // Auto select single option
        setSelecting(true)
        try {
          await setExecutiveContext(options[0].providerCode)
          // Refresh auth state before navigation
          if (window.refreshAuth) window.refreshAuth()
          navigate('/dashboard', { replace: true })
        } finally {
          setSelecting(false)
        }
      } else {
        navigate('/no-provider', { replace: true })
      }
    } catch (err) {
      setError(err.message || 'Login failed')
    } finally {
      setIsLoading(false)
    }
  }

  if (AUTH_MODE !== 'direct') {
    return (
      <div className="login-container">
        <div className="login-card">
          <div className="login-header">
            <div className="login-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor">
                <path d="M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4"/>
                <polyline points="10,17 15,12 10,7"/>
                <line x1="15" y1="12" x2="3" y2="12"/>
              </svg>
            </div>
            <h1>Welcome</h1>
            <p>Sign in with your CDB account to continue</p>
          </div>
          <div style={{marginTop:'16px'}}>
            <LoginWithCDB />
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="login-container">
      <div className="login-card">
        <div className="login-header">
          <div className="login-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor">
              <path d="M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4"/>
              <polyline points="10,17 15,12 10,7"/>
              <line x1="15" y1="12" x2="3" y2="12"/>
            </svg>
          </div>
          <h1>Welcome Back</h1>
          <p>Sign in to your CDB Provider account</p>
          {info && (<div style={{marginTop:'8px', color:'#2563eb', fontSize:'0.95rem'}}>{info}</div>)}
          {error && (<div style={{marginTop:'8px', color:'#dc2626', fontSize:'0.95rem'}}>{error}</div>)}
        </div>

        <form className="login-form" onSubmit={submit}>
          <div className="form-group">
            <label className="form-label">
              Email Address
              <input 
                type="email" 
                className="form-input"
                value={email} 
                onChange={(e) => setEmail(e.target.value)} 
                placeholder="Enter your email"
                required 
              />
            </label>
          </div>

          <div className="form-group">
            <label className="form-label">
              Password
              <div className="input-wrapper">
                <input 
                  type={showPassword ? 'text' : 'password'} 
                  className="form-input"
                  value={password} 
                  onChange={(e) => setPassword(e.target.value)} 
                  placeholder="Enter your password"
                  required 
                />
                <button 
                  type="button" 
                  className="password-toggle"
                  onClick={() => setShowPassword(!showPassword)}
                >
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor">
                    {showPassword ? (
                      <>
                        <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"/>
                        <line x1="1" y1="1" x2="23" y2="23"/>
                      </>
                    ) : (
                      <>
                        <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                        <circle cx="12" cy="12" r="3"/>
                      </>
                    )}
                  </svg>
                </button>
              </div>
            </label>
          </div>



          <button type="submit" className="login-btn" disabled={isLoading}>
            {isLoading ? (
              <>
                <svg className="btn-spinner" viewBox="0 0 24 24">
                  <circle cx="12" cy="12" r="10" fill="none" stroke="currentColor" strokeWidth="4" opacity="0.25"/>
                  <path fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" opacity="0.75"/>
                </svg>
                Signing in...
              </>
            ) : (
              'Sign In'
            )}
          </button>
        </form>

        <div className="login-footer">
          <p>Don't have an account? <Link to="/signup" className="signup-link">Sign up</Link></p>
        </div>
      </div>

      {showProviderModal && (
        <div style={modalStyles.backdrop}>
          <div style={modalStyles.modal} role="dialog" aria-modal="true">
            <h3 style={{marginTop:0}}>Choose Provider Context</h3>
            <p style={{marginTop: '4px', color:'#4b5563'}}>Select which provider context you want to use for this session.</p>
            <div style={{marginTop:'12px', display:'grid', gap:'8px'}}>
              {providerOptions.map((opt, idx) => (
                <button key={idx}
                        disabled={selecting}
                        style={modalStyles.optionBtn}
                        onClick={async () => {
                          setSelecting(true)
                          try {
                            await setExecutiveContext(opt.providerCode)
                            setShowProviderModal(false)
                            // Refresh auth state before navigation
                            if (window.refreshAuth) window.refreshAuth()
                            navigate('/dashboard', { replace: true })
                          } catch (e) {
                            setError(e.message || 'Failed to set provider context')
                          } finally {
                            setSelecting(false)
                          }
                        }}>
                  <span style={{fontWeight:600}}>{opt.providerCode}</span>
                  <span style={{color:'#6b7280'}}> (ID: {opt.providerId ?? '—'})</span>
                </button>
              ))}
            </div>
            <div style={{marginTop:'12px', display:'flex', justifyContent:'flex-end', gap:'8px'}}>
              <button className="btn" onClick={() => setShowProviderModal(false)} disabled={selecting}>Cancel</button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

const modalStyles = {
  backdrop: {
    position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.35)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000
  },
  modal: {
    background: '#fff', padding: '16px', borderRadius: '8px', width: 'min(480px, 96vw)', boxShadow: '0 10px 25px rgba(0,0,0,0.2)'
  },
  optionBtn: {
    textAlign: 'left', border: '1px solid #e5e7eb', borderRadius: '8px', padding: '10px 12px', background:'#f9fafb', cursor:'pointer'
  }
}
