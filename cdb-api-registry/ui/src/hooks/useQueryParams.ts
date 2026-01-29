import { useCallback, useMemo } from 'react'
import { useLocation, useSearchParams } from 'react-router-dom'

export function useQueryParams() {
  const [params, setParams] = useSearchParams()
  const location = useLocation()

  const values = useMemo(() => ({
    q: params.get('q') || '',
    tags: params.get('tags')?.split(',').filter(Boolean) || [],
    domains: params.get('domains')?.split(',').filter(Boolean) || [],
    owners: params.get('owners')?.split(',').filter(Boolean) || [],
    page: Number(params.get('page') || '0')
  }), [params])

  // Stable updater; builds from current location to avoid stale params closure
  const update = useCallback((patch: Record<string, string | string[] | number | undefined>) => {
    const next = new URLSearchParams(window.location.search)
    Object.entries(patch).forEach(([k, v]) => {
      if (v === undefined || (Array.isArray(v) && v.length === 0) || v === '') {
        next.delete(k)
      } else if (Array.isArray(v)) {
        next.set(k, v.join(','))
      } else if (typeof v === 'number') {
        next.set(k, String(v))
      } else {
        next.set(k, v)
      }
    })
    // No-op if unchanged to prevent unnecessary updates
    if (next.toString() === location.search.slice(1)) return
    setParams(next, { replace: true })
  }, [setParams, location.search])

  return { params: values, setParams: update }
}
