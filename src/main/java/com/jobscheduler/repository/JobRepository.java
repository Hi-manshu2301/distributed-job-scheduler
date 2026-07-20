package com.jobscheduler.repository;

import com.jobscheduler.entity.Job;
import com.jobscheduler.entity.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface JobRepository extends JpaRepository<Job, UUID> {

    List<Job> findByStatus(JobStatus status);

    // Used by the dashboard to page through jobs, most recent first
    List<Job> findAllByOrderByCreatedAtDesc();

    @Query("SELECT j FROM Job j WHERE j.status = 'RETRYING' AND j.runAt <= CURRENT_TIMESTAMP")
    List<Job> findRetryableJobsDue();
}
