package com.chronos.chronos.controller;

import com.chronos.chronos.dto.JobExecutionResponse;
import com.chronos.chronos.entity.Job;
import com.chronos.chronos.service.JobExecutionService;
import com.chronos.chronos.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/jobs")
@RequiredArgsConstructor
public class JobExecutionController {

    private final JobExecutionService jobExecutionService;
    private final JobRepository jobRepository;

    @GetMapping("/{id}/logs")
    public ResponseEntity<List<JobExecutionResponse>> getJobLogs(@PathVariable Long id) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Job not found"));

        List<JobExecutionResponse> logs = jobExecutionService.getExecutionsForJob(job);
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/{id}/logs/content")
    public ResponseEntity<String> getJobLogContent(@PathVariable Long id) throws IOException {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Job not found"));

        Path logFilePath = Path.of("job-logs/job-" + job.getJobId() + ".log");

        if (!Files.exists(logFilePath)) {
            return ResponseEntity.ok("No logs found for job " + id);
        }

        String content = Files.readString(logFilePath);
        return ResponseEntity.ok(content);
    }

}
