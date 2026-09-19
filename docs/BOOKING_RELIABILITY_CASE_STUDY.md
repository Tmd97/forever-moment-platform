# Booking Reliability Case Study

## Purpose

This case study records the design problems encountered while building the
Platform, Booking and Payment saga. It is intended as teaching material for senior
engineers and architects: not only what was implemented, but why plausible designs
failed under concurrency, retries and partial failure.

## System context

The system has three business services:

- Platform owns catalog configuration and date-scoped inventory.
- Booking owns the booking lifecycle.
- Payment owns payment execution and payment state.

Kafka carries commands and results between services. Each service owns its
database. No distributed transaction spans a database and Kafka.

The required delivery model is therefore:

```text
local ACID transaction
+ durable outbox/inbox state
+ at-least-once Kafka delivery
+ idempotent business transitions
= effectively-once business outcome
```

"Exactly once" is a business property built by the application. Kafka producer or
consumer settings alone cannot guarantee it across independent databases.

## Challenge 1: inventory was a global counter

### Initial problem

Capacity was originally tracked on the slot mapper itself. That mixed static
configuration with dynamic state and ignored booking date.

```text
SlotMapper(maxCapacity=20, currentBookings=12)
```

Bookings for different dates competed for the same counter.

### Resolution

Separate configuration from state:

```text
ExperienceTimeSlotMapper
  maxCapacity

SlotInventory
  slotMapperId
  bookingDate
  bookedCount
  version
```

The unique key `(slot_mapper_id, booking_date)` scopes capacity correctly.
Optimistic locking prevents existing-row lost updates. A unique constraint handles
the race where two transactions both try to create the first row; the loser retries
the entire transaction from a clean boundary.

### Lesson

Concurrency control must match the aggregate being protected. A global counter
cannot enforce a date-specific invariant.

## Challenge 2: retrying inside a failed transaction

### Initial temptation

Catch an optimistic-lock or unique-constraint exception and recursively call the
same transactional method.

### Why it fails

After a persistence exception, the current transaction may be rollback-only. A
recursive call is still inside the same failed transaction and cannot safely
recover. Recursion also hides retry bounds.

### Resolution

Use:

- a non-transactional bounded retry coordinator;
- a separate proxied transactional worker;
- a new transaction for each attempt;
- retry only known concurrency conflicts.

### Lesson

Retry the unit of work, not individual SQL statements, and always retry from a new
transaction after a database concurrency failure.

## Challenge 3: outbox status was marked too early

### Initial problem

Platform started `KafkaTemplate.send()` and immediately marked the outbox
`PUBLISHED`. A later broker failure existed only in logs.

### Resolution

Return the Kafka future and wait for broker acknowledgement before the
`PROCESSING -> PUBLISHED` transition. On failure, persist the real exception and
leave the row retryable.

### Remaining limitation

Kafka acknowledgement and the database transition are still not atomic:

```text
Kafka accepts event
process crashes before PUBLISHED commit
poller republishes event
```

This is normal at-least-once behavior. Consumers must be idempotent.

### Lesson

An outbox prevents loss; it does not eliminate duplicate delivery.

## Challenge 4: duplicate failure events released inventory twice

### Failure example

```text
bookedCount = 10
booking B owns 4 seats

first booking-failed:  10 -> 6
duplicate event:        6 -> 2  (wrong)
```

Optimistic locking does not solve this. It serializes writes, but both sequential
writes are valid unless the database knows they represent the same booking.

### Rejected design: status on SlotInventory

One inventory row aggregates many bookings:

```text
SlotInventory(20-Aug, bookedCount=9)
```

A single `RELEASED` flag cannot say whether booking A, B or C was released. The
idempotency state must have the same granularity as the idempotency key.

### Final design: BookingReservation

The initial transaction writes:

```text
increment SlotInventory
insert BookingReservation(bookingId, RESERVED)
insert BookingOutbox(PENDING)
commit
```

Failure handling uses:

```sql
UPDATE booking_reservation
SET status = 'RELEASED',
    released_at = CURRENT_TIMESTAMP
WHERE booking_reference_id = ?
  AND status = 'RESERVED';
```

Only the transaction that updates one row decrements inventory. Duplicate or
concurrent events update zero rows and do nothing.

The state transition and decrement share one transaction. If the decrement fails,
both changes roll back and the reservation remains `RESERVED`.

### Why no separately committed COMPENSATING state

This operation is local and short. Committing `COMPENSATING` before the inventory
update creates ambiguity:

```text
COMPENSATING committed
inventory decremented
process crashes before COMPENSATED
```

Recovery cannot know whether decrement happened. A transient claim state is useful
for long external work, but unnecessary when both changes fit in one ACID
transaction.

### Lesson

Idempotency is usually a conditional business-state transition, not merely a
processed-message table.

### Why an event inbox is still useful

The reservation transition and an inbox solve different problems:

```text
eventId       -> was this exact delivery already processed?
reservationId -> has this business capacity already been released?
```

A stable producer `eventId` plus `UNIQUE(producer,eventId)` avoids repeating all
consumer work and provides an audit trail. The conditional reservation transition
still protects the invariant if a producer mistakenly emits two distinct event IDs
for the same booking failure.

The reviewed Booking service does not currently emit a stable event ID. Platform's
DTO must not generate a replacement during deserialization and treat it as producer
identity. Event IDs must be generated once with the outgoing outbox row and reused
for every publication attempt.

## Challenge 5: local and remote compensation could race

Platform may release capacity after exhausting its own publish retries. Booking may
also later send `booking-failed`.

If each path directly decrements inventory, both can succeed.

Both paths now call the same `BookingReservationService.releaseOnce` operation.
Whichever path transitions `RESERVED -> RELEASED` wins; the other becomes a no-op.

### Lesson

Every path that performs the same business effect must pass through the same
idempotency boundary.

## Challenge 6: early Kafka acknowledgement

Booking intentionally persists inbound work and acknowledges Kafka before doing
the heavier processing. Quartz then performs recovery. This can be valid under high
load because it moves retry pressure from Kafka partitions to controlled database
work queues.

The invariant must be:

```text
persist durable inbound work
commit
acknowledge Kafka
process asynchronously
```

Acknowledging before durable persistence loses messages. Persisting work but using
non-atomic claims allows duplicate processing.

### Booking review findings

The current Booking implementation still has important gaps:

- incoming uniqueness is scoped only by booking reference, not event identity/type;
- business state and outgoing outbox are not always one transaction;
- Quartz workers select rows without an atomic claim/lease;
- retry paths can republish already-sent work;
- event routing contains mismatched event-type cases;
- payment-failure processing can fail to create `booking-failed`.
- `markAsProcessing` writes `PENDING`, so the persisted state does not represent
  worker ownership;
- successful Quartz retry does not mark inbound work `PROCESSED`;
- Booking and Platform event classes have drifted and do not share a required,
  producer-generated event identity.

### Lesson

Early acknowledgement is safe only when the database queue is itself durable,
claimable, observable and idempotent.

## Challenge 7: stale workers and lease expiry

Platform resets `PROCESSING` work after a timeout. Before worker fencing was added,
a worker could be slow rather than dead:

```text
10:00  worker A claims MFB-100
       outbox = PROCESSING, retryCount = 4

10:06  attempt exceeds the five-minute cutoff
       poller changes PROCESSING -> FAILED
       retryCount = 5

       retries are exhausted
       reservation changes RESERVED -> RELEASED
       inventory changes 10 -> 6
       outbox changes FAILED -> COMPENSATED

later  worker A resumes and publishes booking-requested
       the old implementation changes COMPENSATED -> PUBLISHED
```

The worker was slow, not dead. Booking could then process a real booking after
Platform returned its capacity. A timestamp can detect old work, but it cannot
revoke the old worker's authority to commit.

The implemented design adds a unique token to every processing attempt:

```text
claim: status=PROCESSING, processing_owner_token=TOKEN-A

publish completion:
UPDATE outbox
SET status='PUBLISHED'
WHERE id=?
  AND status='PROCESSING'
  AND processing_owner_token=TOKEN-A
```

> [!IMPORTANT]
> ### How `TOKEN-A` and `TOKEN-B` protect two workers
>
> Quartz starts the recovery scan, but `BookingEnrichmentTask.enrich(...)` runs on
> the `bookingTaskExecutor` because it is annotated with `@Async`. The competing
> attempts may therefore run on different threads in one application or on
> different application instances:
>
> ```text
> Thread 1 / Instance 1: worker A
> Thread 2 / Instance 1: worker B
>
> or
>
> Instance 1: worker A
> Instance 2: worker B
> ```
>
> Worker A first claims the row:
>
> ```java
> BookingOutboxProcessingClaim claim =
>         bookingOutboxStateService.claimForProcessing(bookingReferenceId);
> ```
>
> `claimForProcessing` creates a new UUID and stores it with the atomic state
> change:
>
> ```java
> String ownerToken = UUID.randomUUID().toString();
> bookingOutboxDao.markAsProcessing(
>         bookingReferenceId,
>         ownerToken,
>         LocalDateTime.now());
> ```
>
> At this point:
>
> ```text
> worker A local claim: TOKEN-A
> database row:          PROCESSING, TOKEN-A
> ```
>
> The claim is an ordinary immutable Java record held by that method invocation:
>
> ```java
> public record BookingOutboxProcessingClaim(
>         String bookingReferenceId,
>         String ownerToken
> ) {}
> ```
>
> It is not a `ThreadLocal`. A thread-pool thread may be reused later, but each
> invocation has its own claim object.
>
> Suppose worker A stalls for more than five minutes. Recovery changes the row to
> `FAILED` and clears `TOKEN-A`. Worker B then claims the row with a new UUID:
>
> ```text
> worker A local claim: TOKEN-A
> worker B local claim: TOKEN-B
> database row:          PROCESSING, TOKEN-B
> ```
>
> When worker A resumes, it locks the row and compares its old in-memory claim
> with the current database owner:
>
> ```java
> BookingOutbox outboxRecord =
>         bookingOutboxDao.findForUpdate(claim.bookingReferenceId());
>
> if (!BookingOutbox.STATUS_PROCESSING.equals(outboxRecord.getStatus())
>         || !claim.ownerToken().equals(
>                 outboxRecord.getProcessingOwnerToken())) {
>     return false;
> }
> ```
>
> `TOKEN-A` does not equal `TOKEN-B`, so worker A stops without publishing.
> Worker B is the only worker whose local token matches the database token.
>
> The row lock closes the final timing gap. The matching worker keeps the lock
> while Kafka acknowledges the event and while the row changes to `PUBLISHED`:
>
> ```java
> bookingEventProducer.sendBookingRequested(event).join();
> bookingOutboxDao.markPublished(
>         claim.bookingReferenceId(),
>         claim.ownerToken(),
>         LocalDateTime.now());
> ```
>
> Therefore one of two ordered outcomes occurs:
>
> 1. Worker A locks first. Recovery waits; A publishes and commits
>    `PUBLISHED`, so the later reset no longer matches `PROCESSING`.
> 2. Recovery resets first. Worker B receives `TOKEN-B`; when A later locks the
>    row, its `TOKEN-A` check fails.
>
> Failure updates use the same token condition, so worker A also cannot mark
> worker B's attempt `FAILED`. Compensation locks the row and requires no active
> token before releasing capacity.

Kafka and PostgreSQL still cannot form one distributed transaction. A process crash
after Kafka acknowledges but before the database commits may resend the same stable
event ID. Downstream event-ID deduplication handles that remaining delivery window.

### Lesson

A timestamp tells you work is old. The token identifies the current attempt, and
the row lock keeps that ownership stable through publication.

## Challenge 8: HTTP retries are different from Kafka retries

Kafka failure idempotency protects inventory release, but that alone did not make
the booking creation HTTP endpoint safe to retry.

If the client times out and retries:

```text
request 1:
  generate MFB-100
  reserve inventory
  create BookingReservation
  create BookingOutbox
  database COMMIT succeeds

  connection drops before the client receives HTTP 202

client retry:
  generate MFB-101
  reserve the same inventory again
```

Previously, the server could not recognize the duplicate because it generated a
new booking ID for every request.

Quartz retry is different: it finds the existing `MFB-100` outbox and restarts
enrichment without reserving again. The duplicate risk comes from retrying the HTTP
endpoint, whether initiated by the user, browser/mobile client, gateway or retrying
HTTP library.

The probability is lower because the synchronous path is short, but the impact is
high. It is P1 for a controlled client/gateway that never automatically retries
POST requests, and becomes P0 when mobile uncertainty or automatic retries are
expected.

Platform now requires an `Idempotency-Key`, uniquely scoped to caller and
operation. It stores the completed response coherently with the reservation and
outbox. The same key and payload return the original booking reference; the same
key with a different payload is rejected with `409`.

### Lesson

Idempotency must be designed at every retry boundary: HTTP, scheduler and message
consumer boundaries are separate.

## Challenge 9: high-load fast path can contradict durable state

Platform commits inventory/reservation/outbox and then submits async enrichment.
The executor has a bounded queue. Previously, submission rejection could fail the
HTTP call after the durable booking already existed.

```text
inventory + reservation + outbox COMMIT
             |
             v
bookingTaskExecutor rejects enrich(MFB-100)
             |
             v
exception reaches controller before return bookingId

database: MFB-100 exists and is PENDING
client:   receives an error
```

Quartz eventually processes `MFB-100`, so this was not message loss. The defect was
that the API outcome contradicted committed state.

The durable outbox means async submission is only an optimization. Platform now
catches and logs fast-path rejection while returning the committed booking
reference; Quartz can pick it up. An after-commit dispatcher or dedicated outbox
publisher remains a possible structural improvement.

### Lesson

Once durable state commits, optional fast-path failure must not change the API
outcome.

## Challenge 10: schema and historical-state migration

Adding `BookingReservation` requires more than creating a table. Platform outbox
status says whether `booking-requested` was published, not whether Booking later
confirmed or failed it.

Therefore a migration that maps every historical `PUBLISHED` row to `RESERVED`
cannot prove current business state. Historical data must be reconciled with
Booking, or migration must be limited to a known active window.

### Safe reconciliation process

Do not derive reservation state from Platform outbox status alone. Export the
authoritative historical lifecycle from Booking and join it to Platform's outbox
payload by booking ID:

```text
Platform: bookingId, slotMapperId, bookingDate, guestCount
Booking:  bookingId, final/current bookingStatus
```

Apply an explicitly approved lifecycle mapping:

```text
CONFIRMED / PENDING / PAYMENT_PENDING -> RESERVED
FAILED / CANCELLED / EXPIRED          -> RELEASED
```

Then validate the reconstructed rows against aggregate inventory:

```text
expected SlotInventory.bookedCount
  = SUM(guestCount for RESERVED reservations grouped by mapper/date)
```

Any difference between expected and stored inventory belongs in an exception
report; it must not be silently overwritten.

A production rollout should use a traffic pause or migration cutoff timestamp:

```text
create table
capture Platform and Booking state at the same cutoff
backfill reservations
validate grouped totals
resolve exceptions
enable the new compensation path
resume traffic
```

For non-production data, rebuilding inventory and reservations from a known clean
baseline may be simpler than cross-service reconciliation.

The project currently uses `ddl-auto:update` plus SQL files. Production should use
versioned Flyway/Liquibase migrations executed as part of deployment.

### Lesson

Schema migration is easy; business-state migration requires ownership-aware
reconciliation.

## Challenge 11: compensation errors and rollback-only transactions

Compensation currently invokes the reservation release inside the scheduler's
existing transaction and catches exceptions in that same method. A persistence
exception may mark the shared transaction rollback-only:

```text
reservation RESERVED -> RELEASED
inventory decrement throws
catch attempts outbox -> DEAD
method exits
whole transaction rolls back
```

The Java code and logs can appear to mark `DEAD`, while the database retains the
old `FAILED` row and Quartz attempts it again.

A missing inventory row should be rare because inventory, reservation and outbox
are created atomically. Practical failures still include:

```text
retryable:
  optimistic conflict
  deadlock
  connection loss
  timeout or database failover

terminal/data-integrity:
  historical migration mismatch
  inventory count below reservation quantity
  manual/bug-driven data corruption
  incompatible event and reservation details
  deployment/schema mismatch
```

Recovery must classify these outcomes and persist retry state or terminal
investigation state in a transaction that is not already rollback-only.

## Challenge 12: event identity and schema evolution

`bookingId` identifies the business aggregate. It does not uniquely identify an
event:

```text
bookingId -> which booking?
eventId   -> which exact durable event?
```

After an ambiguous send, two Kafka records may be retries of the same durable event.
A stable `eventId` must be generated once with the outgoing outbox row and reused
for every publish attempt. Generating a new ID during enrichment or retry defeats
event-level deduplication.

The event also needs explicit contract versioning. Replayed data and rolling
deployments may contain multiple valid shapes:

```text
v1: booking date absent
v2: booking date required
v3: reservation identity added
```

Without `schemaVersion`, compatibility is implicit and consumers cannot reliably
apply version-specific validation or transformation.

Business idempotency still uses domain identity. For inventory release,
`BookingReservation RESERVED -> RELEASED` remains the final guard even when two
different event IDs describe the same booking failure.

## Target state machines

### Platform reservation

```text
RESERVED --failure/cancellation winner--> RELEASED
RELEASED --duplicate--> RELEASED
```

`RESERVED` means capacity remains held, including confirmed bookings.

### Platform outgoing outbox

```text
PENDING -> PROCESSING(attempt token)
PROCESSING -> PUBLISHED
PROCESSING -> FAILED
FAILED -> PROCESSING
FAILED -> COMPENSATED
FAILED -> DEAD
```

Every transition must be conditional on the expected state and, for worker
completion, the current attempt token.

### Booking inbound work

```text
RECEIVED/PENDING -> PROCESSING(lease) -> PROCESSED
                         |
                         +-> FAILED -> PROCESSING
```

Business state and outgoing events must commit together.

## Review checklist for similar systems

1. What is the business idempotency key?
2. Is its state stored at the same granularity as that key?
3. Which writes must be one local transaction?
4. What happens if the process crashes after every individual step?
5. Can two workers both claim the same row?
6. Can an expired worker still commit? Is there a fencing token?
7. Does broker acknowledgement happen before or after durable state?
8. Can the same message be processed twice safely?
9. Can the same HTTP request be submitted twice safely?
10. Are retryable and terminal failures classified differently?
11. Does DLT routing match the topic operations actually monitor?
12. Are migrations versioned and historical states reconciled?
13. Are concurrency guarantees proven against the production database?
14. Are queues, partitions, timeouts and rejection behavior sized for load?

## Final architectural principle

Reliable event-driven systems do not prevent every duplicate. They make duplicates,
retries, crashes and reordering safe by expressing business effects as durable,
conditional state transitions inside clear ownership boundaries.
