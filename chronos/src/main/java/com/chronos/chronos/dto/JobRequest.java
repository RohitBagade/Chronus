package com.chronos.chronos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class JobRequest {

    @NotBlank
    private String jobType;

    @NotBlank
    private String command;

    @NotNull
    private LocalDateTime scheduleTime;

    private String recurrence; // optional: daily, weekly, etc.

    private String filePath;
}
