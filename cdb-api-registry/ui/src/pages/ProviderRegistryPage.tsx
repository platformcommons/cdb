import { useEffect, useMemo, useState } from 'react'
import { providerRegistryService, Provider, ProviderKeySummary, buildEnvFileContent, downloadTextFile, extractTagsSet } from '@services/providerRegistryService'

function useUserEmail(): string | null {
  try {
    const email = sessionStorage.getItem('cdb.user_email')
    return email || null
  } catch {
    return null
  }
}

function getAccessToken(): string | null {
  try {
    return localStorage.getItem('cdb_access_token') || sessionStorage.getItem('cdb_access_token') || null
  } catch {
    return null
  }
}

function OverflowMenu({ onDownloadEnv, onRequestKey }: { onDownloadEnv: () => void; onRequestKey: () => void }) {
  const [open, setOpen] = useState(false)
  return (
    <div className="relative">
      <button className="p-2 rounded hover:bg-gray-100" onClick={() => setOpen(o => !o)} aria-label="More actions">
        <svg className="w-5 h-5 text-gray-600" fill="currentColor" viewBox="0 0 20 20">
          <path d="M6 10a2 2 0 114 0 2 2 0 01-4 0zm-6 0a2 2 0 114 0 2 2 0 01-4 0zm12 0a2 2 0 114 0 2 2 0 01-4 0z" />
        </svg>
      </button>
      {open && (
        <div className="absolute right-0 mt-2 w-56 bg-white border border-gray-200 rounded shadow-md z-10">
          <button onClick={() => { setOpen(false); onDownloadEnv() }} className="w-full text-left px-4 py-2 hover:bg-gray-50">Download public info (.env)</button>
          <button onClick={() => { setOpen(false); onRequestKey() }} className="w-full text-left px-4 py-2 hover:bg-gray-50">Request/Download symmetric key (.pem)</button>
        </div>
      )}
    </div>
  )
}

export default function ProviderRegistryPage() {
  const [query, setQuery] = useState('')
  const [tagFilter, setTagFilter] = useState<string>('')
  const [providers, setProviders] = useState<Provider[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string>('')
  const [page, setPage] = useState(0)
  const pageSize = 10
  const email = useUserEmail()
  const [message, setMessage] = useState<string>('')
  const [accessToken, setAccessToken] = useState<string | null>(getAccessToken())

  // Listen for auth changes (in case login happens in another tab or flow updates token)
  useEffect(() => {
    const handler = () => setAccessToken(getAccessToken())
    window.addEventListener('storage', handler)
    // Support immediate refresh hook if app sets window.refreshAuth
    ;(window as any).refreshAuth = handler
    return () => {
      window.removeEventListener('storage', handler)
      if ((window as any).refreshAuth === handler) delete (window as any).refreshAuth
    }
  }, [])

  useEffect(() => {
    if (!accessToken) return
    const load = async () => {
      setLoading(true)
      setError('')
      try {
        const data = await providerRegistryService.searchProviders({ q: query || undefined, tag: tagFilter || undefined })
        setProviders(Array.isArray(data) ? data : [])
        setPage(0)
      } catch (e: any) {
        setError(e?.message || 'Failed to load providers')
      } finally {
        setLoading(false)
      }
    }
    // Debounce-like simple delay
    const t = setTimeout(load, 300)
    return () => clearTimeout(t)
  }, [query, tagFilter, accessToken])

  const allTags = useMemo(() => extractTagsSet(providers), [providers])

  const pagedProviders = useMemo(() => {
    const start = page * pageSize
    return providers.slice(start, start + pageSize)
  }, [providers, page])

  const totalPages = Math.ceil((providers.length || 0) / pageSize)

  const handleDownloadEnv = async (p: Provider) => {
    try {
      // Try to pick a public key from keys list (ACTIVE SIGNING preferred, else any ACTIVE)
      const keys: ProviderKeySummary[] = await providerRegistryService.listProviderKeys(p.id)
      const preferred = keys.find(k => k.keyStatus === 'ACTIVE' && k.keyType === 'SIGNING') || keys.find(k => k.keyStatus === 'ACTIVE') || keys[0]
      const publicKeyPem = preferred?.publicKeyPem || ''

      // Fetch provider environments to fill base URLs
      const envs = await providerRegistryService.listProviderEnvironments(p.id)
      const prodBaseUrl = envs.find(e => e.environmentType === 'PRODUCTION')?.baseUrl || ''
      // Map STAGING to SANDBOX for .env naming
      const sandboxBaseUrl = envs.find(e => e.environmentType === 'SANDBOX')?.baseUrl || ''

      const content = buildEnvFileContent(p, publicKeyPem, prodBaseUrl, sandboxBaseUrl)
      downloadTextFile(`${p.code || 'provider'}-public.env`, content)
      setMessage('Public info downloaded successfully')
    } catch (e: any) {
      setMessage(e?.message || 'Failed to prepare .env file')
    }
  }

  const handleRequestKey = async (p: Provider) => {
    try {
      const clientId = email || prompt('Enter your client email to bind this key (client_id):') || ''
      if (!clientId) return
      // Check if a key already exists for this client
      const keys = await providerRegistryService.listProviderKeys(p.id)
      const existing = keys.find(k => k.clientId === clientId && k.keyType === 'ENCRYPTION')
      if (existing) {
        // If private key isn't available (usually not returned), just inform status
        setMessage(`Key already exists for ${clientId}. Status: ${existing.keyStatus}. Downloading latest copy if available...`)
        // Try to download private if somehow present (edge case)
        // We do not have endpoint to fetch private by keyId (by design). So we just download public part as .pem for now.
        if (existing.publicKeyPem) {
          downloadTextFile(`${p.code}-${clientId}-public.pem`, existing.publicKeyPem)
        }
        return
      }

      const resp = await providerRegistryService.generateSymmetricKey(p.id, clientId)
      if (resp.keyStatus === 'PENDING_FOR_APPROVAL') {
        setMessage('Key request submitted. Status: PENDING_FOR_APPROVAL. You will be able to download once approved.')
      }
      if (resp.privateKeyPem) {
        downloadTextFile(`${p.code}-${clientId}.pem`, resp.privateKeyPem)
        setMessage('Symmetric key generated and downloaded successfully')
      }
    } catch (e: any) {
      setMessage(e?.message || 'Failed to request/download key')
    }
  }

  if (!accessToken) {
    return (
      <div className="p-6">
        <div className="mb-6">
          <h1 className="text-2xl font-semibold text-gray-900">Provider Registry</h1>
          <p className="text-gray-600">This page requires you to log in.</p>
        </div>
        <div className="p-4 border rounded bg-yellow-50 text-yellow-900 border-yellow-200">
          You must be authenticated to view providers. Please log in and return to this page.
        </div>
      </div>
    )
  }

  return (
    <div className="p-6">
      <div className="mb-6">
        <h1 className="text-2xl font-semibold text-gray-900">Provider Registry</h1>
        <p className="text-gray-600">Discover registered providers and download their public info.</p>
      </div>

      <div className="flex flex-col md:flex-row gap-3 mb-4">
        <input
          type="text"
          value={query}
          onChange={e => setQuery(e.target.value)}
          placeholder="Search by provider name, code, or id"
          className="flex-1 border rounded px-3 py-2"
        />
        <div className="flex items-center gap-2">
          <select value={tagFilter} onChange={e => setTagFilter(e.target.value)} className="border rounded px-3 py-2 min-w-[200px]">
            <option value="">All tags</option>
            {allTags.map(t => (
              <option key={t} value={t}>{t}</option>
            ))}
          </select>
          <button onClick={() => { setQuery(''); setTagFilter('') }} className="px-3 py-2 border rounded hover:bg-gray-50">Clear</button>
        </div>
      </div>

      {message && (
        <div className="mb-4 p-3 rounded bg-blue-50 text-blue-800 border border-blue-200">{message}</div>
      )}

      {error && (
        <div className="mb-4 p-3 rounded bg-red-50 text-red-800 border border-red-200">{error}</div>
      )}

      {loading ? (
        <div className="py-12 text-center text-gray-600">Loading providers...</div>
      ) : (
        <>
          {providers.length === 0 ? (
            <div className="py-12 text-center text-gray-600">No providers found.</div>
          ) : (
            <>
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                {pagedProviders.map(p => (
                  <div key={p.id} className="border rounded-lg p-4 bg-white shadow-sm flex flex-col">
                    <div className="flex items-start justify-between">
                      <div>
                        <h3 className="text-lg font-medium text-gray-900">{p.name}</h3>
                        <p className="text-sm text-gray-500">Code: {p.code} • ID: {p.id}</p>
                      </div>
                      <OverflowMenu onDownloadEnv={() => handleDownloadEnv(p)} onRequestKey={() => handleRequestKey(p)} />
                    </div>
                    {p.description && (
                      <p className="text-gray-700 text-sm mt-2 line-clamp-3">{p.description}</p>
                    )}
                    {(p.tags && p.tags.split(',').filter(Boolean).length > 0) && (
                      <div className="flex flex-wrap gap-1 mt-3">
                        {p.tags.split(',').map(t => t.trim()).filter(Boolean).map(t => (
                          <span key={t} className="text-xs bg-gray-100 text-gray-700 px-2 py-1 rounded">{t}</span>
                        ))}
                      </div>
                    )}
                  </div>
                ))}
              </div>

              <div className="flex items-center justify-between mt-6">
                <div className="text-sm text-gray-600">Page {page + 1} of {Math.max(totalPages, 1)}</div>
                <div className="flex gap-2">
                  <button disabled={page === 0} onClick={() => setPage(p => Math.max(0, p - 1))} className="px-3 py-1 border rounded disabled:opacity-50">Previous</button>
                  <button disabled={page + 1 >= totalPages} onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))} className="px-3 py-1 border rounded disabled:opacity-50">Next</button>
                </div>
              </div>
            </>
          )}
        </>
      )}
    </div>
  )
}
