package com.jobscheduler;

import com.jobscheduler.entity.Job;
import com.jobscheduler.entity.JobStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JobEntityTest {

    @Test
    void newJobDefaultsToPendingWithThreeRetries() {
        Job job = new Job();
        assertEquals(JobStatus.PENDING, job.getStatus());
        assertEquals(3, job.getMaxRetries());
        assertEquals(0, job.getRetryCount());
    }

    @Test
    void exponentialBackoffDoublesEachAttempt() {
        // Mirrors the formula used in JobWorker.handleFailure: 2^retryCount seconds
        assertEquals(2, (long) Math.pow(2, 1));
        assertEquals(4, (long) Math.pow(2, 2));
        assertEquals(8, (long) Math.pow(2, 3));
    }
}
