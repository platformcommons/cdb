import React, { useEffect, useState } from 'react'
import { Link, NavLink, useNavigate } from 'react-router-dom'
import { getCurrentUser, logout as doLogout } from '../services/authService'

export default function Navbar() {
  const navigate = useNavigate()
  const [user, setUser] = useState(getCurrentUser())

  useEffect(() => {
    // Listen to storage changes (e.g., logout in another tab)
    const onStorage = (e) => {
      if (e.key === 'cdb.user') setUser(getCurrentUser())
    }
    window.addEventListener('storage', onStorage)
    return () => window.removeEventListener('storage', onStorage)
  }, [])

  function onLogout() {
    doLogout()
    setUser(null)
    navigate('/login', { replace: true })
  }

  return (
    <header className="navbar">
      <div className="navbar-brand">
        <Link to="/">CDB</Link>
      </div>
      <nav className="navbar-nav">
        {user && (
          <>
            <NavLink to="/dashboard">Dashboard</NavLink>
            <NavLink to="/manage-users">Manage Users</NavLink>
            <NavLink to="/keys">Keys</NavLink>
          </>
        )}
        {!user ? (
          <>
            <NavLink to="/login">Login</NavLink>
           {/* <NavLink to="/signup">Sign Up</NavLink>*/}
          </>
        ) : (
          <div className="nav-user">
            <span className="nav-username">{user.username}</span>
            <button className="nav-logout" onClick={onLogout}>Logout</button>
          </div>
        )}
      </nav>
    </header>
  )
}
