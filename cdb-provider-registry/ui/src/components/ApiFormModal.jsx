import React, { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import AutocompleteInput from './AutocompleteInput.jsx'
import { apiRegistryService } from '../services/apiRegistryService.js'

const ApiDesignerLazy = React.lazy(() => import('../modules/api-designer/index.jsx'))

function lintYaml(yaml) {
  if (!yaml || yaml.trim() === '') return { ok: true, warnings: ['Empty spec'] }
  const lines = yaml.split(/\r?\n/)
  const errors = []
  const warnings = []
  lines.forEach((ln, idx) => { if (/\t/.test(ln)) errors.push(`Line ${idx + 1}: Tab character found; use spaces for YAML`) })
  return { ok: errors.length === 0, errors, warnings }
}

function ToolbarButton({ onClick, children, title }) {
  return (
    <button type="button" onClick={onClick} title={title} className="px-2 py-1 text-sm rounded hover:bg-gray-100">
      {children}
    </button>
  )
}

export default function ApiFormModal({ api, onClose, onSave }) {
  const navigate = useNavigate()
  const [form, setForm] = useState({
    name: '', owner: '', description: '', detailedDescription: '', basePath: '', version: '1.0.0', status: 'DRAFT', openApiSpec: '', tags: '', domains: ''
  })
  const [showDesigner, setShowDesigner] = useState(false)

  useEffect(() => {
    if (api) {
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
  }, [api])

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

  const handleSave = () => {
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
    onSave && onSave(payload)
  }

  return (
    <div className="modal-overlay">
      <div className="modal-panel">
        <div className="modal-header">
          <h2 style={{ margin: 0 }}>{api ? 'Edit API' : 'Create API'}</h2>
          <div className="api-actions">
            <button onClick={handleSave} className="btn primary">Save</button>
            <button onClick={onClose} className="btn">Cancel</button>
          </div>
        </div>

        <div className="modal-content">
          <div className="grid-2">
            <div>
              <label>Name
                <input value={form.name} onChange={update('name')} placeholder="Payments API" />
              </label>
            </div>
            <div>
              <label>Owner
                <input value={form.owner} onChange={update('owner')} placeholder="Team Alpha" />
              </label>
            </div>
            <div>
              <label>Base Path
                <input value={form.basePath} onChange={update('basePath')} placeholder="/payments" />
              </label>
            </div>
            <div>
              <label>Version
                <input value={form.version} onChange={update('version')} placeholder="1.0.0" />
              </label>
            </div>
            <div>
              <label>Status
                <select className="select" value={form.status} onChange={update('status')}>
                  <option value="DRAFT">Draft</option>
                  <option value="PUBLISHED">Published</option>
                  <option value="DEPRECATED">Deprecated</option>
                  <option value="RETIRED">Retired</option>
                </select>
              </label>
            </div>
            <div style={{ display: 'flex', gap: '12px' }}>
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

          <div>
            <label>Short Description
              <input value={form.description} onChange={update('description')} placeholder="Concise description" />
            </label>
          </div>

          <div className="section-card">
            <div className="section-header">
              <div>Detailed Description</div>
              <div className="toolbar">
                <button type="button" className="btn icon" title="Bold" onClick={() => applyFormatting('**')}>B</button>
                <button type="button" className="btn icon" title="Italic" onClick={() => applyFormatting('*')}>I</button>
                <button type="button" className="btn icon" title="Inline Code" onClick={() => applyFormatting('`')}>Code</button>
                <button type="button" className="btn" title="H2" onClick={() => applyFormatting('\n## ', '')}>H2</button>
              </div>
            </div>
            <div className="split">
              <textarea id="desc-editor" className="textarea" value={form.detailedDescription} onChange={update('detailedDescription')} placeholder="Use markdown for rich text..." />
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
                  onClick={() => { onClose && onClose(); navigate('/api-designer'); }}
                  className="btn primary"
                  style={{ fontSize: '12px', padding: '4px 8px' }}
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
              <textarea className="textarea mono" value={form.openApiSpec} onChange={update('openApiSpec')} placeholder={"openapi: 3.0.0\ninfo:\n  title: Sample API\n  version: '1.0'\npaths:\n  /hello:\n    get:\n      summary: Say hello"} />
              <pre className="preview-pane mono">{form.openApiSpec || 'Preview'}</pre>
            </div>
          </div>
        </div>
      </div>
      
    </div>
  )
}

function ApiDesignerModal({ onClose, onSave }) {
  const [designerRef, setDesignerRef] = useState(null)
  
  const handleSave = () => {
    // Get YAML content from the designer context
    if (window.getApiDesignerYaml) {
      const yamlContent = window.getApiDesignerYaml()
      onSave(yamlContent)
    } else {
      // Fallback - generate basic YAML
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
            <div className="h-full flex items-center justify-center">
              <div className="text-center">
                <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600 mx-auto mb-4"></div>
                <p className="text-slate-600">Loading API Designer...</p>
              </div>
            </div>
          }>
            <ApiDesignerWithCallback onMount={setDesignerRef} />
          </React.Suspense>
        </div>
      </div>
    </div>
  )
}

function ApiDesignerWithCallback({ onMount }) {
  useEffect(() => {
    // Set up global function to extract YAML from designer
    window.getApiDesignerYaml = () => {
      // This will be called by the parent to get the current YAML
      const event = new CustomEvent('exportApiDesignerYaml')
      document.dispatchEvent(event)
      return window.lastExportedYaml || ''
    }
    
    return () => {
      delete window.getApiDesignerYaml
      delete window.lastExportedYaml
    }
  }, [])
  
  return <ApiDesignerLazy />
}

function DescriptionPreview({ text }) {
  // tiny markdown support
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
