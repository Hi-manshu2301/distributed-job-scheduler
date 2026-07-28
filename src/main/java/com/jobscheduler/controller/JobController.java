package com.jobscheduler.controller;

import com.jobscheduler.dto.JobRequest;
import com.jobscheduler.dto.JobResponse;
import com.jobscheduler.service.JobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;                  
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)                     // api 1 this will create(submit) the job
    public JobResponse submitJob(@Valid @RequestBody JobRequest request) {
        return jobService.submitJob(request);
    }

    @GetMapping("/{id}")                                    //2nd api this will get one job and read information about it like what happened to job - 101?
    public JobResponse getJob(@PathVariable UUID id) {
        return jobService.getJob(id);
    }

    @GetMapping                                             //3rd api frontend get list of job and the actions on them we will get them from this api 
    public List<JobResponse> listJobs() {
        return jobService.listJobs();
    }

    @PostMapping("/{id}/retry")                             //4th api this will perfrom action such as retry 
    public JobResponse retryJob(@PathVariable UUID id) {
        return jobService.retryJob(id);
    }

    @GetMapping("/dead-letter")
    public List<JobResponse> getDeadLetterJobs() {
        return jobService.getDeadLetterJobs();
    }
}
