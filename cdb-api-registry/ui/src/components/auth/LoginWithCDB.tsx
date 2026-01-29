import {useState} from 'react'
import {generateCodeChallenge, generateCodeVerifier} from '@services/pkce'

interface LoginWithCDBProps {
    className?: string
    onSuccess?: (token: string) => void
    onError?: (error: string) => void
}

export default function LoginWithCDB({className = '', onSuccess, onError}: LoginWithCDBProps) {
    const [isLoading, setIsLoading] = useState(false)

    const handleLogin = async () => {
        console.log('handleLogin')
        setIsLoading(true)

        try {
            // Generate PKCE parameters
            const codeVerifier = generateCodeVerifier()
            const codeChallenge = await generateCodeChallenge(codeVerifier)

            // Persist verifier and state in storage for callback verification
            sessionStorage.setItem('oauth2_code_verifier', codeVerifier)

            // Compute redirect URI with support for BASE_URL and explicit override
            const explicitRedirect = import.meta.env.VITE_PUBLIC_REDIRECT_URI as string | undefined
            const base = (import.meta.env.BASE_URL as string | undefined) || '/'
            const baseNormalized = base.endsWith('/') ? base.slice(0, -1) : base
            const fallbackRedirect = `${window.location.origin}${baseNormalized || ''}/auth/callback`
            const redirectUri = (explicitRedirect && explicitRedirect.trim().length > 0) ? explicitRedirect : fallbackRedirect

            // Generate state and persist
            const state = Math.random().toString(36).substring(2)
            sessionStorage.setItem('oauth2_state', state)

            // OAuth2 parameters
            const params = new URLSearchParams({
                client_id: import.meta.env.VITE_OAUTH_CLIENT_ID || 'cdb_api_registry',
                redirect_uri: redirectUri,
                response_type: 'code',
                scope: 'read write',
                code_challenge: codeChallenge,
                code_challenge_method: 'S256',
                state
            })

            // Redirect to CDB auth server
            const authBase = import.meta.env.VITE_CDB_AUTH_URL || 'http://localhost:8083'
            const authUrl = `${authBase}/oauth2/authorize?${params.toString()}`
            window.location.assign(authUrl)

        } catch (error) {
            setIsLoading(false)
            onError?.(error instanceof Error ? error.message : 'Login failed')
        }
    }

    return (
        <button
            onClick={handleLogin}
            disabled={isLoading}
            className={`
        inline-flex items-center justify-center px-6 py-3 
        bg-gradient-to-r from-blue-600 to-purple-600 
        hover:from-blue-700 hover:to-purple-700
        text-white font-semibold rounded-lg
        transition-all duration-200 transform hover:scale-105
        disabled:opacity-50 disabled:cursor-not-allowed disabled:transform-none
        shadow-lg hover:shadow-xl
        ${className}
      `}
        >
            {isLoading ? (
                <>
                    <svg className="animate-spin -ml-1 mr-3 h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg"
                         fill="none" viewBox="0 0 24 24">
                        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor"
                                strokeWidth="4"></circle>
                        <path className="opacity-75" fill="currentColor"
                              d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                    </svg>
                    Connecting...
                </>
            ) : (
                <>
                    <svg className="w-5 h-5 mr-2" viewBox="0 0 24 24" fill="currentColor">
                        <path
                            d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"/>
                    </svg>
                    Login with CDB
                </>
            )}
        </button>
    )
}