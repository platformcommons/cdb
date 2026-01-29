import React, { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import {
  getProviderConfigurations,
  deleteProviderConfiguration
} from '../services/providerConfigurationService'

export default function Configuration() {
  const [configs, setConfigs] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    fetchConfigs()
  }, [])

  const fetchConfigs = async () => {
    try {
      const configs = await getProviderConfigurations()
      setConfigs(configs)
    } catch (error) {
      console.error('Failed to fetch configurations:', error)
    } finally {
      setLoading(false)
    }
  }



  const handleDelete = async (id) => {
    if (confirm('Are you sure you want to delete this configuration?')) {
      try {
        await deleteProviderConfiguration(id)
        fetchConfigs()
      } catch (error) {
        console.error('Failed to delete configuration:', error)
      }
    }
  }



  if (loading) return <div className="p-6">Loading...</div>

  return (
    <div className="container">
      <div className="form-header">
        <div>
          <h1>Provider Configuration</h1>
          <p>Manage your provider configuration settings</p>
        </div>
        <div className="form-actions">
          <Link to="/configuration/new" className="btn primary">
            Add Configuration
          </Link>
        </div>
      </div>

      <div className="form-container">
        <table className="keys-table">
          <thead>
            <tr>
              <th>Code</th>
              <th>Label</th>
              <th>Type</th>
              <th>Status</th>
              <th>Visibility</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {configs.map((config) => (
              <tr key={config.id}>
                <td>{config.configCode}</td>
                <td>{config.configLabel}</td>
                <td>{config.configDataType}</td>
                <td>
                  <span className={`badge ${
                    config.status === 'ACTIVE' ? 'success' : 'danger'
                  }`}>
                    {config.status}
                  </span>
                </td>
                <td>{config.visibility}</td>
                <td>
                  <div className="key-actions">
                    <Link
                      to={`/configuration/${config.id}/edit`}
                      className="btn-icon"
                      title="Edit"
                    >
                      ✏️
                    </Link>
                    <button
                      onClick={() => handleDelete(config.id)}
                      className="btn-icon delete-btn"
                      title="Delete"
                    >
                      🗑️
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

    </div>
  )
}
