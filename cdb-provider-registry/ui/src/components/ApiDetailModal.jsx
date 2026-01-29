import React, { useMemo } from 'react'

function SimpleMarkdown({ text }) {
  const html = useMemo(() => {
    if (!text) return ''
    let out = text
    out = out.replace(/^###### (.*$)/gim, '<h6>$1</h6>')
             .replace(/^##### (.*$)/gim, '<h5>$1</h5>')
             .replace(/^#### (.*$)/gim, '<h4>$1</h4>')
             .replace(/^### (.*$)/gim, '<h3>$1</h3>')
             .replace(/^## (.*$)/gim, '<h2>$1</h2>')
             .replace(/^# (.*$)/gim, '<h1>$1</h1>')
             .replace(/\*\*(.*?)\*\*/gim, '<strong>$1</strong>')
             .replace(/\*(.*?)\*/gim, '<em>$1</em>')
             .replace(/`(.*?)`/gim, '<code>$1</code>')
             .replace(/\n$/gim, '<br />')
    return out
  }, [text])
  return <div dangerouslySetInnerHTML={{ __html: html }} />
}

function lintYaml(yaml) {
  if (!yaml || yaml.trim() === '') return { ok: true, warnings: ['Empty spec'] }
  const lines = yaml.split(/\r?\n/)
  const errors = []
  const warnings = []
  // basic indentation check: no tab characters
  lines.forEach((ln, idx) => {
    if (/\t/.test(ln)) errors.push(`Line ${idx + 1}: Tab character found; use spaces for YAML`)
  })
  // key:value presence check in first 50 lines
  let colonCount = 0
  for (let i = 0; i < Math.min(lines.length, 50); i++) {
    if (/:\s*\S/.test(lines[i])) { colonCount++ }
  }
  if (colonCount < 3) warnings.push('YAML does not appear to contain typical key:value structure')
  return { ok: errors.length === 0, errors, warnings }
}

export default function ApiDetailModal({ api, onClose, onEdit, onRollback }) {
  const { api: apiMeta, auditHistory = [], availableVersions = [] } = api || {}
  const lint = useMemo(() => lintYaml(apiMeta?.openApiSpec || ''), [apiMeta])

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
      <div className="bg-white w-[95vw] h-[90vh] rounded-xl shadow-xl overflow-hidden flex flex-col">
        <div className="px-6 py-4 border-b flex items-center justify-between">
          <div>
            <h2 className="text-xl font-semibold">{apiMeta?.name} <span className="text-gray-400 font-normal">v{apiMeta?.version}</span></h2>
            <p className="text-gray-500 text-sm">{apiMeta?.description}</p>
          </div>
          <div className="flex items-center gap-2">
            <button onClick={onEdit} className="px-3 py-1.5 rounded-md bg-blue-600 text-white hover:bg-blue-700">Edit</button>
            <button onClick={onClose} className="px-3 py-1.5 rounded-md bg-gray-100 hover:bg-gray-200">Close</button>
          </div>
        </div>

        <div className="flex-1 overflow-auto p-6 grid grid-cols-12 gap-6">
          {/* Left: Details and Markdown Preview */}
          <div className="col-span-12 lg:col-span-4 space-y-4">
            <div className="border rounded-lg p-4">
              <h3 className="font-semibold mb-2">Details</h3>
              <div className="text-sm text-gray-700 space-y-1">
                <div><span className="text-gray-500">Owner:</span> {apiMeta?.owner || '-'} </div>
                <div><span className="text-gray-500">Base Path:</span> {apiMeta?.basePath || '-'} </div>
                <div><span className="text-gray-500">Status:</span> {apiMeta?.status || '-'} </div>
                <div><span className="text-gray-500">Updated:</span> {apiMeta?.updatedAt ? new Date(apiMeta.updatedAt).toLocaleString() : '-'} </div>
                <div><span className="text-gray-500">Tags:</span> {(apiMeta?.tags || []).join(', ') || '-'} </div>
              </div>
            </div>

            <div className="border rounded-lg p-4">
              <h3 className="font-semibold mb-2">Detailed Description</h3>
              {apiMeta?.detailedDescription ? (
                <div className="prose max-w-none text-sm">
                  <SimpleMarkdown text={apiMeta.detailedDescription} />
                </div>
              ) : (
                <p className="text-gray-500 text-sm">No detailed description provided.</p>
              )}
            </div>

            <div className="border rounded-lg p-4">
              <div className="flex items-center justify-between mb-2">
                <h3 className="font-semibold">Audit History</h3>
                {availableVersions?.length > 0 && (
                  <select
                    className="text-sm border rounded px-2 py-1"
                    onChange={e => onRollback && onRollback(e.target.value)}
                    defaultValue=""
                  >
                    <option value="" disabled>Rollback to...</option>
                    {availableVersions.map(v => <option key={v} value={v}>{v}</option>)}
                  </select>
                )}
              </div>
              <div className="max-h-64 overflow-auto divide-y">
                {auditHistory.length === 0 && (
                  <div className="text-sm text-gray-500">No changes yet.</div>
                )}
                {auditHistory.map(log => (
                  <div key={log.id} className="py-2 text-sm">
                    <div className="flex items-center justify-between">
                      <div className="font-medium">{log.action} <span className="text-gray-400">v{log.version}</span></div>
                      <div className="text-gray-500">{new Date(log.changedAt).toLocaleString()}</div>
                    </div>
                    <div className="text-gray-600">{log.changeDescription}</div>
                  </div>
                ))}
              </div>
            </div>
          </div>

          {/* Right: OpenAPI Spec */}
          <div className="col-span-12 lg:col-span-8 space-y-4">
            <div className="border rounded-lg p-4">
              <div className="flex items-center justify-between mb-3">
                <h3 className="font-semibold">OpenAPI Specification</h3>
                <div className="text-sm">
                  {lint.ok ? (
                    <span className="px-2 py-1 rounded-full bg-green-100 text-green-800">YAML OK</span>
                  ) : (
                    <span className="px-2 py-1 rounded-full bg-red-100 text-red-800">YAML Issues</span>
                  )}
                </div>
              </div>
              {!lint.ok && lint.errors?.length > 0 && (
                <div className="mb-3 text-sm text-red-600">
                  {lint.errors.map((e, i) => <div key={i}>• {e}</div>)}
                </div>
              )}
              {lint.warnings?.length > 0 && (
                <div className="mb-3 text-sm text-amber-600">
                  {lint.warnings.map((w, i) => <div key={i}>• {w}</div>)}
                </div>
              )}
              <div className="grid grid-cols-2 gap-4">
                <pre className="bg-gray-50 p-3 rounded text-xs overflow-auto max-h-[55vh]">
                  {apiMeta?.openApiSpec || 'No OpenAPI YAML provided.'}
                </pre>
                <div className="border rounded p-3 max-h-[55vh] overflow-auto">
                  <h4 className="font-medium mb-2">Documentation Preview</h4>
                  <p className="text-sm text-gray-600 mb-2">A full Swagger UI is not bundled; this is a lightweight preview.</p>
                  <OpenApiSummary yaml={apiMeta?.openApiSpec} />
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}

function OpenApiSummary({ yaml }) {
  // very rough extraction of title and paths from YAML text without dependencies
  if (!yaml) return <div className="text-gray-500 text-sm">No spec.</div>
  const lines = yaml.split(/\r?\n/)
  const doc = { info: {}, paths: [] }
  let inInfo = false
  let inPaths = false
  lines.forEach((ln) => {
    const t = ln.trimEnd()
    if (/^info:\s*$/.test(t)) { inInfo = true; inPaths = false; return }
    if (/^paths:\s*$/.test(t)) { inPaths = true; inInfo = false; return }
    if (/^components:\s*$/.test(t)) { inPaths = false; inInfo = false; return }
    if (inInfo) {
      const m = t.match(/^(title|version|description):\s*(.*)$/)
      if (m) doc.info[m[1]] = m[2]
    } else if (inPaths) {
      const m = t.match(/^([\s\-\w\/_{}:]+):\s*$/)
      if (m && m[1].startsWith('/')) doc.paths.push(m[1])
    }
  })
  return (
    <div className="text-sm">
      <div className="mb-2">
        <div className="font-medium">{doc.info.title || 'Untitled API'}</div>
        <div className="text-gray-500">{doc.info.description || ''}</div>
        {doc.info.version && <div className="text-gray-500">Version: {doc.info.version}</div>}
      </div>
      <div>
        <div className="font-medium mb-1">Paths</div>
        {doc.paths.length === 0 && <div className="text-gray-500">No paths parsed.</div>}
        <ul className="list-disc pl-5 space-y-1 max-h-48 overflow-auto">
          {doc.paths.map(p => <li key={p} className="font-mono">{p}</li>)}
        </ul>
      </div>
    </div>
  )
}
