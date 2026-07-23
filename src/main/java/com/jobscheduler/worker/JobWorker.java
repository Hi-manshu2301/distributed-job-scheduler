package com.jobscheduler.worker;

import com.jobscheduler.entity.Job;
import com.jobscheduler.entity.JobStatus;
import com.jobscheduler.repository.JobRepository;
import com.jobscheduler.service.JobQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class JobWorker {

    private final JobQueueService jobQueueService;
    private final JobRepository jobRepository;
    private final JobExecutor jobExecutor;

    /**
     * Polls Redis every 500ms. Because dequeueBlocking uses BRPOP with a 2s timeout,
     * this effectively means: "wait up to 2s for a job, process it if found, repeat immediately."
     * Running multiple instances of this app is exactly what makes the system "distributed" -
     * Redis's BRPOP guarantees no two instances ever grab the same job ID.
     */
    @Scheduled(fixedDelay = 500)
    public void pollAndProcess() {
        Optional<String> jobIdOpt = jobQueueService.dequeueBlocking(Duration.ofSeconds(2));
        jobIdOpt.ifPresent(this::processJob);
    }

    private void processJob(String jobIdStr) {
        UUID jobId = UUID.fromString(jobIdStr);
        Job job = jobRepository.findById(jobId).orElse(null);
        if (job == null) {
            log.warn("Job {} was in the queue but not found in DB - skipping", jobId);
            return;
        }

        if(job.getStatus() == JobStatus.SUCCESS ||job.getStatus() == JobStatus.FAILED ||job.getStatus() == JobStatus.CANCELLED){
            log.warn("Job {} already in terminal state {} - skipping duplicate processing",
                    jobId, job.getStatus());
            return;
        }
        job.setStatus(JobStatus.RUNNING);
        jobRepository.save(job);
        log.info("Processing job {} (type={}, attempt={})", jobId, job.getType(), job.getRetryCount() + 1);

        try {
            jobExecutor.execute(job);
            job.setStatus(JobStatus.SUCCESS);
            job.setErrorMessage(null);
            log.info("Job {} succeeded", jobId);
        } catch (Exception ex) {
            handleFailure(job, ex);
        }

        jobRepository.save(job);
    }

    private void handleFailure(Job job, Exception ex) {
        job.setRetryCount(job.getRetryCount() + 1);
        job.setErrorMessage(ex.getMessage());

        if (job.getRetryCount() >= job.getMaxRetries()) {
            job.setStatus(JobStatus.FAILED);
            log.warn("Job {} failed permanently after {} attempts", job.getId(), job.getRetryCount());
        } else {
            job.setStatus(JobStatus.RETRYING);
            // Exponential backoff: 2^retryCount seconds (2s, 4s, 8s, ...)
            long backoffSeconds = (long) Math.pow(2, job.getRetryCount());
            job.setRunAt(Instant.now().plusSeconds(backoffSeconds));
            log.info("Job {} will retry in {}s (attempt {}/{})",
                    job.getId(), backoffSeconds, job.getRetryCount(), job.getMaxRetries());
        }
    }
}
