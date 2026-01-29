import React, { useState, useEffect } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { getProviderConfigurations, updateProviderConfiguration } from '../services/providerConfigurationService'

export default function EditConfiguration() {
  const navigate = useNavigate()
  const { id } = useParams()
  const [loading, setLoading] = useState(false)
  const [initialLoading, setInitialLoading] = useState(true)
  const [error, setError] = useState('')
  const [formData, setFormData] = useState({
    configCode: '',
    configLabel: '',
    configValue: '',
    status: 'ACTIVE',
    visibility: 'PRIVATE',
    configDataType: 'STRING',
    hasList: false,
    configValueList: []
  })

  useEffect(() => {
    fetchConfiguration()
  }, [id])

  const fetchConfiguration = async () => {
    try {
      const configs = await getProviderConfigurations()
      const config = configs.find(c => c.id === parseInt(id))
      if (config) {
        setFormData({
          configCode: config.configCode,
          configLabel: config.configLabel,
          configValue: config.configValue || '',
          status: config.status,
          visibility: config.visibility,
          configDataType: config.configDataType,
          hasList: config.hasList,
          configValueList: config.configDataList?.map(item => item.configValue) || []
        })
      }
    } catch (error) {
      console.error('Failed to fetch configuration:', error)
    } finally {
      setInitialLoading(false)
    }
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setLoading(true)
    setError('')
    try {
      await updateProviderConfiguration(id, formData)
      navigate('/configuration')
    } catch (error) {
      console.error('Failed to update configuration:', error)
      setError(error.message || 'Failed to update configuration')
    } finally {
      setLoading(false)
    }
  }

  const addListValue = () => {
    setFormData(prev => ({
      ...prev,
      configValueList: [...prev.configValueList, '']
    }))
  }

  const updateListValue = (index, value) => {
    setFormData(prev => ({
      ...prev,
      configValueList: prev.configValueList.map((item, i) => i === index ? value : item)
    }))
  }

  const removeListValue = (index) => {
    setFormData(prev => ({
      ...prev,
      configValueList: prev.configValueList.filter((_, i) => i !== index)
    }))
  }

  if (initialLoading) {
    return <div className="container">Loading...</div>
  }

  return (
    <div className="container">
      <div className="form-header">
        <div>
          <h1>Edit Configuration</h1>
          <p>Update provider configuration setting</p>
        </div>
        <div className="form-actions">
          <button
            type="button"
            onClick={() => navigate('/configuration')}
            className="btn"
          >
            Cancel
          </button>
        </div>
      </div>

      <div className="form-container">
        <div className="form-content">
          {error && (
            <div className="error-message" style={{ 
              padding: '12px', 
              marginBottom: '20px', 
              backgroundColor: '#fee', 
              border: '1px solid #fcc', 
              borderRadius: '4px', 
              color: '#c33' 
            }}>
              {error}
            </div>
          )}
          <form onSubmit={handleSubmit} className="form-section">
            <div className="grid-2">
              <div className="form-group">
                <label>Config Code *</label>
                <input
                  type="text"
                  required
                  value={formData.configCode}
                  onChange={(e) => setFormData(prev => ({ ...prev, configCode: e.target.value }))}
                  placeholder="CONFIG.EXAMPLE_NAME"
                  pattern="CONFIG\.[A-Z_]+"
                  title="Must start with CONFIG. followed by uppercase letters and underscores only"
                />
                <div className="form-hint">Format: CONFIG.ALL_CAPS_NO_SPACE (e.g., CONFIG.DATABASE_URL)</div>
              </div>
              <div className="form-group">
                <label>Config Label *</label>
                <input
                  type="text"
                  required
                  value={formData.configLabel}
                  onChange={(e) => setFormData(prev => ({ ...prev, configLabel: e.target.value }))}
                  placeholder="Enter configuration label"
                />
                <div className="form-hint">Human-readable name for the configuration</div>
              </div>
            </div>

            <div className="grid-2">
              <div className="form-group">
                <label>Data Type</label>
                <select
                  value={formData.configDataType}
                  onChange={(e) => setFormData(prev => ({ ...prev, configDataType: e.target.value }))}
                >
                  <option value="STRING">String</option>
                  <option value="NUMBER">Number</option>
                  <option value="DOUBLE">Double</option>
                  <option value="FLOAT">Float</option>
                  <option value="BOOLEAN">Boolean</option>
                </select>
              </div>
              <div className="form-group">
                <label>Status</label>
                <select
                  value={formData.status}
                  onChange={(e) => setFormData(prev => ({ ...prev, status: e.target.value }))}
                >
                  <option value="ACTIVE">Active</option>
                  <option value="INACTIVE">Inactive</option>
                </select>
              </div>
            </div>

            <div className="grid-2">
              <div className="form-group">
                <label>Visibility</label>
                <select
                  value={formData.visibility}
                  onChange={(e) => setFormData(prev => ({ ...prev, visibility: e.target.value }))}
                >
                  <option value="PRIVATE">Private</option>
                  <option value="PUBLIC">Public</option>
                </select>
              </div>
              <div className="form-group">
                <label className="checkbox-label">
                  <input
                    type="checkbox"
                    checked={formData.hasList}
                    onChange={(e) => setFormData(prev => ({ ...prev, hasList: e.target.checked }))}
                  />
                  Has List Values
                </label>
              </div>
            </div>

            {!formData.hasList && (
              <div className="form-group">
                <label>Config Value</label>
                <input
                  type="text"
                  value={formData.configValue}
                  onChange={(e) => setFormData(prev => ({ ...prev, configValue: e.target.value }))}
                  placeholder="Enter configuration value"
                />
              </div>
            )}

            {formData.hasList && (
              <div className="form-group">
                <label>List Values</label>
                <div className="scope-grid">
                  {formData.configValueList.map((value, index) => (
                    <div key={index} className="scope-item">
                      <input
                        type="text"
                        value={value}
                        onChange={(e) => updateListValue(index, e.target.value)}
                        placeholder={`Value ${index + 1}`}
                        style={{ flex: 1, margin: 0, border: 'none', background: 'transparent' }}
                      />
                      <button
                        type="button"
                        onClick={() => removeListValue(index)}
                        className="btn-icon delete-btn"
                      >
                        ✕
                      </button>
                    </div>
                  ))}
                  <button
                    type="button"
                    onClick={addListValue}
                    className="btn"
                    style={{ justifySelf: 'start' }}
                  >
                    + Add Value
                  </button>
                </div>
              </div>
            )}

            <div className="form-actions" style={{ marginTop: '24px' }}>
              <button
                type="button"
                onClick={() => navigate('/configuration')}
                className="btn"
                disabled={loading}
              >
                Cancel
              </button>
              <button
                type="submit"
                className="btn primary"
                disabled={loading}
              >
                {loading ? 'Updating...' : 'Update Configuration'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  )
}