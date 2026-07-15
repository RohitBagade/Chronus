package com.chronos.chronos.service;

import com.chronos.chronos.dto.JobRequest;
import com.chronos.chronos.dto.JobResponse;
import com.chronos.chronos.dto.RescheduleRequest;
import com.chronos.chronos.entity.Job;
import com.chronos.chronos.entity.User;
import com.chronos.chronos.repository.JobRepository;
import com.chronos.chronos.repository.UserRepository;
import com.chronos.chronos.scheduler.JobSchedulerService;
import com.chronos.chronos.scheduler.JobStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class JobServiceImpl implements JobService {

    private final JobRepository jobRepository;
    private final JobSchedulerService jobSchedulerService;
    private final UserRepository userRepository;

    public JobServiceImpl(JobRepository jobRepository,
                          JobSchedulerService jobSchedulerService,
                          UserRepository userRepository) {
        this.jobRepository = jobRepository;
        this.jobSchedulerService = jobSchedulerService;
        this.userRepository = userRepository;
    }

    @Override
    public JobResponse createJob(JobRequest request, String userEmail) {
        User owner = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("Owner not found: " + userEmail));

        Job job = Job.builder()
                .user(owner)
                .jobType(request.getJobType())
                .command(request.getCommand())
                .scheduleTime(request.getScheduleTime())
                .recurrence(request.getRecurrence())
                .status(JobStatus.SCHEDULED)
                .filePath(request.getFilePath())
                .attemptCount(0)
                .maxAttempts(3)
                .build();

        Job saved = jobRepository.save(job);
        jobSchedulerService.scheduleJob(saved);
        return toResponse(saved);
    }

    @Override
    public List<JobResponse> getAllJobs() {
        return jobRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    public JobResponse getJobById(Long id) {
        return jobRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + id));
    }

    @Override
    public JobResponse reschedule(Long id, RescheduleRequest request) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + id));
        job.setScheduleTime(request.getScheduleTime());
        job.setRecurrence(request.getRecurrence());
        job.setAttemptCount(0);
        job.setStatus(JobStatus.SCHEDULED);
        Job saved = jobRepository.save(job);
        jobSchedulerService.scheduleJob(saved); // deletes + recreates the Quartz trigger
        return toResponse(saved);
    }

    @Override
    public void cancelJob(Long id) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + id));
        jobSchedulerService.unschedule(id);
        job.setStatus(JobStatus.CANCELLED);
        jobRepository.save(job); // keep the record (audit trail), just cancel it
    }

    private JobResponse toResponse(Job job) {
        return JobResponse.builder()
                .jobId(job.getJobId())
                .jobType(job.getJobType())
                .command(job.getCommand())
                .scheduleTime(job.getScheduleTime())
                .recurrence(job.getRecurrence())
                .status(job.getStatus())
                .filePath(job.getFilePath())
                .attemptCount(job.getAttemptCount())
                .maxAttempts(job.getMaxAttempts())
                .build();
    }
}
