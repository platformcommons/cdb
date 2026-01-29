import { useEffect, useMemo, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { fetchApiById } from '@services/apiService'
import ReactMarkdown from 'react-markdown'
import remarkGfm from 'remark-gfm'
import StatusBadge from '@components/api/StatusBadge'
import TagChip from '@components/api/TagChip'
import ScalarApiTester from '@components/api/ScalarApiTester'

export default function ApiDetailPage({ defaultTab }: { defaultTab: 'docs' | 'try' }) {
  const { apiId } = useParams()
  const [tab, setTab] = useState<'docs' | 'try'>(defaultTab)
  const { data: api } = useQuery({ queryKey: ['api', apiId], queryFn: () => fetchApiById(apiId!), enabled: !!apiId })

  useEffect(() => setTab(defaultTab), [defaultTab])

  const breadcrumbs = useMemo(() => (
    <nav className="text-sm text-gray-600">
      <Link className="hover:text-gray-900" to="/">Marketplace</Link>
      <span className="mx-1">/</span>
      <span className="text-gray-900">{api?.name || 'API'}</span>
    </nav>
  ), [api])

  if (!api) return <div className="container-responsive py-6">Loading...</div>

  return (
    <div className="container-responsive py-6">
      <div className="mb-4">{breadcrumbs}</div>

      <div className="flex items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold text-gray-900">{api.name}</h1>
          <div className="mt-1 text-sm text-gray-600">by {api.owner} • v{api.version}</div>
          <div className="mt-2 flex items-center gap-2">
            <StatusBadge status={api.status} />
            {api.tags?.map(t => <TagChip key={t} tag={t} />)}
          </div>
        </div>
        <div className="text-sm text-gray-600">Base path: <span className="font-mono text-gray-900">{api.basePath}</span></div>
      </div>

      <div className="mt-6 border-b">
        <nav className="-mb-px flex gap-4">
          {['docs','try'].map(t => (
            <button key={t} onClick={() => setTab(t as any)} className={`border-b-2 px-3 py-2 text-sm font-medium ${tab === t ? 'border-brand text-brand' : 'border-transparent text-gray-600 hover:text-gray-900'}`}>
              {t === 'docs' ? 'Documentation' : 'Try It Out'}
            </button>
          ))}
        </nav>
      </div>

      {tab === 'docs' ? (
        <article className="prose max-w-none prose-headings:scroll-mt-24">
          <ReactMarkdown remarkPlugins={[remarkGfm]}>{api.detailedDescription}</ReactMarkdown>
        </article>
      ) : (
        <div className="mt-4 rounded-lg border bg-white p-4">
          <ScalarApiTester spec={api.openApiSpec} />
        </div>
      )}
    </div>
  )
}
