package com.chronos.chronos.service;

import com.chronos.chronos.dto.JobExecutionResponse;
import com.chronos.chronos.entity.Job;

import java.util.List;

public interface JobExecutionService {
    void logExecution(Job job, String status, String errorLog);

    List<JobExecutionResponse> getExecutionsForJob(Job job);

    void executeJob(Job job);
}
