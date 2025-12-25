package com.chronos.chronos.service;

import com.chronos.chronos.dto.JobRequest;
import com.chronos.chronos.dto.JobResponse;

public interface JobService {
    JobResponse createJob(JobRequest request);
    JobResponse getJobById(Long id);
    void deleteJob(Long id);
}

