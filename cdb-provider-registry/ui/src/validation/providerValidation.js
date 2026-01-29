// Business validation layer for provider registration

/** Field-level validators return true if valid, or a string message if invalid */
export const fieldValidators = {
  name: (v) => (v && v.trim().length >= 2) || 'Name must be at least 2 characters',
  code: (v) => (/^[a-z0-9-]{3,50}$/).test(v || '') || 'Code must be 3-50 chars, lowercase letters, numbers, or hyphens',
  contactEmail: (v) => (!v || /.+@.+\..+/.test(v)) || 'Invalid contact email',
  adminUsername: (v) => /.+@.+\..+/.test(v || '') || 'Valid email required for username',
  adminEmail: (v) => /.+@.+\..+/.test(v || '') || 'Valid email required',
  adminPassword: (v) => ((v || '').length >= 8) || 'Password must be at least 8 characters',
}

/**
 * Validate a full ProviderRegistrationRequest and return an errors map
 * @param {import('../models/provider').ProviderRegistrationRequest} data
 * @returns {Record<string, string>} errors map: key -> message
 */
export function validateProviderRegistration(data) {
  const errors = {}
  const checks = [
    ['name', data.name],
    ['code', data.code],
    ['contactEmail', data.contactEmail],
    ['adminUsername', data.adminUsername],
    ['adminEmail', data.adminEmail],
    ['adminPassword', data.adminPassword],
  ]
  checks.forEach(([k, v]) => {
    const rule = fieldValidators[k]
    if (rule) {
      const ok = rule(v)
      if (ok !== true) errors[k] = ok
    }
  })
  return errors
}
