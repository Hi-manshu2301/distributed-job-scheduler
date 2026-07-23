package com.jobscheduler;

import com.jobscheduler.service.JobQueueService;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * NOTE: this test expects Redis to already be running on localhost:6379
 * (i.e. run "docker compose up" first, then run this test).
 *
 * What it proves: if 5 "pretend workers" all try to grab jobs from the
 * queue at the same time, every job goes to exactly one worker - never
 * two workers getting the same job, never a job going missing.
 */
class JobQueueConcurrencyTest {

    @Test
    void concurrentConsumersNeverReceiveDuplicateJobIds() throws InterruptedException {

        // Step 1: connect to the same Redis your app already uses
        LettuceConnectionFactory factory = new LettuceConnectionFactory("localhost", 6379);
        factory.afterPropertiesSet();
        JobQueueService queueService = new JobQueueService(new StringRedisTemplate(factory));

        int jobCount = 50;

        // Step 2: push 50 fake job IDs into the queue
        for (int i = 0; i < jobCount; i++) {
            queueService.enqueue("test-job-" + i);
        }

        // Step 3: this list will collect every job ID any worker receives.
        // CopyOnWriteArrayList = a List that's safe to add to from multiple
        // threads at the same time without corrupting itself.
        List<String> allReceivedIds = new CopyOnWriteArrayList<>();

        // Step 4: create 5 "pretend workers" - each one is just a Thread
        // running the same loop: keep grabbing jobs until none are left.
        Thread[] workers = new Thread[5];
        for (int w = 0; w < workers.length; w++) {
            workers[w] = new Thread(() -> {
                while (true) {
                    var result = queueService.dequeueBlocking(Duration.ofMillis(500));
                    if (result.isEmpty()) {
                        break; // nothing left in the queue, this worker is done
                    }
                    allReceivedIds.add(result.get());
                }
            });
        }

        // Step 5: start all 5 workers at (roughly) the same instant
        for (Thread worker : workers) {
            worker.start();
        }

        // Step 6: wait for every worker to finish before checking results
        for (Thread worker : workers) {
            worker.join();
        }

        // Step 7: the actual checks
        // Check A: did we receive exactly 50 total? (not 49 = something lost,
        // not 51 = something duplicated)
        assertEquals(jobCount, allReceivedIds.size(),
                "Expected exactly " + jobCount + " jobs received, some went missing or got duplicated");

        // Check B: are all 50 IDs unique? A HashSet automatically removes
        // duplicates, so if two workers got the same job, the set will be
        // smaller than the list.
        Set<String> uniqueIds = new HashSet<>(allReceivedIds);
        assertEquals(jobCount, uniqueIds.size(),
                "Two workers received the same job ID - this should never happen");
    }
}
