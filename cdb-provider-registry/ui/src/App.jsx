import React, { useState, useEffect } from 'react'
import { Routes, Route, Navigate } from 'react-router-dom'
import Navbar from './components/Navbar.jsx'
import Landing from './pages/Landing.jsx'
import Login from './pages/Login.jsx'
import Signup from './pages/Signup.jsx'
import Keys from './pages/Keys.jsx'
import AddKey from './pages/AddKey.jsx'
import ManageUsers from './pages/ManageUsers.jsx'
import Dashboard from './pages/Dashboard.jsx'
import Apis from './pages/Apis.jsx'
import Configuration from './pages/Configuration.jsx'
import Sandbox from './pages/Sandbox.jsx'
import NoProvider from './pages/NoProvider.jsx'
import ApiDesignerPage from './pages/ApiDesignerPage.jsx'
import ApiDesigner from './pages/ApiDesigner.jsx'
import { getCurrentUser, logout } from './services/authService'
import AuthLayout from './layouts/AuthLayout.jsx'
import TokenExpiredDialog from './components/TokenExpiredDialog.jsx'
import { setTokenExpiredHandler } from './services/api'

import ApiEdit from './pages/ApiEdit.jsx'
import ApiView from './pages/ApiView.jsx'
import ApiCreate from './pages/ApiCreate.jsx'
import AddConfiguration from './pages/AddConfiguration.jsx'
import EditConfiguration from './pages/EditConfiguration.jsx'
import AuthCallbackPage from './pages/AuthCallbackPage.jsx'

function PrivateRoute({ children }) {
  const user = getCurrentUser()
  return user ? children : <Navigate to="/login" replace />
}

export default function App() {
  const [user, setUser] = useState(getCurrentUser())
  const [showTokenExpiredDialog, setShowTokenExpiredDialog] = useState(false)
  
  useEffect(() => {
    const checkAuth = () => setUser(getCurrentUser())
    window.addEventListener('storage', checkAuth)
    
    setTokenExpiredHandler(() => {
      setShowTokenExpiredDialog(true)
    })
    
    return () => window.removeEventListener('storage', checkAuth)
  }, [])
  
  window.refreshAuth = () => setUser(getCurrentUser())
  
  const handleTokenExpiredConfirm = () => {
    setShowTokenExpiredDialog(false)
    logout()
    setUser(null)
  }
  
  return (
    <div className="app-container">
      {!user && <Navbar />}
      {!user && (
        <main className="container">
          <Routes>
            <Route path="/" element={<Login />} />
            <Route path="/login" element={<Login />} />
            <Route path="/signup" element={<Signup />} />
            <Route path="/no-provider" element={<NoProvider />} />
            <Route path="/auth/callback" element={<AuthCallbackPage />} />
          </Routes>
        </main>
      )}
      {user && (
        <Routes>
          <Route path="/dashboard" element={<PrivateRoute><AuthLayout><Dashboard /></AuthLayout></PrivateRoute>} />
          <Route path="/app" element={<Navigate to="/dashboard" replace />} />
          <Route path="/apis" element={<PrivateRoute><AuthLayout><Apis /></AuthLayout></PrivateRoute>} />
          <Route path="/apis/new" element={<PrivateRoute><AuthLayout><ApiCreate /></AuthLayout></PrivateRoute>} />
          <Route path="/apis/:id" element={<PrivateRoute><AuthLayout><ApiView /></AuthLayout></PrivateRoute>} />
          <Route path="/apis/:id/edit" element={<PrivateRoute><AuthLayout><ApiEdit /></AuthLayout></PrivateRoute>} />
          <Route path="/configuration" element={<PrivateRoute><AuthLayout><Configuration /></AuthLayout></PrivateRoute>} />
          <Route path="/configuration/new" element={<PrivateRoute><AuthLayout><AddConfiguration /></AuthLayout></PrivateRoute>} />
          <Route path="/configuration/:id/edit" element={<PrivateRoute><AuthLayout><EditConfiguration /></AuthLayout></PrivateRoute>} />
          <Route path="/sandbox" element={<PrivateRoute><AuthLayout><Sandbox /></AuthLayout></PrivateRoute>} />
          <Route path="/keys" element={<PrivateRoute><AuthLayout><Keys /></AuthLayout></PrivateRoute>} />
          <Route path="/keys/new" element={<PrivateRoute><AuthLayout><AddKey /></AuthLayout></PrivateRoute>} />
          <Route path="/manage-users" element={<PrivateRoute><AuthLayout><ManageUsers /></AuthLayout></PrivateRoute>} />
          <Route path="/api-designer" element={<PrivateRoute><AuthLayout><ApiDesigner /></AuthLayout></PrivateRoute>} />
          <Route path="/no-provider" element={<PrivateRoute><NoProvider /></PrivateRoute>} />
          <Route path="/auth/callback" element={<AuthCallbackPage />} />
          <Route path="/" element={<Navigate to="/dashboard" replace />} />
          <Route path="/login" element={<Navigate to="/dashboard" replace />} />
          <Route path="/signup" element={<Navigate to="/dashboard" replace />} />
        </Routes>
      )}
      <TokenExpiredDialog 
        isOpen={showTokenExpiredDialog} 
        onConfirm={handleTokenExpiredConfirm} 
      />
    </div>
  )
}
