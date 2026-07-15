package com.chronos.chronos.controller;

import com.chronos.chronos.dto.JobRequest;
import com.chronos.chronos.dto.JobResponse;
import com.chronos.chronos.dto.RescheduleRequest;
import com.chronos.chronos.service.JobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    @PostMapping
    public ResponseEntity<JobResponse> createJob(@Valid @RequestBody JobRequest request, Authentication auth) {
        return ResponseEntity.ok(jobService.createJob(request, auth.getName()));
    }

    @GetMapping
    public List<JobResponse> getAllJobs() {
        return jobService.getAllJobs();
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobResponse> getJob(@PathVariable Long id) {
        return ResponseEntity.ok(jobService.getJobById(id));
    }

    @PutMapping("/{id}/reschedule")
    public ResponseEntity<JobResponse> reschedule(@PathVariable Long id,
                                                  @Valid @RequestBody RescheduleRequest request) {
        return ResponseEntity.ok(jobService.reschedule(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> cancelJob(@PathVariable Long id) {
        jobService.cancelJob(id);
        return ResponseEntity.ok(Map.of("status", "cancelled", "jobId", String.valueOf(id)));
    }
}
