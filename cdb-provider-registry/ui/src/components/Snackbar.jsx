import React, { useEffect } from 'react'

export default function Snackbar({ message, type = 'success', onClose, duration = 4000 }) {
  useEffect(() => {
    if (message) {
      const timer = setTimeout(onClose, duration)
      return () => clearTimeout(timer)
    }
  }, [message, onClose, duration])

  if (!message) return null

  return (
    <div className={`snackbar snackbar-${type}`}>
      <span>{message}</span>
      <button onClick={onClose} className="snackbar-close">×</button>
    </div>
  )
}