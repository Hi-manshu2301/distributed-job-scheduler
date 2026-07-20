package com.jobscheduler.dto;

import com.jobscheduler.entity.Job;
import com.jobscheduler.entity.JobStatus;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
public class JobResponse {
    private final UUID id;
    private final String type;
    private final String payload;
    private final JobStatus status;
    private final int retryCount;
    private final int maxRetries;
    private final String errorMessage;
    private final Instant createdAt;
    private final Instant updatedAt;

    public JobResponse(Job job) {
        this.id = job.getId();
        this.type = job.getType();
        this.payload = job.getPayload();
        this.status = job.getStatus();
        this.retryCount = job.getRetryCount();
        this.maxRetries = job.getMaxRetries();
        this.errorMessage = job.getErrorMessage();
        this.createdAt = job.getCreatedAt();
        this.updatedAt = job.getUpdatedAt();
    }
}
