import { apiFetch } from './api'

const SERVICE_PREFIX = '/cdb-provider-registry'

// Initiate or re-send OTP by hitting backend precheck endpoint (via Gateway)
export async function requestOtp(code, adminEmail) {
  const res = await apiFetch(`${SERVICE_PREFIX}/api/v1/providers/register/precheck`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ code, adminEmail })
  })
  if (!res.ok) return null
  const key = await res.text()
  return key
}
