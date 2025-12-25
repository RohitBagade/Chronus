package com.chronos.chronos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class JobResponse {
    private Long jobId;
    private String jobType;
    private String command;
    private LocalDateTime scheduleTime;
    private String recurrence;
    private String status;
    private String filePath;
}
