export default function TagChip({ tag, onRemove }: { tag: string; onRemove?: () => void }) {
  return (
    <span className="inline-flex items-center gap-1 rounded-full bg-brand/10 text-brand border border-brand/20 px-2 py-0.5 text-xs">
      <span>#{tag}</span>
      {onRemove && (
        <button onClick={onRemove} aria-label={`Remove ${tag}`} className="hover:text-brand-dark">×</button>
      )}
    </span>
  )
}
