package com.chronos.chronos.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RescheduleRequest {
    @NotNull
    private LocalDateTime scheduleTime;
    private String recurrence; // optional: minutely, hourly, daily, weekly
}
