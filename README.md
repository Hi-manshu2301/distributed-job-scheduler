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

## Design decisions & lessons learned

**Why Redis `BRPOP` over a naive `LPOP`-in-a-loop:** `LPOP` requires a
check-then-act pattern (check if the list is empty, then pop) which is a
race condition across multiple processes. `BRPOP` is a single atomic,
blocking command — no polling loop, no race.

**Why job state lives in Postgres, not Redis:** Redis holds only the
queue (ephemeral — a restart or flush loses it). Postgres holds durable
history, so a job's status, retry count, and error message survive a
queue restart.

**A real concurrency bug found under load:** while stress-testing with
concurrent worker instances, retry counts were observed exceeding
`maxRetries` (e.g. 28 retries with a limit of 3), and some jobs were
reprocessed after already reaching a terminal state.

Root cause: every running instance (`api`, `worker-1`, `worker-2`) runs
its own independent `RetryScheduler`, each polling Postgres every second
for jobs whose retry backoff had elapsed. With no coordination between
instances, two schedulers could read the same "due" job in the same
window and both re-queue it — producing duplicate entries in Redis for a
single job, each treated as an independent attempt.

Fix: the query that finds due retry jobs now uses
`@Lock(LockModeType.PESSIMISTIC_WRITE)` with a
`jakarta.persistence.lock.timeout = -2` hint (Hibernate's `SKIP LOCKED`),
wrapped in a `@Transactional` method. This tells Postgres to let only one
instance claim a given row; every other instance skips it instead of
waiting or re-reading it. Combined with an idempotency guard in
`JobWorker` (skip jobs already in a terminal state), this closes the race
at the database level rather than relying on application-level timing.

**Known limitation (by design, not yet fixed):** this system uses
at-least-once delivery. If a worker crashes after popping a job from
Redis but before writing its final status, that job is lost from the
queue and stays stuck in `RUNNING`. A future iteration could move from a
plain Redis list to Redis Streams with consumer groups, which support
acknowledgment and reclaiming unacknowledged messages after a timeout.

## Next steps (roadmap)

- [ ] Delayed/scheduled jobs (submit with a future `run_at`)
- [ ] Dead-letter handling with a `/api/jobs/dead-letter` view
- [ ] React dashboard (list, filter by status, manual retry button)
- [ ] Swap Redis list for Redis Streams + consumer groups for
      at-least-once → exactly-once-ish delivery with ack/reclaim
- [ ] GitHub Actions CI running tests on every push
- [ ] Deploy to Railway/Render with a live demo link
