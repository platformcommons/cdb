import React, {useState} from 'react'
import {NavLink, useNavigate} from 'react-router-dom'
import {logout} from '../services/authService'

export default function Sidebar() {
    const navigate = useNavigate()
    const [openMgmt, setOpenMgmt] = useState(true)
    const [openSettings, setOpenSettings] = useState(true)
    const [openTools, setOpenTools] = useState(true)

    const handleLogout = () => {
        logout()
        localStorage.clear()
        sessionStorage.clear()
        navigate('/login')
    }

    const linkStyle = ({isActive}) => ({
        display: 'block',
        padding: '8px 12px',
        borderRadius: '6px',
        background:  '#111827' ,
        color: isActive ? '#2563eb' : '#f2f2f2',
        textDecoration: 'none',
        fontWeight: isActive ? 600 : 500,
    })

    const sectionHeaderStyle = {
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        padding: '8px 8px',
        color: '#6b7280',
        fontSize: '0.8rem',
        textTransform: 'uppercase',
        letterSpacing: '0.04em',
        marginTop: '12px',
        cursor: 'pointer'
    }

    const caret = (open) => (
        <span style={{transform: `rotate(${open ? 90 : 0}deg)`, transition: 'transform 150ms ease'}}>▶</span>
    )

    return (
        <aside style={styles.aside}>
            <div style={styles.header}>Menu</div>

            <div style={{padding: '8px'}}>
                <NavLink to="/dashboard" style={linkStyle}>
                    Dashboard
                </NavLink>


                <div style={sectionHeaderStyle} onClick={() => setOpenMgmt(!openMgmt)}>
                    <span>Management</span>
                    {caret(openMgmt)}
                </div>
                {openMgmt && (
                    <div style={styles.submenu}>
                        <NavLink to="/apis" style={linkStyle}>Manage APIs</NavLink>
                        <NavLink to="/manage-users" style={linkStyle}>Manage Users</NavLink>
                        <NavLink to="/keys" style={linkStyle}>Manage Keys</NavLink>
                    </div>
                )}

                <div style={sectionHeaderStyle} onClick={() => setOpenSettings(!openSettings)}>
                    <span>Settings</span>
                    {caret(openSettings)}
                </div>
                {openSettings && (
                    <div style={styles.submenu}>
                        <NavLink to="/configuration" style={linkStyle}>Configuration</NavLink>
                        <NavLink to="/sandbox" style={linkStyle}>Environment</NavLink>

                    </div>
                )}

              {/*  <div style={sectionHeaderStyle} onClick={() => setOpenTools(!openTools)}>
                    <span>Tools</span>
                    {caret(openTools)}
                </div>
                {openTools && (
                    <div style={styles.submenu}>
                        <NavLink to="/api-designer" style={linkStyle}>Master Data Management</NavLink>
                    </div>
                )}*/}

                <div style={styles.logoutSection}>
                    <button style={styles.logoutBtn} onClick={handleLogout}>
                        🚪 Logout
                    </button>
                </div>
            </div>
        </aside>
    )
}

const styles = {
    aside: {
        width: '260px',
        minWidth: '220px',
        height: '100%',
        background: '#111827',
        borderRight: '1px solid #e5e7eb',
        boxSizing: 'border-box',
        overflowY: 'auto'
    },
    header: {
        padding: '12px 16px',
        fontWeight: 700,
        color: '#ffffff',
        background: '#374151',
        borderBottom: '1px solid #e5e7eb'
    },
    submenu: {
        paddingLeft: '4px',
        marginBottom: '4px'
    },
    logoutSection: {
        marginTop: 'auto',
        padding: '16px 8px 8px'
    },
    logoutBtn: {
        width: '100%',
        padding: '8px 12px',
        border: '1px solid #dc2626',
        borderRadius: '6px',
        background: '#121a31',
        color: '#a9b0c6',
        cursor: 'pointer',
        fontSize: '0.875rem',
        fontWeight: 500
    }
}
