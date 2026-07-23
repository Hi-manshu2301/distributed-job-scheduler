package com.jobscheduler.repository;

import com.jobscheduler.entity.Job;
import com.jobscheduler.entity.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;


import java.util.List;          //repo method rerturn multiple rows eg.- 10 pending jobs so we need list
import java.util.UUID;

public interface JobRepository extends JpaRepository<Job, UUID> {         //my repo manages the job entity whose primary key type is UUID

    List<Job> findByStatus(JobStatus status);

    // Used by the dashboard to page through jobs, most recent first
    List<Job> findAllByOrderByCreatedAtDesc();

    @Query("SELECT j FROM Job j WHERE j.status = 'RETRYING' AND j.runAt <= CURRENT_TIMESTAMP")
    List<Job> findRetryableJobsDue();
}
