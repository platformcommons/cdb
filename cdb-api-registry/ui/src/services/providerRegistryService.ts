import { apiClient } from './apiClient'

const SERVICE_PREFIX = '/cdb-provider-registry'

export type Provider = {
  id: number
  name: string
  code: string
  description?: string
  status?: string
  contactEmail?: string
  contactPhone?: string
  tags?: string // comma-separated from backend
}

export type ProviderKeySummary = {
  id: number
  keyId: string
  title?: string
  keyType: 'ENCRYPTION' | 'SIGNING'
  keyStatus: 'ACTIVE' | 'NOT_ACTIVE' | 'PENDING_FOR_APPROVAL'
  environment: 'PRODUCTION' | 'SANDBOX'
  clientId: string
  issuedAt?: string
  expiresAt?: string
  scopes?: string[]
  publicKeyPem?: string
  privateKeyChecksum?: string
}

export type GenerateKeyResponse = {
  keyId: string
  publicKeyPem?: string
  privateKeyPem?: string
  keyType: 'ENCRYPTION' | 'SIGNING'
  keyStatus: 'ACTIVE' | 'NOT_ACTIVE' | 'PENDING_FOR_APPROVAL'
  environment: 'PRODUCTION' | 'SANDBOX'
  clientId: string
  issuedAt?: string
  expiresAt?: string
  scopes?: string[]
  title?: string
}

export type ProviderEnvironment = {
  id: number
  providerId: number
  environmentType: 'PRODUCTION' | 'SANDBOX'
  baseUrl?: string
  uptimeStatus?: string
  rateLimit?: number
  remarks?: string
}

export const providerRegistryService = {
  async searchProviders(params: { q?: string; tag?: string }): Promise<Provider[]> {
    const res = await apiClient.get(`${SERVICE_PREFIX}/api/v1/providers/search`, { params })
    return res.data as Provider[]
  },

  async getProviderById(id: number): Promise<Provider> {
    const res = await apiClient.get(`${SERVICE_PREFIX}/api/v1/providers/${id}`)
    return res.data as Provider
  },

  async listProviderKeys(providerId: number): Promise<ProviderKeySummary[]> {
    const res = await apiClient.get(`${SERVICE_PREFIX}/api/v1/providers/${providerId}/keys/public`)
    return res.data as ProviderKeySummary[]
  },

  async listProviderEnvironments(providerId: number): Promise<ProviderEnvironment[]> {
    const res = await apiClient.get(`${SERVICE_PREFIX}/api/provider-environments/provider/${providerId}`)
    return res.data as ProviderEnvironment[]
  },

  async generateSymmetricKey(providerId: number, clientId: string): Promise<GenerateKeyResponse> {
    const body = {
      title: `Symmetric key for ${clientId}`,
      keyType: 'ENCRYPTION',
      keyStatus: 'PENDING_FOR_APPROVAL',
      environment: 'SANDBOX',
      clientId,
      scopes: [],
    }
    const res = await apiClient.post(`${SERVICE_PREFIX}/api/v1/providers/${providerId}/keys/symmetric/request`, body)
    return res.data as GenerateKeyResponse
  }
}

export function extractTagsSet(providers: Provider[]): string[] {
  const set = new Set<string>()
  providers.forEach(p => {
    const csv = p.tags || ''
    csv.split(',').map(s => s.trim()).filter(Boolean).forEach(t => set.add(t))
  })
  return Array.from(set).sort()
}

export function buildEnvFileContent(provider: Provider, publicKeyPem?: string, prodBaseUrl?: string, sandboxBaseUrl?: string): string {
  // Compose .env content with comments as requested
  const lines: string[] = []
  lines.push('# Provider public information exported from CDB')
  lines.push(`# Name: ${provider.name}`)
  lines.push(`# Code: ${provider.code}`)
  lines.push('')
  lines.push(`TARGET_PROVDER_ID=${provider.id}`)
  lines.push(`TARGET_PROVIDER_CODE=${provider.code}`)
  lines.push(`TARGET_PROVIDER_PROD_BASE_URL=${prodBaseUrl ?? ''}`)
  lines.push(`TARGET_PROVIDER_SANDBOX_BASE_URL=${sandboxBaseUrl ?? ''}`)
  lines.push(`TARGET_PROVIDER_PUBLIC_KEY=${(publicKeyPem || '').replace(/\n/g, '\\n')}`)
  lines.push('')
  lines.push('# Note: PUBLIC_KEY is PEM formatted with newlines escaped as \\n for .env compatibility')
  return lines.join('\n')
}

export function downloadTextFile(filename: string, content: string) {
  const blob = new Blob([content], { type: 'text/plain;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  document.body.appendChild(a)
  a.click()
  a.remove()
  URL.revokeObjectURL(url)
}
