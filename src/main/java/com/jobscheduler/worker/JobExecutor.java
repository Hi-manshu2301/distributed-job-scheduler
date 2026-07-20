package com.jobscheduler.worker;

import com.jobscheduler.entity.Job;
import org.springframework.stereotype.Component;

import java.util.Random;

/**
 * This is where real work would happen - calling an email API, processing a file, etc.
 * For the demo/portfolio version, we simulate work with a delay and a random failure
 * rate so retry logic actually gets exercised end-to-end.
 *
 * Swap the body of execute() for real logic per job.getType() when you extend this.
 */
@Component
public class JobExecutor {

    private final Random random = new Random();

    public void execute(Job job) throws Exception {
        // Simulate variable work duration
        Thread.sleep(300 + random.nextInt(700));

        // Simulate a ~30% failure rate so you can watch retries happen in the dashboard
        if (random.nextInt(100) < 30) {
            throw new RuntimeException("Simulated failure while executing job type: " + job.getType());
        }
    }
}
