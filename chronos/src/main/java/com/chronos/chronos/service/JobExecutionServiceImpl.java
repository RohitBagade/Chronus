package com.chronos.chronos.service;

import com.chronos.chronos.dto.JobExecutionResponse;
import com.chronos.chronos.entity.Job;
import com.chronos.chronos.entity.JobExecution;
import com.chronos.chronos.repository.JobExecutionRepository;
import com.chronos.chronos.repository.JobRepository;
import com.chronos.chronos.scheduler.JobSchedulerService;
import com.chronos.chronos.scheduler.JobStatus;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Runs a job and records the outcome. On failure it retries with exponential backoff
 * (base 5s, doubling per attempt) up to Job.maxAttempts, re-scheduling via Quartz.
 */
@Service
@RequiredArgsConstructor
public class JobExecutionServiceImpl implements JobExecutionService {

    private static final Logger log = LoggerFactory.getLogger(JobExecutionServiceImpl.class);
    private static final long BASE_BACKOFF_SECONDS = 5;
    private static final long MAX_BACKOFF_SECONDS = 300;

    private final JobExecutionRepository executionRepository;
    private final JobRepository jobRepository;
    private final JobSchedulerService schedulerService;

    @Override
    public void logExecution(Job job, String status, String errorLog) {
        executionRepository.save(JobExecution.builder()
                .job(job)
                .startTime(LocalDateTime.now())
                .endTime(LocalDateTime.now())
                .status(status)
                .errorLog(errorLog)
                .build());
    }

    @Override
    public List<JobExecutionResponse> getExecutionsForJob(Job job) {
        return executionRepository.findByJob(job).stream()
                .map(ex -> JobExecutionResponse.builder()
                        .executionId(ex.getExecutionId())
                        .startTime(ex.getStartTime())
                        .endTime(ex.getEndTime())
                        .status(ex.getStatus())
                        .message(ex.getErrorLog() == null ? "Execution successful" : ex.getErrorLog())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public void executeJob(Job job) {
        LocalDateTime start = LocalDateTime.now();
        job.setStatus(JobStatus.RUNNING);
        jobRepository.save(job);

        String output;
        try {
            output = runCommand(job);
        } catch (Exception e) {
            recordExecution(job, start, JobStatus.FAILED, e.getMessage());
            handleFailure(job, e.getMessage());
            return;
        }

        recordExecution(job, start, JobStatus.SUCCESS, null);
        // A recurring job stays SCHEDULED for its next fire; a one-time job is done.
        job.setStatus(isRecurring(job) ? JobStatus.SCHEDULED : JobStatus.SUCCESS);
        jobRepository.save(job);
        log.info("Job {} succeeded: {}", job.getJobId(), output);
    }

    /** Retry with exponential backoff, or give up after maxAttempts. */
    private void handleFailure(Job job, String error) {
        int attempt = job.getAttemptCount() + 1;
        job.setAttemptCount(attempt);
        if (attempt < job.getMaxAttempts()) {
            long delay = Math.min(BASE_BACKOFF_SECONDS * (1L << (attempt - 1)), MAX_BACKOFF_SECONDS);
            job.setStatus(JobStatus.RETRYING);
            jobRepository.save(job);
            schedulerService.scheduleRetry(job.getJobId(), delay);
            log.warn("Job {} failed (attempt {}/{}): {} — retrying in {}s",
                    job.getJobId(), attempt, job.getMaxAttempts(), error, delay);
        } else {
            job.setStatus(JobStatus.FAILED);
            jobRepository.save(job);
            log.error("Job {} failed permanently after {} attempts: {}", job.getJobId(), attempt, error);
        }
    }

    private void recordExecution(Job job, LocalDateTime start, String status, String error) {
        executionRepository.save(JobExecution.builder()
                .job(job)
                .startTime(start)
                .endTime(LocalDateTime.now())
                .status(status)
                .errorLog(error)
                .build());
    }

    private boolean isRecurring(Job job) {
        return job.getRecurrence() != null && !job.getRecurrence().isBlank();
    }

    /**
     * Executes the job's command. Built-in demo commands make the scheduler runnable with no setup:
     *   noop / log:* / anything unknown -> success;  fail:* -> throws;  run_java_code -> compile+run a file.
     */
    private String runCommand(Job job) throws Exception {
        String command = job.getCommand() == null ? "" : job.getCommand().trim();

        if (command.startsWith("fail")) {
            throw new RuntimeException("Simulated failure for command '" + command + "'");
        }

        if ("run_java_code".equals(command)) {
            return runJavaFile(job);
        }

        // noop / log:<msg> / http:<url> / any other -> treated as a successful task for the demo.
        String message = command.isEmpty() ? "noop" : command;
        log.info("Job {} executed command: {}", job.getJobId(), message);
        return "Executed: " + message;
    }

    private String runJavaFile(Job job) throws Exception {
        File javaFile = new File(System.getProperty("user.dir"), job.getFilePath());
        if (!javaFile.exists()) {
            throw new IllegalArgumentException("Java file not found: " + javaFile.getAbsolutePath());
        }
        Process compile = new ProcessBuilder("javac", javaFile.getAbsolutePath())
                .redirectErrorStream(true).start();
        String compileOutput = new String(compile.getInputStream().readAllBytes());
        compile.waitFor();
        if (compile.exitValue() != 0) {
            throw new RuntimeException("Compilation failed:\n" + compileOutput);
        }
        String className = javaFile.getName().replace(".java", "");
        Process run = new ProcessBuilder("java", "-cp", javaFile.getParent(), className)
                .redirectErrorStream(true).start();
        String output = new String(run.getInputStream().readAllBytes());
        run.waitFor();
        if (run.exitValue() != 0) {
            throw new RuntimeException("Execution failed:\n" + output);
        }
        Path logDir = Path.of("job-logs");
        Files.createDirectories(logDir);
        Files.writeString(logDir.resolve("job-" + job.getJobId() + ".log"), output + System.lineSeparator());
        return output;
    }
}
