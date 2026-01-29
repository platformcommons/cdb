import { apiFetch } from './api'

const SERVICE_PREFIX = '/cdb-provider-registry'

export async function precheckRegistration(data) {
  return apiFetch(`${SERVICE_PREFIX}/api/v1/providers/register/precheck`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data)
  })
}

export async function registerProvider(data) {
  return apiFetch(`${SERVICE_PREFIX}/api/v1/providers/register`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data)
  })
}

export async function registerSimpleProvider(data) {
  const res = await apiFetch(`${SERVICE_PREFIX}/api/v1/providers`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data)
  })
  if (!res.ok) {
    const error = await res.text()
    throw new Error(error || 'Failed to register provider')
  }
  return res.json()
}

export async function searchProviders(searchTerm) {
  const res = await apiFetch(`${SERVICE_PREFIX}/api/v1/providers/search?q=${encodeURIComponent(searchTerm)}`)
  if (!res.ok) {
    throw new Error('Failed to search providers')
  }
  return res.json()
}

export async function createProviderRequest(requestData) {
  const res = await apiFetch(`${SERVICE_PREFIX}/api/v1/provider-requests`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      ...requestData,
      userId: 1, // TODO: Get from auth context
      userEmail: 'user@example.com' // TODO: Get from auth context
    })
  })
  if (!res.ok) {
    const error = await res.text()
    throw new Error(error || 'Failed to create request')
  }
  return res.json()
}

export async function getMyRequests() {
  const res = await apiFetch(`${SERVICE_PREFIX}/api/v1/provider-requests/my-requests`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ userId: 1 }) // TODO: Get from auth context
  })
  if (!res.ok) {
    throw new Error('Failed to fetch requests')
  }
  return res.json()
}

export async function getProviderRequests(providerId) {
  const res = await apiFetch(`${SERVICE_PREFIX}/api/v1/provider-requests/provider/${providerId}`)
  if (!res.ok) {
    throw new Error('Failed to fetch provider requests')
  }
  return res.json()
}

export async function approveRequest(requestId, approvalData) {
  const res = await apiFetch(`${SERVICE_PREFIX}/api/v1/provider-requests/${requestId}/approve`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      approverId: 1, // TODO: Get from auth context
      approvalData
    })
  })
  if (!res.ok) {
    throw new Error('Failed to approve request')
  }
  return res.json()
}

export async function rejectRequest(requestId, notes) {
  const res = await apiFetch(`${SERVICE_PREFIX}/api/v1/provider-requests/${requestId}/reject`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      approverId: 1, // TODO: Get from auth context
      notes
    })
  })
  if (!res.ok) {
    throw new Error('Failed to reject request')
  }
}

export async function fetchScopeMaster() {
  const res = await apiFetch(`${SERVICE_PREFIX}/api/v1/scopes`)
  if (!res.ok) {
    throw new Error('Failed to fetch scopes')
  }
  return res.json()
}

export async function listProviderKeys(providerId) {
  const res = await apiFetch(`${SERVICE_PREFIX}/api/v1/providers/${providerId}/keys`)
  if (!res.ok) {
    throw new Error('Failed to list keys')
  }
  return res.json()
}

export async function generateProviderKey(providerId, data) {
  const res = await apiFetch(`${SERVICE_PREFIX}/api/v1/providers/${providerId}/keys/generate`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data)
  })
  if (!res.ok) {
    throw new Error('Failed to generate key')
  }
  return res.json()
}

export async function validateProviderKey(providerId, data) {
  const res = await apiFetch(`${SERVICE_PREFIX}/api/v1/providers/${providerId}/keys/validate`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data)
  })
  if (!res.ok) {
    throw new Error('Failed to validate key')
  }
  return res.json()
}

export async function deactivateProviderKey(providerId, keyId) {
  const res = await apiFetch(`${SERVICE_PREFIX}/api/v1/providers/${providerId}/keys/${keyId}/deactivate`, {
    method: 'PUT'
  })
  if (!res.ok) {
    throw new Error('Failed to deactivate key')
  }
}