import React, { useEffect, useMemo, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { apiRegistryService } from '../services/apiRegistryService.js'
import AutocompleteInput from '../components/AutocompleteInput.jsx'

const ApiDesignerLazy = React.lazy(() => import('../modules/api-designer/index.jsx'))

function lintYaml(yaml) {
  if (!yaml || yaml.trim() === '') return { ok: true, warnings: ['Empty spec'] }
  const lines = yaml.split(/\r?\n/)
  const errors = []
  const warnings = []
  lines.forEach((ln, idx) => { if (/\t/.test(ln)) errors.push(`Line ${idx + 1}: Tab character found; use spaces for YAML`) })
  return { ok: errors.length === 0, errors, warnings }
}

export default function ApiEdit() {
  const navigate = useNavigate()
  const { id } = useParams()
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [showDesigner, setShowDesigner] = useState(false)
  const [form, setForm] = useState({
    name: '', owner: '', description: '', detailedDescription: '', basePath: '', version: '1.0.0', status: 'DRAFT', openApiSpec: '', tags: '', domains: ''
  })

  useEffect(() => {
    let mounted = true
    const load = async () => {
      try {
        setLoading(true)
        const detail = await apiRegistryService.getApiDetail(id)
        const api = detail?.api || detail // fallback if shape differs
        if (mounted && api) {
          setForm({
            name: api.name || '',
            owner: api.owner || '',
            description: api.description || '',
            detailedDescription: api.detailedDescription || '',
            basePath: api.basePath || '',
            version: api.version || '1.0.0',
            status: api.status || 'DRAFT',
            openApiSpec: api.openApiSpec || '',
            tags: (api.tags || []).join(', '),
            domains: (api.domains || []).join(', ')
          })
        }
      } catch (e) {
        console.error('Failed to load API detail', e)
        setError(e?.response?.data?.message || 'Failed to load API')
      } finally {
        if (mounted) setLoading(false)
      }
    }
    load()
    return () => { mounted = false }
  }, [id])

  const lint = useMemo(() => lintYaml(form.openApiSpec), [form.openApiSpec])
  const update = (k) => (e) => setForm(s => ({ ...s, [k]: e.target.value }))

  const applyFormatting = (wrapL, wrapR = wrapL) => {
    const el = document.getElementById('desc-editor')
    if (!el) return
    const start = el.selectionStart
    const end = el.selectionEnd
    const before = el.value.substring(0, start)
    const sel = el.value.substring(start, end)
    const after = el.value.substring(end)
    const newVal = `${before}${wrapL}${sel}${wrapR}${after}`
    setForm(s => ({ ...s, detailedDescription: newVal }))
    setTimeout(() => {
      el.focus()
      el.selectionStart = start + wrapL.length
      el.selectionEnd = end + wrapL.length
    }, 0)
  }

  const handleSave = async () => {
    setError('')
    setSaving(true)
    try {
      const payload = {
        name: form.name,
        owner: form.owner,
        description: form.description,
        detailedDescription: form.detailedDescription,
        basePath: form.basePath,
        version: form.version,
        status: form.status,
        openApiSpec: form.openApiSpec,
        tags: form.tags.split(',').map(t => t.trim()).filter(Boolean),
        domains: form.domains.split(',').map(d => d.trim()).filter(Boolean)
      }
      await apiRegistryService.updateApi(id, payload)
      navigate('/apis')
    } catch (e) {
      console.error('Failed to update API', e)
      setError(e?.response?.data?.message || 'Failed to update API')
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="container">
      <div className="form-header">
        <div>
          <h1>{loading ? 'Loading…' : `Edit API: ${form.name || ''}`}</h1>
          <p className="muted">Update your API metadata and OpenAPI YAML with a clean, consistent editor.</p>
        </div>
        <div className="form-actions">
          <button type="button" className="btn" onClick={() => navigate('/apis')} disabled={saving}>Cancel</button>
          <button type="button" className="btn primary" onClick={handleSave} disabled={saving || loading}>
            {saving ? 'Saving…' : 'Save Changes'}
          </button>
        </div>
      </div>

      {error && (
        <div className="empty-state" style={{border:'1px solid #fecaca', background:'#fef2f2', color:'#991b1b', marginBottom:'24px'}}>
          {error}
        </div>
      )}

      <div className="form-container">
        <div className="form-content">
          <div className="form-section">
            <div className="grid-2">
              <div className="form-group">
                <label htmlFor="name">Name *</label>
                <input id="name" value={form.name} onChange={update('name')} placeholder="Payments API" disabled={loading} />
              </div>
              <div className="form-group">
                <label htmlFor="owner">Owner</label>
                <input id="owner" value={form.owner} onChange={update('owner')} placeholder="Team Alpha" disabled={loading} />
              </div>
              <div className="form-group">
                <label htmlFor="basePath">Base Path</label>
                <input id="basePath" value={form.basePath} onChange={update('basePath')} placeholder="/payments" disabled={loading} />
              </div>
              <div className="form-group">
                <label htmlFor="version">Version</label>
                <input id="version" value={form.version} onChange={update('version')} placeholder="1.0.0" disabled={loading} />
              </div>
              <div className="form-group">
                <label htmlFor="status">Status</label>
                <select id="status" value={form.status} onChange={update('status')} disabled={loading}>
                  <option value="DRAFT">Draft</option>
                  <option value="PUBLISHED">Published</option>
                  <option value="DEPRECATED">Deprecated</option>
                  <option value="RETIRED">Retired</option>
                </select>
              </div>
              <div className="form-group" style={{ gridColumn: 'span 2' }}>
                <div style={{ display: 'flex', gap: '16px' }}>
                  <div style={{ flex: 1 }}>
                    <AutocompleteInput
                      label="Tags"
                      value={form.tags}
                      onChange={(value) => setForm(s => ({ ...s, tags: value }))}
                      onSearch={apiRegistryService.searchTags}
                      placeholder="Add tags..."
                      hint="Press Enter, Tab, or comma to add tags"
                    />
                  </div>
                  <div style={{ flex: 1 }}>
                    <AutocompleteInput
                      label="Domains"
                      value={form.domains}
                      onChange={(value) => setForm(s => ({ ...s, domains: value }))}
                      onSearch={apiRegistryService.searchDomains}
                      placeholder="Add domains..."
                      hint="Press Enter, Tab, or comma to add domains"
                    />
                  </div>
                </div>
              </div>
            </div>

            <div className="form-group">
              <label htmlFor="description">Short Description</label>
              <input id="description" value={form.description} onChange={update('description')} placeholder="Concise description" disabled={loading} />
            </div>

            <div className="section-card">
              <div className="section-header">
                <div>Detailed Description</div>
                <div className="toolbar">
                  <button type="button" className="btn icon" title="Bold" onClick={() => applyFormatting('**')} disabled={loading}>B</button>
                  <button type="button" className="btn icon" title="Italic" onClick={() => applyFormatting('*')} disabled={loading}>I</button>
                  <button type="button" className="btn icon" title="Inline Code" onClick={() => applyFormatting('`')} disabled={loading}>Code</button>
                  <button type="button" className="btn" title="H2" onClick={() => applyFormatting('\n## ', '')} disabled={loading}>H2</button>
                </div>
              </div>
              <div className="split">
                <textarea id="desc-editor" className="textarea" value={form.detailedDescription} onChange={update('detailedDescription')} placeholder="Use markdown for rich text..." disabled={loading} />
                <div className="preview-pane">
                  <DescriptionPreview text={form.detailedDescription} />
                </div>
              </div>
            </div>

            <div className="section-card">
              <div className="section-header">
                <div>OpenAPI Specification (YAML)</div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <button 
                    type="button" 
                    onClick={() => setShowDesigner(true)}
                    className="btn primary"
                    style={{ fontSize: '12px', padding: '4px 8px' }}
                    disabled={loading}
                  >
                    🎨 Design API
                  </button>
                  {lint.ok ? (
                    <span className="badge success">YAML OK</span>
                  ) : (
                    <span className="badge danger">YAML Issues</span>
                  )}
                </div>
              </div>
              {!lint.ok && lint.errors?.length > 0 && (
                <div className="notice error">
                  {lint.errors.map((e, i) => <div key={i}>• {e}</div>)}
                </div>
              )}
              {lint.warnings?.length > 0 && (
                <div className="notice warning">
                  {lint.warnings.map((w, i) => <div key={i}>• {w}</div>)}
                </div>
              )}
              <div className="split">
                <textarea className="textarea mono" value={form.openApiSpec} onChange={update('openApiSpec')} placeholder={"openapi: 3.0.0\ninfo:\n  title: Sample API\n  version: '1.0'\npaths:\n  /hello:\n    get:\n      summary: Say hello"} disabled={loading} />
                <pre className="preview-pane mono">{form.openApiSpec || 'Preview'}</pre>
              </div>
            </div>
          </div>
        </div>
      </div>

      {showDesigner && (
        <ApiDesignerModal 
          initialYaml={form.openApiSpec}
          onClose={() => setShowDesigner(false)}
          onSave={(yamlContent) => {
            setForm(s => ({ ...s, openApiSpec: yamlContent }))
            setShowDesigner(false)
          }}
        />
      )}
    </div>
  )
}

function ApiDesignerModal({ initialYaml, onClose, onSave }) {
  const handleSave = () => {
    if (window.getApiDesignerYaml) {
      const yamlContent = window.getApiDesignerYaml()
      onSave(yamlContent)
    } else {
      const basicYaml = `openapi: 3.0.1
info:
  title: My API
  description: API created with API Designer
  version: 1.0.0
paths:
  /example:
    get:
      summary: Example endpoint
      responses:
        '200':
          description: Successful response`
      onSave(basicYaml)
    }
  }
  
  return (
    <div className="modal-overlay" style={{ zIndex: 1001 }}>
      <div className="modal-panel" style={{ maxWidth: '95vw', height: '95vh' }}>
        <div className="modal-header">
          <h2 style={{ margin: 0 }}>API Designer</h2>
          <div className="api-actions">
            <button onClick={handleSave} className="btn primary">Use This Design</button>
            <button onClick={onClose} className="btn">Cancel</button>
          </div>
        </div>
        <div style={{ flex: 1, overflow: 'hidden' }}>
          <React.Suspense fallback={
            <div style={{ height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              <div style={{ textAlign: 'center' }}>
                <div style={{ 
                  width: '32px', 
                  height: '32px', 
                  border: '2px solid #e5e7eb', 
                  borderTop: '2px solid #4f46e5', 
                  borderRadius: '50%', 
                  animation: 'spin 1s linear infinite',
                  margin: '0 auto 16px'
                }}></div>
                <p style={{ color: '#6b7280' }}>Loading API Designer...</p>
              </div>
            </div>
          }>
            <ApiDesignerWithCallback initialYaml={initialYaml} />
          </React.Suspense>
        </div>
      </div>
    </div>
  )
}

function ApiDesignerWithCallback({ initialYaml }) {
  useEffect(() => {
    window.getApiDesignerYaml = () => {
      const event = new CustomEvent('exportApiDesignerYaml')
      document.dispatchEvent(event)
      return window.lastExportedYaml || ''
    }
    
    // Pass initial YAML to the designer
    if (initialYaml && initialYaml.trim()) {
      window.initialApiYaml = initialYaml
    }
    
    return () => {
      delete window.getApiDesignerYaml
      delete window.lastExportedYaml
      delete window.initialApiYaml
    }
  }, [initialYaml])
  
  return <ApiDesignerLazy />
}

function DescriptionPreview({ text }) {
  const html = useMemo(() => {
    if (!text) return '<span class="text-gray-500">Nothing to preview.</span>'
    return text
      .replace(/^###### (.*$)/gim, '<h6>$1</h6>')
      .replace(/^##### (.*$)/gim, '<h5>$1</h5>')
      .replace(/^#### (.*$)/gim, '<h4>$1</h4>')
      .replace(/^### (.*$)/gim, '<h3>$1</h3>')
      .replace(/^## (.*$)/gim, '<h2>$1</h2>')
      .replace(/^# (.*$)/gim, '<h1>$1</h1>')
      .replace(/\*\*(.*?)\*\*/gim, '<strong>$1</strong>')
      .replace(/\*(.*?)\*/gim, '<em>$1</em>')
      .replace(/`(.*?)`/gim, '<code>$1</code>')
      .replace(/\n$/gim, '<br />')
  }, [text])
  return <div className="prose max-w-none" dangerouslySetInnerHTML={{ __html: html }} />
}
