import React, { useState } from 'react'
import Sidebar from '../components/Sidebar'

export default function AuthLayout({ children }) {
  const [open, setOpen] = useState(true)

  return (
    <div style={styles.wrapper}>
      {/* Sidebar */}
      {open && (
        <div style={styles.sidebar}>
          <Sidebar />
        </div>
      )}

      {/* Content area */}
      <div style={styles.content}>
        <div style={styles.contentHeader}>
          <button style={styles.toggleBtn} onClick={() => setOpen(!open)} aria-label="Toggle sidebar">
            ☰
          </button>
        </div>
        <div style={styles.contentBody} className="auth-content">
          {children}
        </div>
      </div>
    </div>
  )
}

const styles = {
  wrapper: {
    display: 'flex',
    minHeight: '100vh',
    boxSizing: 'border-box'
  },
  sidebar: {
    position: 'sticky',
    top: 0,
    alignSelf: 'flex-start',
    height: '100vh'
  },
  content: {
    flex: 1,
    display: 'flex',
    flexDirection: 'column',
    minWidth: 0
  },
  contentHeader: {
    height: '48px',
    display: 'flex',
    alignItems: 'center',
    padding: '0 12px',
    borderBottom: '1px solid rgba(255,255,255,0.08)',
    background: 'var(--card)',
    position: 'sticky',
    top: 0,
    zIndex: 1
  },
  toggleBtn: {
    border: '1px solid rgba(255,255,255,0.15)',
    background: 'rgba(255,255,255,0.04)',
    borderRadius: '6px',
    padding: '6px 10px',
    cursor: 'pointer',
    color: 'var(--text)'
  },
  contentBody: {
    padding: '16px',
    flex: 1,
    overflow: 'auto'
  }
}
