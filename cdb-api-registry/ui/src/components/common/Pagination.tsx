import React from 'react'

type Props = {
  page: number
  totalPages: number
  onChange: (page: number) => void
  className?: string
}

// Simple, theme-coherent pagination control using existing utility classes
export default function Pagination({ page, totalPages, onChange, className }: Props) {
  if (totalPages <= 1) return null

  const canPrev = page > 0
  const canNext = page < totalPages - 1

  // Generate a compact window of page numbers
  const pages: number[] = []
  const window = 2
  const start = Math.max(0, page - window)
  const end = Math.min(totalPages - 1, page + window)
  for (let i = start; i <= end; i++) pages.push(i)

  return (
    <nav className={"mt-6 flex items-center justify-center gap-2 " + (className || '')} aria-label="Pagination">
      <button
        type="button"
        className={`px-3 py-2 rounded-md border text-sm ${canPrev ? 'bg-white text-gray-700 hover:bg-gray-50 border-gray-300' : 'bg-gray-100 text-gray-400 border-gray-200 cursor-not-allowed'}`}
        onClick={() => canPrev && onChange(page - 1)}
        disabled={!canPrev}
        aria-label="Previous page"
      >
        ← Prev
      </button>

      {start > 0 && (
        <>
          <button
            type="button"
            className={`px-3 py-2 rounded-md border text-sm bg-white text-gray-700 hover:bg-gray-50 border-gray-300`}
            onClick={() => onChange(0)}
            aria-label="Page 1"
          >
            1
          </button>
          {start > 1 && <span className="px-2 text-gray-400">…</span>}
        </>
      )}

      {pages.map(p => (
        <button
          key={p}
          type="button"
          className={`px-3 py-2 rounded-md border text-sm ${p === page ? 'bg-brand text-white border-brand' : 'bg-white text-gray-700 hover:bg-gray-50 border-gray-300'}`}
          onClick={() => onChange(p)}
          aria-current={p === page ? 'page' : undefined}
          aria-label={`Page ${p + 1}`}
        >
          {p + 1}
        </button>
      ))}

      {end < totalPages - 1 && (
        <>
          {end < totalPages - 2 && <span className="px-2 text-gray-400">…</span>}
          <button
            type="button"
            className={`px-3 py-2 rounded-md border text-sm bg-white text-gray-700 hover:bg-gray-50 border-gray-300`}
            onClick={() => onChange(totalPages - 1)}
            aria-label={`Page ${totalPages}`}
          >
            {totalPages}
          </button>
        </>
      )}

      <button
        type="button"
        className={`px-3 py-2 rounded-md border text-sm ${canNext ? 'bg-white text-gray-700 hover:bg-gray-50 border-gray-300' : 'bg-gray-100 text-gray-400 border-gray-200 cursor-not-allowed'}`}
        onClick={() => canNext && onChange(page + 1)}
        disabled={!canNext}
        aria-label="Next page"
      >
        Next →
      </button>
    </nav>
  )
}
