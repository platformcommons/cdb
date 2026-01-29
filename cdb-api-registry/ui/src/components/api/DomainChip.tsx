export default function DomainChip({ domain, onRemove }: { domain: string; onRemove?: () => void }) {
  return (
    <span className="inline-flex items-center gap-1 rounded-full bg-blue-50 text-blue-700 border border-blue-200 px-2 py-0.5 text-xs font-medium">
      <span>{domain}</span>
      {onRemove && (
        <button onClick={onRemove} aria-label={`Remove ${domain}`} className="hover:text-blue-800">×</button>
      )}
    </span>
  )
}