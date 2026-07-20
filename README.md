# Distributed Job Scheduler

A backend service for submitting asynchronous jobs, processing them across
multiple worker instances via a Redis-backed queue, with automatic retry
and exponential backoff on failure.

## Why this exists

Most fresher portfolios have a to-do app or a CRUD blog. This project
demonstrates the things backend/distributed-systems interviews actually
probe: concurrency-safe queuing, retry semantics, and horizontal scaling —
using tools (Spring Boot, Postgres, Redis, Docker) that map directly to
real production stacks.

## Architecture

```
Client --> POST /api/jobs --> API (Spring Boot)
                                  |
                    saves row     |    pushes job ID
                        v         v
                    Postgres    Redis (LIST used as queue)
                        ^         |
                        |    BRPOP (blocking, atomic)
                        |         v
                    updates    Worker instance #1, #2, #3...
                    status     (same jar, run N times)
```

- **Postgres** is the source of truth for job state (PENDING, RUNNING,
  SUCCESS, RETRYING, FAILED).
- **Redis** is *only* the queue — a list of job IDs. Using `BRPOP`
  (blocking right-pop) guarantees that even with multiple worker
  instances polling simultaneously, no two workers ever receive the same
  job ID. This is the mechanism that makes it safe to scale workers
  horizontally.
- **Exponential backoff**: on failure, a job goes to `RETRYING` with
  `run_at` set `2^retryCount` seconds in the future. A separate scheduled
  task (`RetryScheduler`) re-enqueues jobs once their backoff window has
  passed — this keeps failed jobs *out* of the queue during their
  cooldown instead of being instantly retried.

## Running locally

```bash
docker compose up --build --scale worker=2
```

This starts Postgres, Redis, one API instance (port 8080), and 2 extra
worker-only instances — 3 total consumers pulling from the same queue.

## API

**Submit a job**
```bash
curl -X POST http://localhost:8080/api/jobs \
  -H "Content-Type: application/json" \
  -d '{"type":"SEND_EMAIL","payload":"{\"to\":\"test@example.com\"}"}'
```

**Check a job's status**
```bash
curl http://localhost:8080/api/jobs/{id}
```

**List all jobs**
```bash
curl http://localhost:8080/api/jobs
```

**Manually retry a failed job**
```bash
curl -X POST http://localhost:8080/api/jobs/{id}/retry
```

## What to say about this in an interview

- Why Redis `BRPOP` over a naive `LPOP` in a loop: `LPOP` requires a
  check-then-act (see if empty, then pop) which is a race condition
  across processes; `BRPOP` is a single atomic command with built-in
  blocking, so it doesn't need polling-with-sleep AND it removes the race.
- Why job state lives in Postgres, not Redis: Redis holds *only* the
  queue (ephemeral, can be flushed/restarted); Postgres holds durable
  history so a job's status, retry count, and error message survive a
  queue restart.
- Trade-off acknowledged: this uses at-least-once delivery — if a worker
  crashes mid-execution after popping a job but before writing SUCCESS,
  that job is lost from the queue (not currently re-queued on worker
  crash). A production version would move to Redis Streams with
  consumer groups + acknowledgment, which supports reclaiming
  unacknowledged messages. This is a good "what I'd improve" answer.

## Next steps (roadmap)

- [ ] Delayed/scheduled jobs (submit with a future `run_at`)
- [ ] Dead-letter handling with a `/api/jobs/dead-letter` view
- [ ] React dashboard (list, filter by status, manual retry button)
- [ ] Swap Redis list for Redis Streams + consumer groups for
      at-least-once → exactly-once-ish delivery with ack/reclaim
- [ ] GitHub Actions CI running tests on every push
- [ ] Deploy to Railway/Render with a live demo link
