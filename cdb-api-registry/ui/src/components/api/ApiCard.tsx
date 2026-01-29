import { Link } from 'react-router-dom'
import { Api } from '@types/api'
import StatusBadge from './StatusBadge'
import TagChip from './TagChip'
import DomainChip from './DomainChip'

export default function ApiCard({ api }: { api: Api }) {
  return (
      <Link to={`/api/${api.id}`}
            className="block rounded-lg border bg-white p-4 shadow-sm hover:shadow-md transition-shadow no-underline">
          <div className="flex flex-col">
              <div className="flex items-start justify-between">
                  <div>
                      <div className="text-lg font-semibold text-gray-900 hover:text-brand">
                          {api.name}
                      </div>
                      <div className="text-sm text-gray-600">by {api.owner}</div>
                  </div>
                  <StatusBadge status={api.status}/>
              </div>

              <p className="mt-2 text-sm text-gray-700 line-clamp-2">{api.description}</p>

              <div className="mt-3 flex items-center justify-between text-xs text-gray-500">
                  <span>v{api.version}</span>
                  <span>{api.basePath}</span>
              </div>

              {api.domains?.length > 0 && (
                  <div className="mt-3 flex flex-wrap gap-2">
                      {api.domains.slice(0, 3).map(domain => (
                          <DomainChip key={domain} domain={domain}/>
                      ))}
                  </div>
              )}

              {api.tags?.length > 0 && (
                  <div className="mt-2 flex flex-wrap gap-2">
                      {api.tags.slice(0, 5).map(t => (
                          <TagChip key={t} tag={t}/>
                      ))}
                  </div>
              )}
          </div>
      </Link>
  )
}
