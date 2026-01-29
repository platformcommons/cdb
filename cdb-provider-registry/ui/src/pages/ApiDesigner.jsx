import React, { useState, useEffect } from 'react'

export default function ApiDesigner() {
  const [models, setModels] = useState([])
  const [endpoints, setEndpoints] = useState([])
  const [showModelForm, setShowModelForm] = useState(false)
  const [showEndpointForm, setShowEndpointForm] = useState(false)
  const [modelForm, setModelForm] = useState({
    name: '', description: '', schema: '', category: '', isGlobal: true
  })
  const [endpointForm, setEndpointForm] = useState({
    name: '', description: '', path: '', method: 'GET', requestModel: '', responseModel: '', category: '', isGlobal: true
  })

  useEffect(() => {
    loadData()
  }, [])

  const loadData = async () => {
    try {
      // TODO: Load from API Registry service
      setModels([])
      setEndpoints([])
    } catch (error) {
      console.error('Failed to load data:', error)
    }
  }

  const saveModel = async () => {
    try {
      // TODO: Save to API Registry service
      console.log('Model to save:', modelForm)
      setShowModelForm(false)
      setModelForm({ name: '', description: '', schema: '', category: '', isGlobal: true })
      loadData()
    } catch (error) {
      console.error('Failed to save model:', error)
    }
  }


  return (
    <div style={{ minHeight: '100vh', background: 'var(--bg)', color: 'var(--text)' }}>
      <div style={{ maxWidth: '1200px', margin: '0 auto', padding: '24px' }}>
        <div className="api-header">
          <div>
            <h1 style={{ margin: 0, color: 'var(--text)' }}>API Designer</h1>
            <p style={{ color: 'var(--muted)', margin: '8px 0 0' }}>Create reusable models and endpoints for API projects</p>
          </div>
          <div className="api-actions">
            <button onClick={() => setShowModelForm(true)} className="btn primary">+ Model</button>
          </div>
        </div>

        <div className="grid-2" style={{ gap: 24, marginTop: 24 }}>
          <div className="section-card">
            <div className="section-header">
              <h3>Global Models ({models.length})</h3>
            </div>
            <div style={{ maxHeight: '400px', overflowY: 'auto' }}>
              {models.map(model => (
                <div key={model.id} className="item-card">
                  <div>
                    <h4 style={{ margin: '0 0 4px' }}>{model.name}</h4>
                    <p style={{ margin: 0, color: 'var(--muted)', fontSize: '14px' }}>{model.description}</p>
                    {model.category && <span className="badge">{model.category}</span>}
                  </div>
                </div>
              ))}
              {models.length === 0 && (
                <p style={{ color: 'var(--muted)', textAlign: 'center', padding: '20px' }}>No global models yet</p>
              )}
            </div>
          </div>

        </div>

        {showModelForm && (
          <div className="modal-overlay">
            <div className="modal-panel">
              <div className="modal-header">
                <h2>Create Global Model</h2>
                <div className="api-actions">
                  <button onClick={saveModel} className="btn primary">Save</button>
                  <button onClick={() => setShowModelForm(false)} className="btn">Cancel</button>
                </div>
              </div>
              <div className="modal-content">
                <div className="grid-2" style={{ gap: 16 }}>
                  <label>Name
                    <input value={modelForm.name} onChange={e => setModelForm(s => ({...s, name: e.target.value}))} placeholder="UserModel" />
                  </label>
                  <label>Category
                    <input value={modelForm.category} onChange={e => setModelForm(s => ({...s, category: e.target.value}))} placeholder="auth" />
                  </label>
                </div>
                <label>Description
                  <input value={modelForm.description} onChange={e => setModelForm(s => ({...s, description: e.target.value}))} placeholder="User data model" />
                </label>
                <label>JSON Schema
                  <textarea 
                    value={modelForm.schema} 
                    onChange={e => setModelForm(s => ({...s, schema: e.target.value}))} 
                    placeholder='{"type": "object", "properties": {"id": {"type": "string"}, "name": {"type": "string"}}}'
                    rows={8}
                  />
                </label>
              </div>
            </div>
          </div>
        )}

        {showEndpointForm && (
          <div className="modal-overlay">
            <div className="modal-panel">
              <div className="modal-header">
                <h2>Create Global Endpoint</h2>
                <div className="api-actions">
                  <button onClick={saveEndpoint} className="btn primary">Save</button>
                  <button onClick={() => setShowEndpointForm(false)} className="btn">Cancel</button>
                </div>
              </div>
              <div className="modal-content">
                <div className="grid-2" style={{ gap: 16 }}>
                  <label>Name
                    <input value={endpointForm.name} onChange={e => setEndpointForm(s => ({...s, name: e.target.value}))} placeholder="Get User" />
                  </label>
                  <label>Method
                    <select value={endpointForm.method} onChange={e => setEndpointForm(s => ({...s, method: e.target.value}))}>
                      <option value="GET">GET</option>
                      <option value="POST">POST</option>
                      <option value="PUT">PUT</option>
                      <option value="DELETE">DELETE</option>
                      <option value="PATCH">PATCH</option>
                    </select>
                  </label>
                  <label>Path
                    <input value={endpointForm.path} onChange={e => setEndpointForm(s => ({...s, path: e.target.value}))} placeholder="/users/{id}" />
                  </label>
                  <label>Category
                    <input value={endpointForm.category} onChange={e => setEndpointForm(s => ({...s, category: e.target.value}))} placeholder="users" />
                  </label>
                </div>
                <label>Description
                  <input value={endpointForm.description} onChange={e => setEndpointForm(s => ({...s, description: e.target.value}))} placeholder="Retrieve user by ID" />
                </label>
                <div className="grid-2" style={{ gap: 16 }}>
                  <label>Request Model
                    <input value={endpointForm.requestModel} onChange={e => setEndpointForm(s => ({...s, requestModel: e.target.value}))} placeholder="UserRequest" />
                  </label>
                  <label>Response Model
                    <input value={endpointForm.responseModel} onChange={e => setEndpointForm(s => ({...s, responseModel: e.target.value}))} placeholder="UserResponse" />
                  </label>
                </div>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}