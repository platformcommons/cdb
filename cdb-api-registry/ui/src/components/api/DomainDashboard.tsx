import { useState, useEffect } from 'react'
import { DomainStat, Api } from '@types/api'
import { fetchDomainStats, fetchApisByDomain } from '@services/apiService'
import ApiCard from './ApiCard'

export default function DomainDashboard() {
  const [domains, setDomains] = useState<DomainStat[]>([])
  const [selectedDomain, setSelectedDomain] = useState<string | null>(null)
  const [apis, setApis] = useState<Api[]>([])
  const [loading, setLoading] = useState(true)
  const [apisLoading, setApisLoading] = useState(false)

  useEffect(() => {
    loadDomains()
  }, [])

  const loadDomains = async () => {
    try {
      const response = await fetchDomainStats()
      setDomains(response.domains)
    } finally {
      setLoading(false)
    }
  }

  const handleDomainClick = async (domain: string) => {
    setSelectedDomain(domain)
    setApisLoading(true)
    try {
      const response = await fetchApisByDomain(domain)
      setApis(response.content)
    } finally {
      setApisLoading(false)
    }
  }

  const handleBack = () => {
    setSelectedDomain(null)
    setApis([])
  }

  if (loading) {
    return <div className="flex justify-center p-8">Loading domains...</div>
  }

  if (selectedDomain) {
    return (
      <div className="p-6">
        <div className="mb-6">
          <button
            onClick={handleBack}
            className="text-blue-600 hover:text-blue-800 mb-4"
          >
            ← Back to Domains
          </button>
          <h2 className="text-2xl font-bold">APIs in {selectedDomain}</h2>
        </div>

        {apisLoading ? (
          <div className="flex justify-center p-8">Loading APIs...</div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
            {apis.map((api) => (
              <ApiCard key={api.id} api={api} />
            ))}
            {apis.length === 0 && (
              <div className="text-center py-8 text-gray-500">
                No APIs found in this domain
              </div>
            )}
          </div>
        )}
      </div>
    )
  }

  return (
    <div className="p-6">
      <h2 className="text-2xl font-bold mb-6">API Domains</h2>
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {domains.map((domain) => (
          <div
            key={domain.domain}
            onClick={() => handleDomainClick(domain.domain)}
            className="border rounded-lg p-6 cursor-pointer hover:shadow-lg transition-shadow bg-white"
          >
            <h3 className="text-xl font-semibold mb-2 capitalize">{domain.domain}</h3>
            <p className="text-3xl font-bold text-blue-600">{domain.apiCount}</p>
            <p className="text-gray-600 text-sm">APIs available</p>
          </div>
        ))}
      </div>
      {domains.length === 0 && (
        <div className="text-center py-8 text-gray-500">
          No domains found
        </div>
      )}
    </div>
  )
}