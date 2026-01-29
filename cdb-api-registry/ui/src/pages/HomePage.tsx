import { useQuery } from '@tanstack/react-query'
import { useState, useMemo } from 'react'
import { Link } from 'react-router-dom'
import { fetchApis } from '@services/apiService'
import ApiCard from '@components/api/ApiCard'
import LoadingSpinner from '@components/common/LoadingSpinner'
import { PAGE_SIZE } from '@constants/const'
import Pagination from '@components/common/Pagination'

export default function HomePage() {
  const [page, setPage] = useState(0)
  const queryKey = useMemo(() => ['homepage-apis', page], [page])
  const { data, isLoading } = useQuery({
    queryKey,
    queryFn: () => fetchApis({ 
      page, 
      size: PAGE_SIZE, 
      search: '', 
      status: 'PUBLISHED'
    })
  })

  return (
    <div className="container-responsive py-6">
      <div className="mb-8 text-center">
        <h1 className="text-3xl font-bold text-gray-900 mb-4">API Registry</h1>
        <p className="text-lg text-gray-600 max-w-2xl mx-auto">
          Discover, explore, and integrate with published APIs across the platform
        </p>
      </div>

      <div className="mb-6 flex items-center justify-between">
        <h2 className="text-xl font-semibold text-gray-900">Published APIs</h2>
        <Link 
          to="/search" 
          className="text-brand hover:text-brand-dark font-medium"
        >
          View all →
        </Link>
      </div>

      {isLoading && (
        <div className="p-6">
          <LoadingSpinner label="Loading APIs..." />
        </div>
      )}

      <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
        {data?.content?.map(api => (
          <ApiCard key={api.id} api={api} />
        ))}
      </div>

      {/* Pagination */}
      {!isLoading && (data?.totalPages ?? 0) > 1 && (
        <Pagination
          page={page}
          totalPages={data?.totalPages ?? 1}
          onChange={(p) => setPage(p)}
        />
      )}

      {!isLoading && data?.content?.length === 0 && (
        <div className="text-center py-12">
          <svg className="mx-auto h-12 w-12 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
          </svg>
          <h3 className="mt-2 text-sm font-medium text-gray-900">No APIs available</h3>
          <p className="mt-1 text-sm text-gray-500">Check back later for published APIs</p>
        </div>
      )}
    </div>
  )
}