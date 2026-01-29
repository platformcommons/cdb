import { apiFetch } from './api'

const SERVICE_PREFIX = '/cdb-provider-registry'

export async function getProviderEnvironments(providerId) {
  const res = await apiFetch(`${SERVICE_PREFIX}/api/provider-environments/provider/${providerId}`)
  if (!res.ok) {
    throw new Error('Failed to fetch provider environments')
  }
  return res.json()
}

export async function getProviderEnvironment(id) {
  const res = await apiFetch(`${SERVICE_PREFIX}/api/provider-environments/${id}`)
  if (!res.ok) {
    throw new Error('Failed to fetch provider environment')
  }
  return res.json()
}

export async function createProviderEnvironment(data) {
  const res = await apiFetch(`${SERVICE_PREFIX}/api/provider-environments`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data)
  })
  if (!res.ok) {
    const error = await res.text()
    throw new Error(error || 'Failed to create provider environment')
  }
  return res.json()
}

export async function updateProviderEnvironment(id, data) {
  const res = await apiFetch(`${SERVICE_PREFIX}/api/provider-environments/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data)
  })
  if (!res.ok) {
    const error = await res.text()
    throw new Error(error || 'Failed to update provider environment')
  }
  return res.json()
}

export async function deleteProviderEnvironment(id) {
  const res = await apiFetch(`${SERVICE_PREFIX}/api/provider-environments/${id}`, {
    method: 'DELETE'
  })
  if (!res.ok) {
    const error = await res.text()
    throw new Error(error || 'Failed to delete provider environment')
  }
  return true
}
