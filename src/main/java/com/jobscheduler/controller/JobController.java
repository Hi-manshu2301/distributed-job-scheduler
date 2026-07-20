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
    @ResponseStatus(HttpStatus.CREATED)
    public JobResponse submitJob(@Valid @RequestBody JobRequest request) {
        return jobService.submitJob(request);
    }

    @GetMapping("/{id}")
    public JobResponse getJob(@PathVariable UUID id) {
        return jobService.getJob(id);
    }

    @GetMapping
    public List<JobResponse> listJobs() {
        return jobService.listJobs();
    }

    @PostMapping("/{id}/retry")
    public JobResponse retryJob(@PathVariable UUID id) {
        return jobService.retryJob(id);
    }
}
