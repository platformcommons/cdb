import React from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import App from './App.jsx'
import './styles.css'

// Determine basename from Vite BASE_URL and normalize (no trailing slash)
const rawBase = import.meta.env.BASE_URL || '/'
const basename = rawBase === '/' ? undefined : rawBase.replace(/\/$/, '')

createRoot(document.getElementById('root')).render(
  <BrowserRouter basename={basename}>
    <App />
  </BrowserRouter>
)
