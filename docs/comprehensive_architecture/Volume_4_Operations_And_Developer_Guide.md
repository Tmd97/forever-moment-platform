# Forever Moment Platform/Core Service - Volume 4: Operations and Developer Guide

## SECTION 13 - FAILURE SCENARIOS

In distributed systems, failure is not an anomaly; it is an expectation. Below are the documented failure scenarios and how the Platform Service handles them.

### 1. Database Connection Failure During Booking
- **How it occurs:** The PostgreSQL server restarts or the connection pool is exhausted while `BookingCreationTransactionService` is executing.
- **Current Protection:** Spring's `@Transactional` boundary throws a `DataAccessException`. The transaction rolls back. Nothing is committed. 
- **Recovery:** The outer `BookingOrchestrationService` catches the exception. It attempts to mark the Idempotency state as `FAILED`. If that also fails (because the DB is down), the client receives a 500 error. Because the lease expires, the client can safely retry later.

### 2. Kafka Broker Outage
- **How it occurs:** All Kafka brokers are down or network routing to Kafka drops.
- **Current Protection:** The fast-path `BookingEnrichmentTask` will time out trying to get an ACK from `producer.send()`. An exception is thrown.
- **Recovery:** The fast-path aborts. However, the DB transaction *already committed* the `PENDING` outbox record. A few seconds later, `OutboxRetriesJob` (Quartz) wakes up, picks up the record, and tries to send it. It will continue retrying until Kafka is back online. Zero data loss.

### 3. Service Restart Mid-Transaction (Pod Eviction)
- **How it occurs:** Kubernetes scales down the deployment, sending SIGTERM, and forcefully kills the pod while a booking is processing.
- **Before DB Commit:** The DB connection is severed. PostgreSQL automatically rolls back the uncommitted transaction. Idempotency `IN_PROGRESS` row remains, but its `lease_expiration` lapses 2 minutes later. Client retries safely.
- **After DB Commit, Before Enrichment:** The booking is committed, but the fast-path thread is killed before sending to Kafka. Quartz on another pod picks up the `PENDING` outbox row 60 seconds later and completes the work.

### 4. Consumer Fails to Compensate Inventory
- **How it occurs:** `BookingFailedConsumer` receives a failure event from downstream, but updating `SlotInventory` (decrementing the count) fails due to Optimistic Locking or DB outage.
- **Recovery:** The `@KafkaListener` throws an exception. `enable.auto.commit=false` means the offset is not advanced. Spring Kafka retries the message 3 times. If it still fails, it is pushed to `core-dlt` (Dead Letter Topic). An engineer must manually inspect the DLT and decrement the inventory to stop revenue leakage.

---

## SECTION 14 - PRODUCTION READINESS REVIEW

*Role: Principal Engineer / Architecture Reviewer*

Assuming the codebase is not perfect, here are critical gaps and technical debt identified prior to production launch.

### A. Production Go-Live Blockers

1. **Title: Quartz Lease Expiration vs Task Execution Time Mismatch**
   - **Severity:** HIGH
   - **Description:** `OutboxRetriesJob` claims a row for 30 seconds. If the Kafka cluster is heavily degraded and `producer.send()` blocks for 35 seconds, the lease expires. A second Quartz node claims the row and sends it. Node 1 finally unblocks and sends it.
   - **Risk:** Duplicate events on Kafka. While downstream should be idempotent, it increases latency and load.
   - **Recommended Fix:** Implement Optimistic Locking (`@Version`) on `BookingOutbox`. When Node 1 attempts to set `status = COMPLETED`, the version check will fail, preventing the update and alerting us to the lease overrun.
   - **Classes Involved:** `BookingOutboxDaoImpl`, `BookingOutbox`.

2. **Title: Missing Kafka Dead-Letter Alerting**
   - **Severity:** HIGH
   - **Description:** Messages landing in `core-dlt` mean inventory is perpetually reserved for a booking that failed downstream. This is active revenue loss (preventing someone else from buying that slot).
   - **Recommended Fix:** Wire up Prometheus metrics or Datadog alerts on the specific `core-dlt` topic offset lag. If lag > 0, trigger PagerDuty immediately.

### B. Technical Debt

1. **Title: Synchronous Enrichment Risk**
   - **Severity:** MEDIUM
   - **Description:** `BookingEnrichmentTask` runs asynchronously, but it performs synchronous Redis lookups (and DB fallbacks) before publishing. If Redis is slow, the thread pool handling async enrichment could become exhausted.
   - **Recommended Fix:** Use reactive non-blocking paradigms (e.g., `CompletableFuture` or WebFlux) for the enrichment pipeline.

2. **Title: Idempotency Row Cleanup**
   - **Severity:** LOW
   - **Description:** `booking_request_idempotency` grows infinitely. Over years, this table will become massive, slowing down inserts due to index re-balancing.
   - **Recommended Fix:** Implement a `IdempotencyCleanupJob` to delete `COMPLETED` records older than 30 days.

---

## SECTION 15 - TROUBLESHOOTING GUIDE (OPERATIONAL RUNBOOK)

### Issue: Bookings return HTTP 201/202 but users say they never get confirmation.
- **Symptoms:** Support gets complaints. Downstream payment processor has no record of the transaction.
- **Root Cause:** Platform is accepting bookings, but events are failing to reach Kafka.
- **Investigation Steps:**
  1. Check database: `SELECT count(*) FROM booking_outbox WHERE status = 'PENDING' AND created_at < NOW() - INTERVAL '5 minutes';`
  2. If the count is > 0, Quartz is failing to publish.
  3. Check logs in Kibana for `OutboxRetriesJob` exceptions (likely `TimeoutException` or `BrokerNotAvailableException`).
- **Fix:** Restore Kafka connectivity. Quartz will automatically drain the backlog.

### Issue: High CPU and Database Connection Pool Exhaustion
- **Symptoms:** Platform API latency spikes from 100ms to 5000ms. Database CPU is at 99%.
- **Investigation Steps:**
  1. Check if `CatalogCacheService` (Redis) is down.
  2. If Redis is down, `BookingEnrichmentTask` falls back to PostgreSQL for every single lookup (Locations, Slots, Add-ons). This N+1 query storm crushes the DB.
- **Fix:** Restart Redis. The service will automatically self-heal as the cache re-warms.

---

## SECTION 16 - DEVELOPER ONBOARDING GUIDE

Welcome to the Core Platform team! This service is the heart of Forever Moment.

### 1. Project Structure
- `moment_forever_core`: The Spring Boot app. Contains Controllers, Services, Jobs, and Kafka Listeners.
- `moment_forever_data`: Entity layer and DAOs. We use raw JPA/Hibernate.
- `moment_forever_commons`: DTOs and Enums. Shared across other services.

### 2. Startup Flow
Run `MomentForeverApp.java`. On startup, `SuperAdminSeeder` runs to ensure a default Admin exists. It binds to port 8081. Ensure you have Docker running locally for Postgres, Kafka, and Redis (`docker-compose up` from the `SETUP.md`).

### 3. How to Trace a Booking Request
1. **Entry Point:** `BookingControllerPublic.java` (`POST /bookings`).
2. **Orchestrator:** `BookingOrchestrationService.java`. Look here to understand the high-level saga.
3. **Idempotency:** `BookingRequestIdempotencyCoordinator.java`. Read this to understand how we prevent double-bookings.
4. **Transaction:** `BookingCreationTransactionService.java`. This is the ONLY place where DB inserts happen.
5. **Outbox & Kafka:** `BookingEnrichmentTask.java`.

### 4. Common Mistakes While Changing Code
- ❌ **Adding API calls inside `BookingCreationTransactionService`:** Never do this. This class holds an active database transaction. If you make a 2-second HTTP call to a CRM here, you hold a database connection open for 2 seconds. The connection pool will exhaust immediately.
- ❌ **Bypassing Idempotency:** If you add a new endpoint that creates state (e.g., `POST /addons`), ensure it uses idempotency keys if it can be retried by clients.
- ❌ **Updating `BookingRequestDto` without updating the Fingerprint:** If you add a new field (e.g., `specialRequests`), you MUST update `BookingRequestFingerprintService` to include it in the SHA-256 hash.

---

## SECTION 17 - FAQ

- **Q: Why not just use `UNIQUE` constraints instead of Idempotency Keys?**
  - A: A user might legitimately want to book the same experience twice (e.g., one for themselves, one for a friend later). Unique constraints on `(user_id, experience_id)` prevent this. The UUID represents a *specific UI action intent*.
- **Q: Why do we store FAILED events in the idempotency table?**
  - A: If a request fails because of invalid data (e.g., `paxCount = -1`), we mark it FAILED so the client can fix the data and retry *using the same UUID*. If we marked it COMPLETED or deleted it, they'd get weird errors.
- **Q: What happens if atomic claims are removed?**
  - A: The two pods running Quartz will execute `SELECT * FROM outbox` simultaneously, fetch the same 50 records, and send 50 duplicate messages to Kafka.
- **Q: Why is Request Deduplication not enough?**
  - A: Deduplication (dropping duplicate keys at the API level) doesn't solve the Outbox problem. We need *both* Idempotency (prevent duplicate DB saves) AND Outbox (prevent DB/Kafka sync issues).

---

## SECTION 18 - UML DIAGRAMS

All architectural diagrams, sequence flows, state machines, and ER diagrams have been documented across Volumes 1, 2, and 3.

**Quick Reference Map:**
- **Context & Component Diagrams:** See *Volume 1, Section 2*
- **End-to-End Sequence Diagram:** See *Volume 1, Section 3*
- **Idempotency Race Condition Timeline:** See *Volume 2, Section 5*
- **Database ER Diagram:** See *Volume 3, Section 9*
- **State Machine Flowcharts:** See *Volume 3, Section 11*