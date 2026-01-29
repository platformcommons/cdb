import { ApiStatus } from '@types/api'

const colors: Record<ApiStatus, string> = {
  DRAFT: 'bg-gray-100 text-gray-700 border-gray-200',
  PUBLISHED: 'bg-green-100 text-green-700 border-green-200',
  DEPRECATED: 'bg-yellow-100 text-yellow-700 border-yellow-200',
  RETIRED: 'bg-red-100 text-red-700 border-red-200'
}

export default function StatusBadge({ status }: { status: ApiStatus }) {
  return (
    <span className={`inline-flex items-center rounded-md border px-2 py-0.5 text-xs font-medium ${colors[status]}`}>
      {status}
    </span>
  )
}
