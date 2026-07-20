package com.jobscheduler.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class JobQueueService {

    private static final String QUEUE_KEY = "job:queue";

    private final StringRedisTemplate redisTemplate;

    public void enqueue(String jobId) {
        // LPUSH - new jobs go on the left end
        redisTemplate.opsForList().leftPush(QUEUE_KEY, jobId);
    }

    /**
     * Blocking pop from the right end (BRPOP under the hood).
     * This is the piece that makes the queue safe for multiple worker instances:
     * Redis guarantees only ONE caller ever receives a given jobId, even if
     * three workers call this at the exact same millisecond.
     */
    public Optional<String> dequeueBlocking(Duration timeout) {
        String jobId = redisTemplate.opsForList().rightPop(QUEUE_KEY, timeout);
        return Optional.ofNullable(jobId);
    }
}
