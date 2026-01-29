export default function PlaceholderPage({ title, description }: { title: string; description?: string }) {
  return (
    <div>
      <h1 className="text-2xl font-semibold text-gray-900">{title}</h1>
      {description && <p className="mt-2 text-gray-600 max-w-3xl">{description}</p>}
      <div className="mt-6 p-4 rounded-lg border bg-white shadow-sm">
        <p className="text-sm text-gray-500">This section is coming soon. In the meantime, explore our API Marketplace or check back later for updates.</p>
      </div>
    </div>
  )
}
