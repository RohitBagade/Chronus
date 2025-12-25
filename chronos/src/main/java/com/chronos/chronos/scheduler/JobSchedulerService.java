package com.chronos.chronos.scheduler;

import com.chronos.chronos.entity.Job;
import lombok.RequiredArgsConstructor;
import org.quartz.*;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class JobSchedulerService {

    private final SchedulerFactoryBean schedulerFactoryBean;

    public void scheduleJob(Job job) {
        try {
            Scheduler scheduler = schedulerFactoryBean.getScheduler();

            JobDetail jobDetail = JobBuilder.newJob(JobExecutionQuartzJob.class)
                    .withIdentity("job-" + job.getJobId(), "jobs")
                    .usingJobData("jobId", job.getJobId())
                    .build();

            Date triggerTime1 = Date.from(job.getScheduleTime()
                    .atZone(ZoneId.systemDefault())
                    .toInstant());

            Date triggerTime = Date.from(
                    job.getScheduleTime()
                            .atZone(ZoneId.systemDefault())
                            .plusSeconds(35) // add a small 5s buffer
                            .toInstant()
            );

            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity("trigger-" + job.getJobId(), "jobs")
                    .startAt(triggerTime)
                    .build();

            scheduler.scheduleJob(jobDetail, trigger);

            System.out.println(">>> Job " + job.getJobId() + " scheduled for " + job.getScheduleTime());

        } catch (SchedulerException e) {
            throw new RuntimeException("Failed to schedule job: " + e.getMessage(), e);
        }
    }
}
