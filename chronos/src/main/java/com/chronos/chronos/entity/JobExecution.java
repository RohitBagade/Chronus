package com.chronos.chronos.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "job_executions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long executionId;

    @ManyToOne
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;    // success / failure
    @Column(length = 2000)
    private String errorLog;  // store error details
}
