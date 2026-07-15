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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Runs a job and records the outcome. On failure it retries with exponential backoff
 * (base 5s, doubling per attempt) up to Job.maxAttempts, re-scheduling via Quartz.
 *
 * Supported commands (routed by prefix in {@link #runCommand}):
 *   noop / log:*      -> succeeds (demo)
 *   fail*             -> throws (demonstrates retry)
 *   http:<url>        -> HTTP GET; non-2xx = failure. SSRF-guarded.
 *   shell:<cmd>       -> runs a shell command; disabled unless chronos.jobs.shell-enabled=true (RCE guard)
 *   run_java_code     -> compiles + runs an uploaded .java file
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

    // Shell execution is arbitrary code — off by default so the public demo can't be used for RCE.
    @Value("${chronos.jobs.shell-enabled:false}")
    private boolean shellEnabled;

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

        recordExecution(job, start, JobStatus.SUCCESS, truncate(output));
        job.setStatus(isRecurring(job) ? JobStatus.SCHEDULED : JobStatus.SUCCESS);
        jobRepository.save(job);
        log.info("Job {} succeeded: {}", job.getJobId(), truncate(output));
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

    private void recordExecution(Job job, LocalDateTime start, String status, String message) {
        executionRepository.save(JobExecution.builder()
                .job(job)
                .startTime(start)
                .endTime(LocalDateTime.now())
                .status(status)
                .errorLog(message)
                .build());
    }

    private boolean isRecurring(Job job) {
        return job.getRecurrence() != null && !job.getRecurrence().isBlank();
    }

    private String truncate(String s) {
        if (s == null) return null;
        return s.length() > 1800 ? s.substring(0, 1800) + "…" : s;
    }

    /** Route a job's command to its executor by prefix. */
    private String runCommand(Job job) throws Exception {
        String command = job.getCommand() == null ? "" : job.getCommand().trim();

        if (command.startsWith("fail")) {
            throw new RuntimeException("Simulated failure for command '" + command + "'");
        }
        if (command.startsWith("http:")) {
            return runHttp(command.substring("http:".length()));
        }
        if (command.startsWith("shell:")) {
            return runShell(command.substring("shell:".length()));
        }
        if ("run_java_code".equals(command)) {
            return runJavaFile(job);
        }

        String message = command.isEmpty() ? "noop" : command;
        log.info("Job {} executed command: {}", job.getJobId(), message);
        return "Executed: " + message;
    }

    /** HTTP GET a URL. Non-2xx is a failure (so it retries). Guards against SSRF to internal hosts. */
    private String runHttp(String raw) throws Exception {
        String url = raw.trim();
        if (!url.startsWith("http://") && !url.startsWith("https://")) url = "https://" + url;
        URI uri = URI.create(url);
        String host = uri.getHost();
        if (host == null || isBlockedHost(host)) {
            throw new IllegalArgumentException("Refusing to call internal/invalid host: " + host);
        }
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(10)).GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        int code = response.statusCode();
        if (code < 200 || code >= 300) {
            throw new RuntimeException("HTTP " + code + " from " + host);
        }
        return "HTTP " + code + " · " + host + " · " + response.body().length() + " bytes";
    }

    /** Run a shell command. Gated behind chronos.jobs.shell-enabled (arbitrary code execution). */
    private String runShell(String cmd) throws Exception {
        if (!shellEnabled) {
            throw new IllegalStateException("Shell jobs are disabled. Set chronos.jobs.shell-enabled=true to allow.");
        }
        boolean windows = System.getProperty("os.name", "").toLowerCase().contains("win");
        ProcessBuilder pb = windows
                ? new ProcessBuilder("cmd.exe", "/c", cmd)
                : new ProcessBuilder("sh", "-c", cmd);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        String out = new String(process.getInputStream().readAllBytes());
        if (!process.waitFor(30, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new RuntimeException("Shell command timed out after 30s");
        }
        if (process.exitValue() != 0) {
            throw new RuntimeException("Exit " + process.exitValue() + ": " + out.strip());
        }
        return out.strip();
    }

    /** Block obvious internal/private targets. Not full SSRF protection (no DNS resolution) — a demo-grade guard. */
    private boolean isBlockedHost(String host) {
        String h = host.toLowerCase();
        if (h.equals("localhost") || h.endsWith(".internal") || h.endsWith(".local")) return true;
        if (h.equals("0.0.0.0") || h.equals("::1")) return true;
        if (h.startsWith("127.") || h.startsWith("10.") || h.startsWith("192.168.") || h.startsWith("169.254.")) return true;
        if (h.startsWith("172.")) {
            try {
                int octet = Integer.parseInt(h.split("\\.")[1]);
                if (octet >= 16 && octet <= 31) return true;
            } catch (Exception ignored) { }
        }
        return false;
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
