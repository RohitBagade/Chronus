package com.chronos.chronos.service;

import com.chronos.chronos.dto.JobExecutionResponse;
import com.chronos.chronos.entity.Job;
import com.chronos.chronos.entity.JobExecution;
import com.chronos.chronos.repository.JobExecutionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

@Service
@RequiredArgsConstructor
public class JobExecutionServiceImpl implements JobExecutionService {

    private final JobExecutionRepository executionRepository;

    @Override
    public void logExecution(Job job, String status, String errorLog) {
        JobExecution execution = JobExecution.builder()
                .job(job)
                .startTime(LocalDateTime.now())
                .endTime(LocalDateTime.now()) // for now, same time; will adjust with real execution
                .status(status)
                .errorLog(errorLog)
                .build();
        executionRepository.save(execution);
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
                        .build()
                )
                .collect(Collectors.toList());
    }

    @Override
    public void executeJob(Job job) {
        LocalDateTime start = LocalDateTime.now();
        String status;
        String error = null;

        Path logFilePath = Path.of("job-logs/job-" + job.getJobId() + ".log");

        try {
            if ("run_java_code".equals(job.getCommand())) {
                System.out.println(">>> Compiling and running Java file for job " + job.getJobId());

                File javaFile = new File(System.getProperty("user.dir"), job.getFilePath());

                if (!javaFile.exists()) {
                    throw new IllegalArgumentException("Java file not found: " + javaFile.getAbsolutePath());
                }

                // Step 1: Compile
                Process compile = new ProcessBuilder("javac", javaFile.getAbsolutePath())
                        .redirectErrorStream(true)
                        .start();
                String compileOutput = new String(compile.getInputStream().readAllBytes());
                compile.waitFor();

                if (compile.exitValue() != 0) {
                    throw new RuntimeException("Compilation failed:\n" + compileOutput);
                }

                // Step 2: Run class
                String className = javaFile.getName().replace(".java", "");
                Process run = new ProcessBuilder("java", "-cp", javaFile.getParent(), className)
                        .redirectErrorStream(true)
                        .start();
                String output = new String(run.getInputStream().readAllBytes());
                run.waitFor();

                if (run.exitValue() != 0) {
                    throw new RuntimeException("Execution failed:\n" + output);
                }

                // ✅ Write logs for success
                Files.createDirectories(Path.of("job-logs"));
                Files.writeString(logFilePath,
                        "=== Execution started at " + start + " ===\n",
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                Files.writeString(logFilePath,
                        output + System.lineSeparator(),
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                Files.writeString(logFilePath,
                        "=== Execution ended at " + LocalDateTime.now() + " ===\n\n",
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);

                status = "success";
                error = null;

                System.out.println(">>> Job " + job.getJobId() + " executed successfully");

            } else {
                throw new IllegalArgumentException("Unsupported command: " + job.getCommand());
            }

        } catch (Exception e) {
            status = "failure";
            error = e.getMessage();
            System.err.println(">>> Job " + job.getJobId() + " failed: " + error);

            try {
                // ✅ Always log errors too
                Files.createDirectories(Path.of("job-logs"));
                Files.writeString(logFilePath,
                        "=== Execution started at " + start + " ===\n",
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                Files.writeString(logFilePath,
                        "ERROR: " + error + System.lineSeparator(),
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                Files.writeString(logFilePath,
                        "=== Execution ended at " + LocalDateTime.now() + " ===\n\n",
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException ioEx) {
                System.err.println(">>> Failed to write log file: " + ioEx.getMessage());
            }
        }

        // ✅ Always save execution record
        JobExecution execution = JobExecution.builder()
                .job(job)
                .startTime(start)
                .endTime(LocalDateTime.now())
                .status(status)
                .errorLog(error)
                .build();

        executionRepository.save(execution);
    }

}
