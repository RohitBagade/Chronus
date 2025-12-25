package com.chronos.chronos.controller;

import com.chronos.chronos.dto.JobRequest;
import com.chronos.chronos.dto.JobResponse;
import com.chronos.chronos.service.JobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    @PostMapping
    public ResponseEntity<JobResponse> createJob(@Valid @RequestBody JobRequest request) {
        return ResponseEntity.ok(jobService.createJob(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobResponse> getJob(@PathVariable Long id) {
        return ResponseEntity.ok(jobService.getJobById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteJob(@PathVariable Long id) {
        jobService.deleteJob(id);
        Map<String, String> response = new HashMap<>();
        response.put("status", "deleted");
        response.put("jobId", String.valueOf(id));
        return ResponseEntity.ok(response);
    }
}
