# Forever Moment Platform/Core Service - Volume 5: System Design Interview Preparation

*Role: FAANG Senior Engineer & System Design Interviewer*
*Context: Derived from the Forever Moment Platform/Core Service Architecture*

---

## PART A - BEGINNER QUESTIONS

**Q1: How would you design a catalog and inventory service?**
*Ideal Answer:* I would design a microservice with a relational database (like PostgreSQL) because inventory and bookings require strict ACID transactions. The service would manage relationships between Experiences, Locations, and Time Slots. When a user books, the service checks available capacity, decrements the inventory, and publishes an event to Kafka for downstream services (like Payments) to process.

**Q2: How would you prevent duplicate booking requests from the frontend?**
*Ideal Answer:* I would use an Idempotency Key. The frontend generates a unique UUID for the user's booking attempt and sends it in the HTTP header. The backend stores this UUID in a database table. If the user clicks "Book" twice, the database enforces a unique constraint on the UUID, rejecting the second request and returning the cached response from the first request.

**Q3: How would you handle large image files for the catalog?**
*Ideal Answer:* I would not store large binaries in a relational database. I would use a dedicated blob storage system like AWS S3 or MongoDB GridFS. The relational database would only store the metadata and the URL/reference to the blob store.

**Q4: How would you maintain fast read speeds for a heavily browsed catalog?**
*Ideal Answer:* I would implement a distributed cache like Redis. When a user browses experiences, the service reads from Redis. When an admin updates an experience, the service writes to PostgreSQL and immediately updates the Redis cache (Cache-Aside or Write-Through pattern).

---

## PART B - MID LEVEL QUESTIONS

**Q1: Prevent overselling inventory when 100 users try to book the last seat simultaneously.**
*Detailed Answer:* I would use Optimistic Locking. In the `SlotInventory` table, I add a `version` column. When 100 threads read the row, they all see `version = 1` and `available_capacity = 1`. When they try to update, the SQL query includes `WHERE id = ? AND version = 1`. The first thread succeeds and increments the version to 2. The other 99 threads fail to update (0 rows affected) and throw an `OptimisticLockingFailureException`. The application catches this and returns a "Capacity Exceeded" error to the 99 users.

**Q2: Handle retries safely without causing database deadlocks.**
*Detailed Answer:* If a transaction fails due to a transient error (like the Optimistic Lock failure mentioned above), it shouldn't immediately return a 500 error. I would implement a short retry loop (e.g., `MAX_ATTEMPTS = 3`). On failure, the thread starts a brand-new transaction, reads the fresh database state, and attempts the operation again. This gracefully handles microsecond concurrency collisions.

**Q3: Design a system that updates the database and publishes an event to Kafka safely (The Dual-Write Problem).**
*Detailed Answer:* I would use the Transactional Outbox Pattern. Within a single PostgreSQL transaction, I write the business data (e.g., `BookingReservation`) AND an event payload to a `booking_outbox` table. If the transaction commits, both are saved. A separate asynchronous worker or polling job reads the `booking_outbox` table and publishes the events to Kafka. Once Kafka acknowledges, the worker marks the outbox row as `PUBLISHED`.

---

## PART C - SENIOR ENGINEER QUESTIONS

**Q1: Explain how you would design Request Idempotency beyond just a UUID.**
*Detailed Answer & Tradeoffs:* A UUID is not enough because a malicious user could reuse a failed UUID but change the payload (e.g., changing price or seat count). 
1. I would hash the incoming request payload (SHA-256) to create a **Fingerprint**. 
2. The idempotency table stores `(idempotency_key, fingerprint, state, response_payload)`. 
3. If a retry comes in with the same key but a different fingerprint, I return a `409 Conflict`.
4. To handle concurrent requests with the same key, I use an atomic `INSERT ON CONFLICT DO NOTHING`. The winner sets the state to `IN_PROGRESS`. The loser sees `IN_PROGRESS` and returns `423 Locked`.

**Q2: Explain Worker Fencing and Atomic Claims.**
*Detailed Answer:* When a Quartz background job polls the `booking_outbox` table to publish events, running this job on multiple pods concurrently will result in duplicate Kafka messages. 
To fence workers, we use Atomic Claims. A worker executes an atomic SQL update: 
`UPDATE booking_outbox SET owner_node = 'pod-1', lease_expires = NOW() + 30s WHERE status = 'PENDING' AND owner_node IS NULL`. 
This guarantees only `pod-1` claims those specific rows. 

**Q3: Explain Lease-Based Ownership and crash recovery.**
*Detailed Answer:* Following up on the atomic claim: what if `pod-1` is OOM-Killed exactly after claiming the rows? The rows are locked forever. 
This is why we set `lease_expires`. The polling query looks for `owner_node IS NULL OR lease_expires < NOW()`. If `pod-1` dies, 30 seconds later, its lease expires, and `pod-2` can safely claim and process those abandoned rows.

**Q4: Explain Compensation Consistency.**
*Detailed Answer:* If the Booking Service fails to process a payment, it publishes a `BookingFailedEvent`. The Platform Service consumes this and must restore the reserved inventory. 
Consistency is tricky: what if the Platform service consumes the event, restores the inventory, but crashes before ACKing the Kafka offset? It will consume it again and restore the inventory *again*, creating phantom capacity. The Platform Service must implement an Inbox/Deduplication table to ensure compensation events are processed exactly once.

---

## PART D - STAFF / PRINCIPAL QUESTIONS

**Q1: How would you scale this inventory architecture to 1 million booking attempts per minute (e.g., Taylor Swift tickets)?**
*Architecture Answer:* PostgreSQL with Optimistic Locking will melt under 1M TPS due to transaction contention on a single row. 
- **L1 Cache (Redis + Lua):** I would move the hot inventory counters to Redis. A Lua script executes `if GET(key) >= requested then DECRBY(key, requested) return true else return false end`. This is atomic and handles millions of operations per second.
- **Async Settlement:** Once Redis grants the token, the request enters a Kafka queue. The PostgreSQL database slowly drains this queue and inserts the actual relational records at its own pace.
- **Outbox via CDC:** I would eliminate Quartz polling (which causes DB CPU spikes) and use Debezium (Change Data Capture). Debezium tails the Postgres Write-Ahead Log (WAL) and streams outbox events to Kafka with sub-millisecond latency.

**Q2: How would you handle active-active multi-region deployment for this catalog?**
*Architecture Answer:* Active-Active for inventory is famously difficult due to the speed of light. 
- **Catalog Data:** Highly cacheable, rarely changes. Can be asynchronously replicated across regions.
- **Inventory Data:** If US-East and EU-West both try to sell the last seat, we get a conflict. I would use a distributed SQL database like Google Spanner or CockroachDB that provides external consistency and TrueTime. Alternatively, partition the inventory geographically (e.g., Paris events are strictly mastered in the EU region; US traffic for Paris events is routed cross-region).

**Q3: How would you redesign compensation if Kafka is not available?**
*Architecture Answer:* Use the Saga Orchestration pattern with a durable workflow engine (like Temporal or AWS Step Functions). The Orchestrator holds the state of the Saga. If step 2 fails, the Orchestrator executes step 1's compensation activity. The engine natively handles retries, backoffs, and guarantees execution without manually building Inbox/Outbox tables.

---

## PART E - DEEP DIVE QUESTIONS SPECIFIC TO THIS REPOSITORY

**Q1: Why does `BookingCreationRetryService.java` explicitly catch `ObjectOptimisticLockingFailureException` and loop?**
*Implementation Answer:* Because optimistic locking prevents data corruption by failing one of the competing transactions. However, throwing a 500 Server Error to a user for a microsecond database collision is a terrible UX. By catching it and retrying in a fresh `@Transactional` boundary, the thread reads the new capacity. If seats are still available, the user gets the booking seamlessly. If they fought over the absolute last seat, the retry fetches the new state, sees capacity is 0, and cleanly throws a "Capacity Exceeded" business exception.

**Q2: Why does the Platform Service use Redis as a Snapshot Cache rather than the source of truth?**
*Implementation Answer:* Redis is used in `CatalogCacheService` to enrich `BookingRequestEvent`s with human-readable names and prices before sending them to Kafka. If Redis goes down, the `BookingEnrichmentTask` gracefully falls back to querying PostgreSQL, repopulates Redis, and continues. This guarantees high availability. If Redis was the source of truth, a cache eviction or Redis cluster crash would cause total system downtime.

**Q3: Why is `BookingRequestIdempotencyCoordinator` locking the request with an `IN_PROGRESS` state instead of just doing a unique constraint on a `bookings` table?**
*Implementation Answer:* Because the booking process involves multiple steps (checking capacity, inserting reservations, inserting outbox records) that take time. If we just relied on a unique constraint on the final table, a second rapid request might slip through while the first is still executing its business logic, wasting CPU cycles and potentially causing complex deadlocks. `IN_PROGRESS` acts as an immediate distributed Mutex at the very edge of the application.

---

## PART F - COMMON INTERVIEW MISTAKES

### Topic: Capacity Management
**Question:** How do you prevent two users from booking the same seat?
**Incorrect Answer:** I will use a `SELECT` query to check if capacity > 0, and if so, execute an `UPDATE` query to decrement it.
**Why Incorrect:** Race condition (Time-Of-Check to Time-Of-Use). Both threads can run the `SELECT` at the same time, see capacity > 0, and both will `UPDATE`.
**Correct Answer:** Optimistic locking (`@Version`) or Pessimistic locking (`SELECT FOR UPDATE`), or an atomic update (`UPDATE ... WHERE capacity > 0`).

### Topic: Cache Invalidation
**Question:** How do you keep the catalog cache in sync with the database?
**Incorrect Answer:** I will write to Redis, and if it succeeds, I will write to the Database.
**Why Incorrect:** If the Database write fails (e.g., constraint violation), your cache is now permanently out of sync with the database, showing ghost data.
**Correct Answer:** Write to the Database first. If it succeeds, update or invalidate the cache (Cache-Aside). Alternatively, use CDC (Debezium) to invalidate the cache automatically based on the database transaction log.

### Topic: Distributed Transactions
**Question:** How do you ensure the Booking is saved in Postgres and the Image is saved in MongoDB atomically?
**Incorrect Answer:** I will use the `@Transactional` annotation around both the Postgres save and the Mongo save.
**Why Incorrect:** `@Transactional` in Spring only manages a single `PlatformTransactionManager` (usually JDBC). It cannot automatically execute a 2-Phase Commit across a relational DB and a NoSQL DB.
**Correct Answer:** Accept eventual consistency. Save the metadata to Postgres in a transaction, and queue an asynchronous job to clean up orphaned images in MongoDB if the Postgres transaction rolls back, or vice versa.