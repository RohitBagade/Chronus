import React from 'react'

export default function JobDetailsModal({ show = false, job = null, onClose = () => {} }) {
  if (!show || !job) return null

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg max-w-xl w-full p-4">
        <div className="flex justify-between items-center mb-2">
          <h3 className="text-lg font-semibold">Job Details</h3>
          <button onClick={onClose} className="text-gray-600">Close</button>
        </div>
        <div className="space-y-2 text-sm text-gray-700">
          <div><strong>ID:</strong> {job.id}</div>
          <div><strong>Name:</strong> {job.name}</div>
          <div><strong>Status:</strong> {job.status}</div>
          <div><strong>Created:</strong> {job.createdAt || 'N/A'}</div>
          <div><strong>Description:</strong> {job.description || '—'}</div>
        </div>
      </div>
    </div>
  )
}

