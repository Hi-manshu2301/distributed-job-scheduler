package com.jobscheduler.entity;

public enum JobStatus {
    SCHEDULED,    //Created for the future run at, waiting to be queued  
    PENDING,     // created, waiting in the Redis queue
    RUNNING,     // picked up by a worker, currently executing
    SUCCESS,     // completed without error
    FAILED,      // failed after exhausting all retries
    RETRYING,    // failed once, waiting for the next retry attempt
    CANCELLED    // manually cancelled before execution
}
