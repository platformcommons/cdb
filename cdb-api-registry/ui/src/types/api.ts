export interface Api {
  id: string
  name: string
  owner: string
  description: string
  detailedDescription: string
  basePath: string
  version: string
  status: ApiStatus
  openApiSpec: string
  tags: string[]
  domains: string[]
  createdAt: string
  updatedAt: string
}

export enum ApiStatus {
  DRAFT = 'DRAFT',
  PUBLISHED = 'PUBLISHED',
  DEPRECATED = 'DEPRECATED',
  RETIRED = 'RETIRED'
}

export interface SearchFilters {
  query?: string
  tags?: string[]
  domains?: string[]
  owners?: string[]
}

export interface ApiListResponse {
  content: Api[]
  totalElements: number
  totalPages: number
  currentPage: number
  hasNext: boolean
}

export interface DomainStat {
  domain: string
  apiCount: number
}

export interface DomainStatsResponse {
  domains: DomainStat[]
}
