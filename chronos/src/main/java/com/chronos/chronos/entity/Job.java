package com.chronos.chronos.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "jobs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Job {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long jobId;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private String jobType;
    private String command;
    private LocalDateTime scheduleTime;
    private String recurrence;
    private String status;
    private String filePath;

    // Retry bookkeeping — drives exponential-backoff re-scheduling on failure.
    @Builder.Default
    private int attemptCount = 0;
    @Builder.Default
    private int maxAttempts = 3;
}
