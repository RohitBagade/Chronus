package com.chronos.chronos.scheduler;

import com.chronos.chronos.entity.Job;
import lombok.RequiredArgsConstructor;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.util.Date;

/**
 * Owns all Quartz interaction: schedule (one-time or recurring), reschedule, cancel, and
 * retry (a delayed one-shot trigger with exponential backoff).
 */
@Service
@RequiredArgsConstructor
public class JobSchedulerService {

    private static final Logger log = LoggerFactory.getLogger(JobSchedulerService.class);
    private static final String GROUP = "jobs";

    private final SchedulerFactoryBean schedulerFactoryBean;

    private JobKey jobKey(Long id) { return JobKey.jobKey("job-" + id, GROUP); }
    private TriggerKey triggerKey(Long id) { return TriggerKey.triggerKey("trigger-" + id, GROUP); }

    /** Schedule a job at its scheduleTime; if it has a recurrence, repeat on that interval. */
    public void scheduleJob(Job job) {
        try {
            Scheduler scheduler = schedulerFactoryBean.getScheduler();
            // Replace any existing definition (idempotent for reschedule).
            scheduler.deleteJob(jobKey(job.getJobId()));

            JobDetail jobDetail = JobBuilder.newJob(JobExecutionQuartzJob.class)
                    .withIdentity(jobKey(job.getJobId()))
                    .usingJobData("jobId", job.getJobId())
                    .storeDurably(false)
                    .build();

            Date startAt = Date.from(job.getScheduleTime().atZone(ZoneId.systemDefault()).toInstant());

            TriggerBuilder<Trigger> tb = TriggerBuilder.newTrigger()
                    .withIdentity(triggerKey(job.getJobId()))
                    .startAt(startAt);

            Long intervalSeconds = recurrenceToSeconds(job.getRecurrence());
            if (intervalSeconds != null) {
                tb.withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInSeconds(intervalSeconds.intValue())
                        .repeatForever());
            }

            scheduler.scheduleJob(jobDetail, tb.build());
            log.info("Scheduled job {} at {} (recurrence={})", job.getJobId(), job.getScheduleTime(), job.getRecurrence());
        } catch (SchedulerException e) {
            throw new RuntimeException("Failed to schedule job " + job.getJobId() + ": " + e.getMessage(), e);
        }
    }

    /** Fire this job once more after delaySeconds — used by the retry/backoff path. */
    public void scheduleRetry(Long jobId, long delaySeconds) {
        try {
            Scheduler scheduler = schedulerFactoryBean.getScheduler();
            JobDetail jobDetail = JobBuilder.newJob(JobExecutionQuartzJob.class)
                    .withIdentity("retry-job-" + jobId + "-" + System.nanoTime(), GROUP)
                    .usingJobData("jobId", jobId)
                    .build();
            Trigger trigger = TriggerBuilder.newTrigger()
                    .startAt(new Date(System.currentTimeMillis() + delaySeconds * 1000))
                    .build();
            scheduler.scheduleJob(jobDetail, trigger);
            log.info("Scheduled retry for job {} in {}s", jobId, delaySeconds);
        } catch (SchedulerException e) {
            throw new RuntimeException("Failed to schedule retry for job " + jobId, e);
        }
    }

    /** Remove a job's trigger + definition (cancel/delete). */
    public void unschedule(Long jobId) {
        try {
            schedulerFactoryBean.getScheduler().deleteJob(jobKey(jobId));
        } catch (SchedulerException e) {
            throw new RuntimeException("Failed to unschedule job " + jobId, e);
        }
    }

    /** Named recurrence -> repeat interval in seconds, or null for a one-time job. */
    private Long recurrenceToSeconds(String recurrence) {
        if (recurrence == null || recurrence.isBlank()) return null;
        return switch (recurrence.trim().toLowerCase()) {
            case "minutely" -> 60L;
            case "hourly" -> 3600L;
            case "daily" -> 86400L;
            case "weekly" -> 604800L;
            default -> null; // unknown -> treat as one-time
        };
    }
}
