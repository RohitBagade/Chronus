package com.chronos.chronos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@Builder
public class JobExecutionResponse {
    private Long executionId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private String message;
}
