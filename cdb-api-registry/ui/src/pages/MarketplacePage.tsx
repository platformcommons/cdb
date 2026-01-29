import { useEffect, useMemo, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { fetchApis } from '@services/apiService'
import ApiCard from '@components/api/ApiCard'
import FilterSidebar, { FilterState } from '@components/api/FilterSidebar'
import SearchBar from '@components/api/SearchBar'
import LoadingSpinner from '@components/common/LoadingSpinner'
import { useQueryParams } from '@hooks/useQueryParams'
import { PAGE_SIZE } from '@constants/const'
import Pagination from '@components/common/Pagination'

export default function MarketplacePage() {
  const { params, setParams } = useQueryParams()
  const [filters, setFilters] = useState<FilterState>({ 
    query: params.q || '', 
    tags: params.tags || [], 
    domains: params.domains || [],
    owners: params.owners || []
  })
  const [showFilters, setShowFilters] = useState(false)
  const page = params.page ?? 0

  useEffect(() => {
    setParams({ 
      q: filters.query || '', 
      tags: filters.tags, 
      domains: filters.domains,
      owners: filters.owners
    })
  }, [filters, setParams])

  const queryKey = useMemo(() => ['apis', filters, page], [filters, page])
  const { data, isLoading } = useQuery({
    queryKey,
    queryFn: () => fetchApis({ 
      page, 
      size: PAGE_SIZE, 
      search: filters.query, 
      tags: filters.tags, 
      domains: filters.domains,
      owners: filters.owners
    })
  })

  const handleSearch = (query: string) => {
    setFilters(prev => ({ ...prev, query }))
    // Reset page explicitly when search text changes
    setParams({ page: 0 })
  }

  return (
    <div className="container-responsive py-6">
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-gray-900 mb-4">API Marketplace</h1>
        <div className="flex gap-4 items-center">
          <div className="flex-1">
            <SearchBar value={filters.query} onSearch={handleSearch} />
          </div>
          <button
            onClick={() => setShowFilters(!showFilters)}
            className="px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 flex items-center gap-2"
          >
            <svg className="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 4a1 1 0 011-1h16a1 1 0 011 1v2.586a1 1 0 01-.293.707l-6.414 6.414a1 1 0 00-.293.707V17l-4 4v-6.586a1 1 0 00-.293-.707L3.293 7.207A1 1 0 013 6.5V4z" />
            </svg>
            Filters
          </button>
        </div>
      </div>

      <div className={`grid gap-6 ${showFilters ? 'lg:grid-cols-4' : 'grid-cols-1'}`}>
        {showFilters && (
          <div className="lg:col-span-1">
            <div className="rounded-lg border bg-white p-4 sticky top-4">
              <FilterSidebar 
                filters={filters} 
                onChange={(next) => {
                  setFilters(next)
                  // Reset page explicitly when any filter changes
                  setParams({ page: 0 })
                }} 
              />
            </div>
          </div>
        )}
        <div className={showFilters ? 'lg:col-span-3' : 'col-span-1'}>
          <div className="mb-4 flex items-center justify-between">
            <div className="text-sm text-gray-600">
              {data?.totalElements ?? 0} APIs found
              {filters.query && ` for "${filters.query}"`}
              <span className="text-xs text-gray-500 ml-2"></span>
            </div>
          </div>

          {isLoading && (
            <div className="p-6"><LoadingSpinner label="Searching APIs..." /></div>
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
              onChange={(p) => setParams({ page: p })}
            />
          )}

          {!isLoading && data?.content?.length === 0 && (
            <div className="text-center py-12">
              <svg className="mx-auto h-12 w-12 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
              </svg>
              <h3 className="mt-2 text-sm font-medium text-gray-900">No APIs found</h3>
              <p className="mt-1 text-sm text-gray-500">Try adjusting your search or filters</p>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
