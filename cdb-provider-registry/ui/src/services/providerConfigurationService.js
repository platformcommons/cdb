import { apiFetch } from './api'

const SERVICE_PREFIX = '/cdb-provider-registry'

export async function getProviderConfigurations() {
  const res = await apiFetch(`${SERVICE_PREFIX}/api/v1/provider-configurations`)
  if (!res.ok) {
    throw new Error('Failed to fetch provider configurations')
  }
  return res.json()
}

export async function getProviderConfiguration(id) {
  const res = await apiFetch(`${SERVICE_PREFIX}/api/v1/provider-configurations/${id}`)
  if (!res.ok) {
    throw new Error('Failed to fetch provider configuration')
  }
  return res.json()
}

export async function createProviderConfiguration(data) {
  const res = await apiFetch(`${SERVICE_PREFIX}/api/v1/provider-configurations`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data)
  })
  if (!res.ok) {
    const error = await res.text()
    throw new Error(error || 'Failed to create provider configuration')
  }
  return res.json()
}

export async function updateProviderConfiguration(id, data) {
  const res = await apiFetch(`${SERVICE_PREFIX}/api/v1/provider-configurations/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data)
  })
  if (!res.ok) {
    const error = await res.text()
    throw new Error(error || 'Failed to update provider configuration')
  }
  return res.json()
}

export async function deleteProviderConfiguration(id) {
  const res = await apiFetch(`${SERVICE_PREFIX}/api/v1/provider-configurations/${id}`, {
    method: 'DELETE'
  })
  if (!res.ok) {
    throw new Error('Failed to delete provider configuration')
  }
}

export async function getPublicProviderConfiguration(configCode, providerCode) {
  const res = await apiFetch(`${SERVICE_PREFIX}/api/v1/provider-configurations/public?configCode=${encodeURIComponent(configCode)}&providerCode=${encodeURIComponent(providerCode)}`)
  if (!res.ok) {
    throw new Error('Failed to fetch public provider configuration')
  }
  return res.json()
}