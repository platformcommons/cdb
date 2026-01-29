import { Link, NavLink, useNavigate, useSearchParams } from 'react-router-dom'
import SearchBar from '@components/common/SearchBar'
import LoginWithCDB from '@components/auth/LoginWithCDB'
import { useState, useEffect } from 'react'

function getStoredUser(): { username?: string } | null {
  try {
    const raw = sessionStorage.getItem('cdb.user')
    if (!raw) return null
    return JSON.parse(raw)
  } catch {
    return null
  }
}

export default function Header({ onToggleSidebar, isSidebarOpen }: { onToggleSidebar?: () => void; isSidebarOpen?: boolean }) {
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const [isAuthenticated, setIsAuthenticated] = useState(false)
  const [user, setUser] = useState<{ username?: string } | null>(null)

  useEffect(() => {
    const refresh = () => {
      const token = localStorage.getItem('cdb_access_token')
      setIsAuthenticated(!!token)
      const u = getStoredUser()
      if (u) {
        setUser(u)
      } else if (token) {
        // Fallback to token sub
        try {
          const payload = JSON.parse(atob(token.split('.')[1]))
          setUser({ username: payload?.sub })
        } catch {
          setUser(null)
        }
      } else {
        setUser(null)
      }
    }

    // initial
    refresh()

    // expose global refresh hook for other parts of app
    ;(window as any).refreshAuth = refresh

    // storage changes across tabs/windows
    const onStorage = (e: StorageEvent) => {
      if (e.key === 'cdb.user' || e.key === 'cdb_access_token') {
        refresh()
      }
    }
    window.addEventListener('storage', onStorage)
    return () => window.removeEventListener('storage', onStorage)
  }, [])

  const handleLogout = () => {
    localStorage.removeItem('cdb_access_token')
    localStorage.removeItem('cdb_refresh_token')
    sessionStorage.removeItem('cdb.user')
    sessionStorage.removeItem('cdb.user_email')
    sessionStorage.removeItem('cdb.user_id')
    setIsAuthenticated(false)
    setUser(null)
    if ((window as any).refreshAuth) (window as any).refreshAuth()
  }

  const onSearch = (q: string) => {
    const tags = params.get('tags')
    const status = params.get('status')
    const url = new URLSearchParams()
    if (q) url.set('q', q)
    if (tags) url.set('tags', tags)
    if (status) url.set('status', status)
    navigate(`/search?${url.toString()}`)
  }

  return (
    <header className="border-b bg-white">
      <div className="max-w-100 mx-auto py-3 flex items-center gap-4 pr-4 sm:pr-6 lg:pr-8">
        {/* Sidebar toggle button - left aligned */}
        <button
          type="button"
          onClick={onToggleSidebar}
          aria-pressed={isSidebarOpen}
          aria-label={isSidebarOpen ? 'Collapse navigation' : 'Expand navigation'}
          className="inline-flex items-center justify-center rounded-md border border-gray-200 bg-white px-2.5 py-2 text-gray-600 hover:bg-gray-50 hover:text-gray-900 focus:outline-none focus-visible:ring-2 focus-visible:ring-brand/40 ml-4 sm:ml-6 lg:ml-8"
        >
          <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <line x1="3" y1="6" x2="21" y2="6" />
            <line x1="3" y1="12" x2="21" y2="12" />
            <line x1="3" y1="18" x2="21" y2="18" />
          </svg>
        </button>

        <Link to="/" className="text-xl font-bold text-brand">API Marketplace</Link>



        <div className="flex-1" />



        <div className="flex items-center gap-3">
          {isAuthenticated ? (
            <div className="flex items-center gap-3">
              <span className="text-sm text-gray-600">Welcome, {user?.username}</span>
              <button
                onClick={handleLogout}
                className="text-sm text-gray-600 hover:text-gray-900 px-3 py-1 rounded border border-gray-300 hover:border-gray-400"
              >
                Logout
              </button>
            </div>
          ) : (
            <LoginWithCDB className="text-sm px-4 py-2" />
          )}
        </div>
      </div>
    </header>
  )
}
