package com.jobscheduler.worker;

import com.jobscheduler.entity.Job;
import com.jobscheduler.repository.JobRepository;
import com.jobscheduler.service.JobQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * A job that fails goes to RETRYING with a future runAt (backoff window).
 * It is NOT sitting in the Redis queue during that window - if it were,
 * a worker would just retry it instantly with no actual delay.
 * This scheduler is what "wakes it up" once its backoff has elapsed.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RetryScheduler {

    private final JobRepository jobRepository;
    private final JobQueueService jobQueueService;

    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void requeueDueRetries() {
        List<Job> dueJobs = jobRepository.findRetryableJobsDue();
        for (Job job : dueJobs) {
            job.setStatus(com.jobscheduler.entity.JobStatus.PENDING);
            jobRepository.save(job);

            jobQueueService.enqueue(job.getId().toString());
            log.info("Re-queued job {} for retry attempt {}", job.getId(), job.getRetryCount() + 1);
        }
    }
}
