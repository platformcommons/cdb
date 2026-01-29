import { Api, ApiListResponse, DomainStatsResponse } from '@types/api'
import { apiClient } from './apiClient'
const SERVICE_PREFIX = '/cdb-api-registry'

export async function fetchApis(params: { page?: number; size?: number; search?: string; tags?: string[]; domains?: string[]; owners?: string[]; sort?: string; createdByProvider?: string | number }): Promise<ApiListResponse> {
  try {
    const hasFilters = params.search || params.tags?.length || params.domains?.length || params.owners?.length
    
    let res
    if (hasFilters) {
      // Use search endpoint when filters are applied
      res = await apiClient.get(`${SERVICE_PREFIX}/api/v1/api-registry/discovery/search`, {
        params: {
          query: params.search,
          tags: params.tags,
          domains: params.domains,

          owners: params.owners,
          page: params.page ?? 0,
          size: params.size ?? 20
        }
      })
    } else {
      // Use regular listing endpoint
      res = await apiClient.get(`${SERVICE_PREFIX}/api/v1/api-registry/discovery/apis`, {
        params: {
          page: params.page ?? 0,
          size: params.size ?? 20,
          sort: params.sort,

          owners: params.owners,
          createdByProvider: params.createdByProvider
        }
      })
    }
    
    const d = res.data?.data as any
    const content: Api[] = (d?.apis || []).map((a: any) => ({
      id: String(a.id),
      name: a.name,
      owner: a.owner,
      description: a.description,
      detailedDescription: '',
      basePath: '',
      version: a.version,
      status: a.status,
      openApiSpec: '',
      tags: a.tags || [],
      domains: a.domains || [],
      createdAt: a.createdAt || a.updatedAt,
      updatedAt: a.updatedAt
    }))
    const totalElements = typeof d?.totalElements === 'number' && d.totalElements >= 0 ? d.totalElements : (typeof d?.numberOfElements === 'number' ? d.numberOfElements : content.length)
    const totalPages = typeof d?.totalPages === 'number' && d.totalPages >= 0 ? d.totalPages : ((d?.hasNext ? (d.currentPage ?? 0) + 2 : (d?.currentPage ?? 0) + 1))
    return {
      content,
      totalElements,
      totalPages,
      currentPage: d?.currentPage ?? (params.page ?? 0),
      hasNext: !!d?.hasNext
    }
  } catch {
    return {
      content: [],
      totalElements: 0,
      totalPages: 0,
      currentPage: params.page ?? 0,
      hasNext: false
    }
  }
}

export async function fetchApiById(id: string): Promise<Api> {
  try {
    const res = await apiClient.get(`${SERVICE_PREFIX}/api/v1/api-registry/discovery/apis/${id}`)
    const d = res.data?.data
    const a = d?.api || d // support both shapes
    return {
      id: String(a.id),
      name: a.name,
      owner: a.owner,
      description: a.description,
      detailedDescription: a.detailedDescription,
      basePath: a.basePath,
      version: a.version,
      status: a.status,
      openApiSpec: a.openApiSpec,
      tags: a.tags || [],
      domains: a.domains || [],
      createdAt: a.createdAt,
      updatedAt: a.updatedAt
    }
  } catch {
    return {
      id,
      name: `Sample API ${id}`,
      owner: 'team-alpha',
      description: 'Short description of the API goes here.',
      detailedDescription: '# README\n\nThis is a sample API. Use the Try tab to explore endpoints.',
      basePath: '/api/sample',
      version: '1.0.0',
      status: 'PUBLISHED' as any,
      openApiSpec: JSON.stringify({ openapi: '3.0.0', info: { title: 'Sample', version: '1.0.0' }, paths: {} }),
      tags: ['sample', 'demo'],
      domains: ['finance', 'core'],
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString()
    }
  }
}

export async function fetchDomainStats(): Promise<DomainStatsResponse> {
  try {
    const res = await apiClient.get(`${SERVICE_PREFIX}/api/v1/api-registry/discovery/domains/stats`)
    return res.data
  } catch {
    return {
      domains: [
        { domain: 'finance', apiCount: 12 },
        { domain: 'core', apiCount: 8 },
        { domain: 'analytics', apiCount: 5 },
        { domain: 'security', apiCount: 3 }
      ]
    }
  }
}

export async function fetchApisByDomain(domain: string, page = 0, size = 10): Promise<ApiListResponse> {
  try {
    const res = await apiClient.get(`${SERVICE_PREFIX}/api/v1/api-registry/discovery/domains/${encodeURIComponent(domain)}/apis`, {
      params: { 
        page, 
        size,

      }
    })
    const d = res.data?.data || res.data
    const content: Api[] = (d?.apis || []).map((a: any) => ({
      id: String(a.id),
      name: a.name,
      owner: a.owner,
      description: a.description,
      detailedDescription: '',
      basePath: '',
      version: a.version,
      status: a.status,
      openApiSpec: '',
      tags: a.tags || [],
      domains: a.domains || [],
      createdAt: a.createdAt || a.updatedAt,
      updatedAt: a.updatedAt
    }))
    return {
      content,
      totalElements: d?.totalElements ?? content.length,
      totalPages: d?.totalPages ?? 1,
      currentPage: d?.currentPage ?? page,
      hasNext: !!d?.hasNext
    }
  } catch {
    return {
      content: [],
      totalElements: 0,
      totalPages: 0,
      currentPage: page,
      hasNext: false
    }
  }
}

export async function fetchAvailableTags(search?: string): Promise<string[]> {
  try {
    const res = await apiClient.get(`${SERVICE_PREFIX}/api/v1/api-registry/discovery/tags`, {
      params: { search, limit: 10 }
    })
    return res.data?.data?.tags || res.data?.tags || []
  } catch {
    return ['payments', 'users', 'analytics', 'orders', 'reports', 'auth', 'notifications']
  }
}

export async function fetchAvailableDomains(search?: string): Promise<string[]> {
  try {
    const res = await apiClient.get(`${SERVICE_PREFIX}/api/v1/api-registry/discovery/domains`, {
      params: { search, limit: 10 }
    })
    return res.data?.data?.domains || res.data?.domains || []
  } catch {
    return ['finance', 'core', 'analytics', 'security', 'operations']
  }
}

export async function fetchAvailableOwners(search?: string): Promise<string[]> {
  try {
    const res = await apiClient.get(`${SERVICE_PREFIX}/api/v1/api-registry/discovery/owners`, {
      params: { search, limit: 10 }
    })
    return res.data?.data?.owners || res.data?.owners || []
  } catch {
    return ['team-alpha', 'team-beta', 'platform-team', 'data-team']
  }
}
