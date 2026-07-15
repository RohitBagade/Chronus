import axios from "axios";

const BASE = import.meta.env.VITE_API_URL || "http://localhost:8080";

export const http = axios.create({ baseURL: BASE });

// Attach the JWT to every request.
http.interceptors.request.use((config) => {
  const token = localStorage.getItem("chronos_token");
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// Surface a clean message from the backend's error envelope.
function err(e) {
  return e?.response?.data?.message || e?.response?.data?.error || e.message || "Request failed";
}

export const api = {
  signup: (body) => http.post("/auth/signup", body).then((r) => r.data).catch((e) => Promise.reject(err(e))),
  login: (body) => http.post("/auth/login", body).then((r) => r.data).catch((e) => Promise.reject(err(e))),
  me: () => http.get("/auth/me").then((r) => r.data),

  listJobs: () => http.get("/jobs").then((r) => r.data),
  getJob: (id) => http.get(`/jobs/${id}`).then((r) => r.data),
  createJob: (body) => http.post("/jobs", body).then((r) => r.data).catch((e) => Promise.reject(err(e))),
  reschedule: (id, body) => http.put(`/jobs/${id}/reschedule`, body).then((r) => r.data).catch((e) => Promise.reject(err(e))),
  cancelJob: (id) => http.delete(`/jobs/${id}`).then((r) => r.data),
  jobLogs: (id) => http.get(`/jobs/${id}/logs`).then((r) => r.data).catch(() => []),
};
