package com.jobscheduler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling   // lets the worker poll Redis on a fixed interval
@EnableAsync         // lets job execution happen off the polling thread
public class JobSchedulerApplication {
    public static void main(String[] args) {
        SpringApplication.run(JobSchedulerApplication.class, args);
    }
}
