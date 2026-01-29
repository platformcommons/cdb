export default function LoadingSpinner({ label = 'Loading...' }: { label?: string }) {
  return (
    <div role="status" className="flex items-center gap-2 text-sm text-gray-600">
      <svg className="h-5 w-5 animate-spin text-brand" viewBox="0 0 24 24" fill="none">
        <circle cx="12" cy="12" r="10" stroke="currentColor" strokeOpacity=".25" strokeWidth="4" />
        <path d="M22 12a10 10 0 0 1-10 10" stroke="currentColor" strokeWidth="4" strokeLinecap="round" />
      </svg>
      <span>{label}</span>
    </div>
  )
}
