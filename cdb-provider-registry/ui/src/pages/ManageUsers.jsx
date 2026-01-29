import React, { useEffect, useState } from 'react'
import { getProviderRequests, approveRequest, rejectRequest } from '../services/providerService'

export default function ManageUsers() {
  const [activeTab, setActiveTab] = useState('users')
  const [requests, setRequests] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  useEffect(() => {
    if (activeTab === 'requests') {
      loadRequests()
    }
  }, [activeTab])

  const loadRequests = async () => {
    setLoading(true)
    setError('')
    try {
      // TODO: Get current provider ID from context
      const providerId = 1 // Placeholder
      const data = await getProviderRequests(providerId)
      setRequests(data)
    } catch (err) {
      setError('Failed to load requests')
    } finally {
      setLoading(false)
    }
  }

  const handleApprove = async (requestId) => {
    try {
      await approveRequest(requestId, {
        approvalNotes: 'Approved by admin',
        assignedRole: 'USER'
      })
      setSuccess('Request approved successfully')
      await loadRequests()
    } catch (err) {
      setError('Failed to approve request')
    }
  }

  const handleReject = async (requestId) => {
    try {
      await rejectRequest(requestId, 'Request rejected by admin')
      setSuccess('Request rejected')
      await loadRequests()
    } catch (err) {
      setError('Failed to reject request')
    }
  }

  return (
    <section>
      <h1>Manage Users</h1>
      
      <div style={{ borderBottom: '1px solid #e5e7eb', marginBottom: '20px' }}>
        <div style={{ display: 'flex', gap: '20px' }}>
          <button
            onClick={() => setActiveTab('users')}
            style={{
              padding: '10px 0',
              background: 'none',
              border: 'none',
              borderBottom: activeTab === 'users' ? '2px solid #2563eb' : '2px solid transparent',
              color: activeTab === 'users' ? '#2563eb' : '#6b7280',
              cursor: 'pointer',
              fontSize: '16px',
              fontWeight: '500'
            }}
          >
            Users
          </button>
          <button
            onClick={() => setActiveTab('requests')}
            style={{
              padding: '10px 0',
              background: 'none',
              border: 'none',
              borderBottom: activeTab === 'requests' ? '2px solid #2563eb' : '2px solid transparent',
              color: activeTab === 'requests' ? '#2563eb' : '#6b7280',
              cursor: 'pointer',
              fontSize: '16px',
              fontWeight: '500'
            }}
          >
            Requests
          </button>
        </div>
      </div>

      {error && (
        <div style={{ background: '#fef2f2', border: '1px solid #fecaca', color: '#dc2626', padding: '12px', borderRadius: '8px', marginBottom: '20px' }}>
          {error}
        </div>
      )}

      {success && (
        <div style={{ background: '#f0fdf4', border: '1px solid #bbf7d0', color: '#16a34a', padding: '12px', borderRadius: '8px', marginBottom: '20px' }}>
          {success}
        </div>
      )}

      {activeTab === 'users' && (
        <div className="card">
          <h3>Users</h3>
          <p>Coming soon: list users in this provider, grant/revoke roles and permissions.</p>
        </div>
      )}

      {activeTab === 'requests' && (
        <div className="card">
          <h3>Access Requests</h3>
          {loading ? (
            <p>Loading requests...</p>
          ) : requests.length === 0 ? (
            <p>No pending requests</p>
          ) : (
            <div style={{ display: 'grid', gap: '12px' }}>
              {requests.map(request => (
                <div key={request.id} style={{
                  border: '1px solid #e5e7eb',
                  borderRadius: '8px',
                  padding: '16px'
                }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'start' }}>
                    <div style={{ flex: 1 }}>
                      <h4 style={{ margin: '0 0 8px 0' }}>{request.userEmail}</h4>
                      <p style={{ margin: '0 0 4px 0', color: '#6b7280', fontSize: '14px' }}>
                        Requested Role: {request.requestedRole}
                      </p>
                      <p style={{ margin: '0 0 4px 0', color: '#6b7280', fontSize: '14px' }}>
                        Requested: {new Date(request.createdAt).toLocaleDateString()}
                      </p>
                      {request.requestMessage && (
                        <p style={{ margin: '8px 0 0 0', fontSize: '14px' }}>
                          Message: {request.requestMessage}
                        </p>
                      )}
                    </div>
                    <div style={{ display: 'flex', gap: '8px' }}>
                      <button
                        onClick={() => handleApprove(request.id)}
                        style={{
                          padding: '6px 12px',
                          background: '#16a34a',
                          color: 'white',
                          border: 'none',
                          borderRadius: '4px',
                          cursor: 'pointer',
                          fontSize: '14px'
                        }}
                      >
                        Approve
                      </button>
                      <button
                        onClick={() => handleReject(request.id)}
                        style={{
                          padding: '6px 12px',
                          background: '#dc2626',
                          color: 'white',
                          border: 'none',
                          borderRadius: '4px',
                          cursor: 'pointer',
                          fontSize: '14px'
                        }}
                      >
                        Reject
                      </button>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </section>
  )
}
