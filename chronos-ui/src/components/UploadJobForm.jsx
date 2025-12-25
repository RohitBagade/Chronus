import React, { useState } from 'react'

export default function UploadJobForm({ onUpload = () => {} }) {
  const [file, setFile] = useState(null)
  const [name, setName] = useState('')
  const [submitting, setSubmitting] = useState(false)

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (!file) return
    setSubmitting(true)
    const form = new FormData()
    form.append('file', file)
    form.append('name', name)
    try {
      await onUpload(form)
      setFile(null)
      setName('')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <form className="p-4 border rounded bg-white" onSubmit={handleSubmit}>
      <h3 className="font-semibold mb-2">Upload Job</h3>
      <div className="mb-2">
        <label className="block text-sm mb-1">Name</label>
        <input
          className="w-full border px-2 py-1 rounded"
          value={name}
          onChange={(e) => setName(e.target.value)}
          placeholder="Job name"
        />
      </div>
      <div className="mb-2">
        <label className="block text-sm mb-1">File</label>
        <input
          type="file"
          onChange={(e) => setFile(e.target.files?.[0] || null)}
        />
      </div>
      <div>
        <button
          type="submit"
          className="px-3 py-1 bg-blue-600 text-white rounded"
          disabled={submitting || !file}
        >
          {submitting ? 'Uploading...' : 'Upload'}
        </button>
      </div>
    </form>
  )
}

