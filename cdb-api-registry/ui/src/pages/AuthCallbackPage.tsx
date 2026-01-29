import { useEffect, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'

function decodeJwt(token?: string | null): any | null {
  if (!token) return null
  try {
    const parts = token.split('.')
    if (parts.length < 2) return null
    const json = atob(parts[1].replace(/-/g, '+').replace(/_/g, '/'))
    return JSON.parse(decodeURIComponent(escape(json)))
  } catch {
    return null
  }
}

export default function AuthCallbackPage() {
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const [status, setStatus] = useState<'loading' | 'success' | 'error'>('loading')
  const [error, setError] = useState<string>('')

  useEffect(() => {
    const handleCallback = async () => {
      try {
        const code = searchParams.get('code')
        const state = searchParams.get('state')
        const storedState = sessionStorage.getItem('oauth2_state')
        const codeVerifier = sessionStorage.getItem('oauth2_code_verifier')

        if (!code) {
          throw new Error('Authorization code not received')
        }

        if (state !== storedState) {
          throw new Error('Invalid state parameter')
        }

        if (!codeVerifier) {
          throw new Error('Code verifier not found')
        }

        // Exchange code for token
        const tokenResponse = await fetch(`${import.meta.env.VITE_CDB_AUTH_URL || 'http://localhost:8083'}/oauth2/token`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
          },
          body: new URLSearchParams({
            grant_type: 'authorization_code',
            code,
            client_id: import.meta.env.VITE_OAUTH_CLIENT_ID || 'cdb_api_registry',
            code_verifier: codeVerifier,
          }),
        })

        if (!tokenResponse.ok) {
          let msg = 'Token exchange failed'
          try {
            const errorData = await tokenResponse.json()
            msg = errorData.error_description || errorData.error || msg
          } catch {}
          throw new Error(msg)
        }

        const data = await tokenResponse.json()

        // Normalize token response keys
        const accessToken: string | undefined = data?.access_token || data?.accessToken
        const refreshToken: string | undefined = data?.refresh_token || data?.refreshToken
        const expiresIn: number | undefined = data?.expires_in || data?.expiresIn

        if (accessToken) {
          localStorage.setItem('cdb_access_token', accessToken)
        }


        // Derive and persist display name and user markers in sessionStorage
        const claims = decodeJwt(accessToken) || {}
        const email = claims?.sub || claims?.email || ''
        const userId = claims?.userId || claims?.uid || ''
        const ctxUser = claims?.ctx?.user || {}
        const displayName = ctxUser?.displayName || ctxUser?.name || [ctxUser?.firstName, ctxUser?.lastName].filter(Boolean).join(' ') || email
        try {
          sessionStorage.setItem('cdb.user_email', email)
          sessionStorage.setItem('cdb.user_id', String(userId))
          sessionStorage.setItem('cdb.user', JSON.stringify({ username: displayName || email }))
        } catch {}

        // Clean up session storage
        sessionStorage.removeItem('oauth2_state')
        sessionStorage.removeItem('oauth2_code_verifier')

        setStatus('success')

        // Allow any listeners (e.g., header) to refresh auth state immediately
        if (typeof window !== 'undefined' && (window as any).refreshAuth) {
          ;(window as any).refreshAuth()
        }

        // Redirect to home page after success
        setTimeout(() => {
          navigate('/', { replace: true })
        }, 500)

      } catch (err) {
        setError(err instanceof Error ? err.message : 'Authentication failed')
        setStatus('error')
      }
    }

    handleCallback()
  }, [searchParams, navigate])

  if (status === 'loading') {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto mb-4"></div>
          <h2 className="text-xl font-semibold text-gray-900">Completing login...</h2>
          <p className="text-gray-600 mt-2">Please wait while we authenticate you</p>
        </div>
      </div>
    )
  }

  if (status === 'success') {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-center">
          <div className="w-12 h-12 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-4">
            <svg className="w-6 h-6 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M5 13l4 4L19 7"></path>
            </svg>
          </div>
          <h2 className="text-xl font-semibold text-gray-900">Login successful!</h2>
          <p className="text-gray-600 mt-2">Redirecting you to the application...</p>
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen flex items-center justify-center">
      <div className="text-center">
        <div className="w-12 h-12 bg-red-100 rounded-full flex items-center justify-center mx-auto mb-4">
          <svg className="w-6 h-6 text-red-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12"></path>
          </svg>
        </div>
        <h2 className="text-xl font-semibold text-gray-900">Authentication failed</h2>
        <p className="text-gray-600 mt-2">{error}</p>
        <button
          onClick={() => navigate('/', { replace: true })}
          className="mt-4 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700"
        >
          Return to Home
        </button>
      </div>
    </div>
  )
}