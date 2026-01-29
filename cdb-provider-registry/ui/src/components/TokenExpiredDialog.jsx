import React from 'react'

export default function TokenExpiredDialog({ isOpen, onConfirm }) {
  if (!isOpen) return null

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg p-6 max-w-sm mx-4">
        <h3 className="text-lg font-semibold mb-4">Session Expired</h3>
        <p className="text-gray-600 mb-6">Your session has expired. Please log in again.</p>
        <button
          onClick={onConfirm}
          className="w-full bg-blue-600 text-white py-2 px-4 rounded hover:bg-blue-700"
        >
          OK
        </button>
      </div>
    </div>
  )
}