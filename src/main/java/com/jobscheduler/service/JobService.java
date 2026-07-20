package com.jobscheduler.service;

import com.jobscheduler.dto.JobRequest;
import com.jobscheduler.dto.JobResponse;
import com.jobscheduler.entity.Job;
import com.jobscheduler.entity.JobStatus;
import com.jobscheduler.exception.JobNotFoundException;
import com.jobscheduler.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JobService {

    private final JobRepository jobRepository;
    private final JobQueueService jobQueueService;

    public JobResponse submitJob(JobRequest request) {
        Job job = new Job();
        job.setType(request.getType());
        job.setPayload(request.getPayload());
        if (request.getMaxRetries() != null) {
            job.setMaxRetries(request.getMaxRetries());
        }
        job.setStatus(JobStatus.PENDING);

        job = jobRepository.save(job); // persist first, so we have an ID
        jobQueueService.enqueue(job.getId().toString()); // then hand it to workers

        return new JobResponse(job);
    }

    public JobResponse getJob(UUID id) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new JobNotFoundException(id));
        return new JobResponse(job);
    }

    public List<JobResponse> listJobs() {
        return jobRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(JobResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * Lets a user manually re-queue a job that ended up FAILED,
     * resetting its retry count so it gets a fresh set of attempts.
     */
    public JobResponse retryJob(UUID id) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new JobNotFoundException(id));

        job.setStatus(JobStatus.PENDING);
        job.setRetryCount(0);
        job.setErrorMessage(null);
        job = jobRepository.save(job);

        jobQueueService.enqueue(job.getId().toString());
        return new JobResponse(job);
    }
}
