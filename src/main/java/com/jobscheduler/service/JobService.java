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
        job.setType(request.getType());                             //copy type from request
        job.setPayload(request.getPayload());                       //store json payload
        if (request.getMaxRetries() != null) {                      //if client send the max retry use them otherwise default 
            job.setMaxRetries(request.getMaxRetries());
        }
        job.setStatus(JobStatus.PENDING);

        job = jobRepository.save(job);                              // add to postgres so we got id for redis 
        jobQueueService.enqueue(job.getId().toString());            // then push job id to redis - worker will fetch the full job from redis using ID

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
                .orElseThrow(() -> new JobNotFoundException(id));       //fetch job using id if not find throw job not found

        job.setStatus(JobStatus.PENDING);
        job.setRetryCount(0);
        job.setErrorMessage(null);
        job = jobRepository.save(job);                                  //update database

        jobQueueService.enqueue(job.getId().toString());                //push id back to redis
        return new JobResponse(job);
    }
    //dead-letter endpoint
    public List<JobResponse> getDeadLetterJobs() {
    return jobRepository.findByStatus(JobStatus.FAILED)
            .stream()
            .map(JobResponse::new)
            .collect(Collectors.toList());
    }
}
