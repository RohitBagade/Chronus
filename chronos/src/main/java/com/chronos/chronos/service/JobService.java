package com.chronos.chronos.service;

import com.chronos.chronos.dto.JobRequest;
import com.chronos.chronos.dto.JobResponse;
import com.chronos.chronos.dto.RescheduleRequest;

import java.util.List;

public interface JobService {
    JobResponse createJob(JobRequest request, String userEmail);
    List<JobResponse> getAllJobs();
    JobResponse getJobById(Long id);
    JobResponse reschedule(Long id, RescheduleRequest request);
    void cancelJob(Long id);
}
