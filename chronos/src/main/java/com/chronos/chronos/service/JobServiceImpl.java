package com.chronos.chronos.service;

import com.chronos.chronos.dto.JobRequest;
import com.chronos.chronos.dto.JobResponse;
import com.chronos.chronos.entity.Job;
import com.chronos.chronos.repository.JobRepository;
import com.chronos.chronos.scheduler.JobSchedulerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class JobServiceImpl implements JobService {

    private final JobRepository jobRepository;

    @Autowired
    private JobSchedulerService jobSchedulerService;

    public JobServiceImpl(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    @Override
    public JobResponse createJob(JobRequest request) {
        Job job = Job.builder()
                .jobType(request.getJobType())
                .command(request.getCommand())
                .scheduleTime(request.getScheduleTime())
                .recurrence(request.getRecurrence())
                .status("SCHEDULED")
                .filePath(request.getFilePath())
                .build();

        Job saved = jobRepository.save(job);

        jobSchedulerService.scheduleJob(saved);

        return convertToResponse(saved);
    }

    @Override
    public List<JobResponse> getAllJobs() {
        return jobRepository.findAll()
                .stream()
                .map(job -> JobResponse.builder()
                        .jobId(job.getJobId())
                        .jobType(job.getJobType())
                        .command(job.getCommand())
                        .scheduleTime(job.getScheduleTime())
                        .status(job.getStatus())
                        .filePath(job.getFilePath())
                        .build())
                .toList();
    }


    @Override
    public JobResponse getJobById(Long id) {
        return jobRepository.findById(id)
                .map(this::convertToResponse)
                .orElseThrow(() -> new RuntimeException("Job not found"));
    }

    @Override
    public void deleteJob(Long id) {
        jobRepository.deleteById(id);
    }

    private JobResponse convertToResponse(Job job) {
        return JobResponse.builder()
                .jobId(job.getJobId())
                .jobType(job.getJobType())
                .command(job.getCommand())
                .scheduleTime(job.getScheduleTime())
                .recurrence(job.getRecurrence())
                .status(job.getStatus())
                .build();
    }
}
