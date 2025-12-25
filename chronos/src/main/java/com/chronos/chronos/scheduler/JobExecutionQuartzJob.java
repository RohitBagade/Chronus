package com.chronos.chronos.scheduler;

import com.chronos.chronos.entity.Job;
import com.chronos.chronos.repository.JobRepository;
import com.chronos.chronos.service.JobExecutionService;
import lombok.RequiredArgsConstructor;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JobExecutionQuartzJob extends QuartzJobBean {

    private final JobRepository jobRepository;
    private final JobExecutionService jobExecutionService;

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        Long jobId = context.getMergedJobDataMap().getLong("jobId");
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new JobExecutionException("Job not found: " + jobId));

        System.out.println(">>> Quartz triggered job " + jobId);
        jobExecutionService.executeJob(job);
    }
}
