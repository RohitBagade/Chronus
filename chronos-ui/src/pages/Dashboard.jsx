import React, { useEffect, useState } from 'react'
import JobList from '../components/JobList'
import JobDetailsModal from '../components/JobDetailsModal'
import LogViewer from '../components/LogViewer'
import UploadJobForm from '../components/UploadJobForm'
import * as api from '../services/api'

export default function Dashboard() {
  const [jobs, setJobs] = useState([])
  const [selectedJob, setSelectedJob] = useState(null)
  const [showDetails, setShowDetails] = useState(false)
  const [showLogs, setShowLogs] = useState(false)
  const [logs, setLogs] = useState('')

  const loadJobs = async () => {
    try {
      const data = await api.getJobs()
      setJobs(data || [])
    } catch (e) {
      console.error('Failed to load jobs', e)
    }
  }

  useEffect(() => {
    loadJobs()
  }, [])

  const handleSelect = (job) => {
    setSelectedJob(job)
    setShowDetails(true)
  }

  const handleViewLogs = async (job) => {
    setSelectedJob(job)
    setShowLogs(true)
    try {
      const text = await api.getJobLogs(job.id)
      setLogs(text)
    } catch (e) {
      setLogs('Failed to load logs')
    }
  }

  const handleUpload = async (formData) => {
    await api.uploadJob(formData)
    await loadJobs()
  }

  return (
    <div className="min-h-screen bg-slate-50 p-6">
      <div className="max-w-5xl mx-auto grid grid-cols-3 gap-4">
        <div className="col-span-2">
          <JobList jobs={jobs} onSelect={handleSelect} onViewLogs={handleViewLogs} />
        </div>
        <div>
          <UploadJobForm onUpload={handleUpload} />
        </div>
      </div>

      <JobDetailsModal
        show={showDetails}
        job={selectedJob}
        onClose={() => setShowDetails(false)}
      />

      <LogViewer show={showLogs} logs={logs} onClose={() => setShowLogs(false)} />
    </div>
  )
}

