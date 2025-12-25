package com.chronos.chronos.controller;

import com.chronos.chronos.entity.Job;
import com.chronos.chronos.repository.JobRepository;
import com.chronos.chronos.service.JobExecutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class JobExecutionTestController {

    private final JobRepository jobRepository;
    private final JobExecutionService jobExecutionService;

//    // Trigger a fake execution for testing
//    @PostMapping("/jobs/{id}/execute")
//    public ResponseEntity<String> executeJob(@PathVariable Long id) {
//        Job job = jobRepository.findById(id)
//                .orElseThrow(() -> new IllegalArgumentException("Job with id " + id + " not found"));
//
//        // simulate success execution
//        jobExecutionService.logExecution(job, "success", null);
//
//        return ResponseEntity.ok("Execution logged for job " + id);
//    }

    @PostMapping("/jobs/{id}/execute")
    public ResponseEntity<String> executeJob(@PathVariable Long id) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Job with id " + id + " not found"));

        jobExecutionService.executeJob(job);

        return ResponseEntity.ok("Job " + id + " executed");
    }

}
