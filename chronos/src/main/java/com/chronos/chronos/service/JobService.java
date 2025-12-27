package com.chronos.chronos.service;

import com.chronos.chronos.dto.JobRequest;
import com.chronos.chronos.dto.JobResponse;

import java.util.List;

public interface JobService {
    JobResponse createJob(JobRequest request);
    List<JobResponse> getAllJobs();
    JobResponse getJobById(Long id);
    void deleteJob(Long id);
}

