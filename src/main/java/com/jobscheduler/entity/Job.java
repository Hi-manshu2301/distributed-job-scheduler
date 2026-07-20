package com.jobscheduler.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "jobs")
@Getter
@Setter
public class Job {

    @Id
    @GeneratedValue
    private UUID id;

    // A logical job "type" the worker uses to decide how to execute it,
    // e.g. "SEND_EMAIL", "GENERATE_REPORT", "RESIZE_IMAGE"
    @Column(nullable = false)
    private String type;

    // Arbitrary JSON payload for the job, stored as text.
    // Kept as a raw string so the scheduler stays agnostic to job-specific shapes.
    @Column(columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus status = JobStatus.PENDING;

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    @Column(name = "max_retries", nullable = false)
    private int maxRetries = 3;

    // When this job should next be attempted. Set to "now" on creation,
    // pushed into the future on retry using exponential backoff.
    @Column(name = "run_at", nullable = false)
    private Instant runAt = Instant.now();

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void touch() {
        this.updatedAt = Instant.now();
    }
}
