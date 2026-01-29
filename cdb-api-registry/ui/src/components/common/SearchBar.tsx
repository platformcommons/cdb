import { useEffect, useMemo, useState } from 'react'

export default function SearchBar({ onSearch }: { onSearch: (q: string) => void }) {
  const [value, setValue] = useState('')
  const [debounced, setDebounced] = useState(value)

  useEffect(() => {
    const t = setTimeout(() => setDebounced(value), 300)
    return () => clearTimeout(t)
  }, [value])

  useEffect(() => {
    onSearch(debounced)
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [debounced])

  const placeholder = useMemo(() => 'Search by name, owner, description…', [])

  return (
    <div className="relative">
      <input
        value={value}
        onChange={e => setValue(e.target.value)}
        type="search"
        aria-label="Search APIs"
        placeholder={placeholder}
        className="w-full rounded-md border border-gray-300 px-3 py-2 pr-8 text-sm focus:outline-none focus:ring-2 focus:ring-brand/30 focus:border-brand"
      />
      <svg className="absolute right-2 top-2.5 h-4 w-4 text-gray-400" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
        <circle cx="11" cy="11" r="8" />
        <path d="m21 21-4.3-4.3" />
      </svg>
    </div>
  )
}
