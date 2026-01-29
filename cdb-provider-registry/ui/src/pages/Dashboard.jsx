import React from 'react'
import { Link } from 'react-router-dom'
import { getCurrentUser } from '../services/authService'

export default function Dashboard() {
  const user = getCurrentUser()


  const quickActions = [
    { title: 'APIs Registered', desc: 'View and browse APIs registered with the platform', icon: '🔗', to: '/apis' },
    { title: 'Manage Users', desc: 'Add, remove, and update user roles and access', icon: '👥', to: '/manage-users' },
    { title: 'Manage Keys', desc: 'Create and rotate API keys and credentials', icon: '🔑', to: '/keys' },
    { title: 'Configuration', desc: 'Update provider settings and environment configuration', icon: '⚙️', to: '/configuration' },
  ]

  return (
    <div className="dashboard">
      <div className="dashboard-header">
        <div>
          <h1>Welcome back, {user?.username || 'User'}</h1>
          <p>Here's what's happening with your provider environment today.</p>
        </div>
        <div className="dashboard-actions">
          <Link to="/apis" className="btn btn-primary">Register New API</Link>
        </div>
      </div>

      <div className="dashboard-section">
        <h2>Quick Actions</h2>
        <div className="action-grid">
          {quickActions.map((action, i) => (
            <Link key={i} className="action-card" to={action.to}>
              <div className="action-icon">{action.icon}</div>
              <div className="action-content">
                <h3>{action.title}</h3>
                <p>{action.desc}</p>
              </div>
              <svg className="action-arrow" viewBox="0 0 20 20" fill="currentColor">
                <path fillRule="evenodd" d="M7.293 14.707a1 1 0 010-1.414L10.586 10 7.293 6.707a1 1 0 011.414-1.414l4 4a1 1 0 010 1.414l-4 4a1 1 0 01-1.414 0z" clipRule="evenodd" />
              </svg>
            </Link>
          ))}
        </div>
      </div>
    </div>
  )
}
