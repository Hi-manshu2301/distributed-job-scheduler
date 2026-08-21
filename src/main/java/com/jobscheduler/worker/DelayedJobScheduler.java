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

import java.util.List;

/**
 * A job submitted with a future runAt sits in SCHEDULED state - not in the
 * Redis queue yet, so no worker can accidentally grab and run it early.
 * This scheduler is what "wakes it up": every second, it checks for
 * SCHEDULED jobs whose time has arrived, flips them to PENDING, and pushes
 * them into the real queue.
 *
 * Uses the same row-locking pattern as RetryScheduler (PESSIMISTIC_WRITE +
 * SKIP LOCKED via findScheduledJobsDue), for the same reason: multiple
 * instances of this app are all running this exact scheduler independently,
 * and without the lock, two instances could both claim the same due job.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DelayedJobScheduler {

    private final JobRepository jobRepository;
    private final JobQueueService jobQueueService;

    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void promoteDueScheduledJobs() {
        List<Job> dueJobs = jobRepository.findScheduledJobsDue();
        for (Job job : dueJobs) {
            job.setStatus(JobStatus.PENDING);
            jobRepository.save(job);

            jobQueueService.enqueue(job.getId().toString());
            log.info("Scheduled job {} is now due - queued for execution", job.getId());
        }
    }
}
