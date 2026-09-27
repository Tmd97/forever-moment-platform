# Booking reliability organization

## Why this refactor

Booking reliability code previously accumulated in broad `services`, `consumer`,
and `scheduler` packages. Names such as `BookingCreationTxService`,
`BookingCreationTxWorker`, and `OutboxCleanupService` obscured transaction
boundaries and mixed HTTP request identity, outgoing delivery recovery, and
incoming event identity.

The refactor is organizational and behavior-preserving. It separates retry
boundaries in the core module, expands ambiguous internal names, and keeps all
external compatibility names unchanged.

## Package map

| Package | Responsibility | Primary types |
| --- | --- | --- |
| `com.forvmom.core.idempotency.request` | HTTP key normalization, request fingerprint, atomic claim/replay, stable response | `BookingRequestIdempotencyCoordinator`, `BookingRequestFingerprintService`, `BookingRequestIdempotencyClaim`, `BookingInitiationResult` |
| `com.forvmom.core.idempotency.event` | Incoming `booking-failed` delivery identity and acknowledgement boundary | `BookingFailedConsumer`, `BookingFailureEventProcessor` |
| `com.forvmom.core.retries` | Outgoing outbox state, recovery, compensation, and scheduled retention | `BookingOutboxStateService`, `BookingReliabilityMaintenanceService` |
| `com.forvmom.core.services` | Booking workflow, bounded inventory-conflict retry, and atomic creation transaction | `BookingOrchestrationService`, `BookingCreationRetryService`, `BookingCreationTransactionService` |
| `com.forvmom.core.scheduler` | Quartz-compatible job shells and stable job identities | `OutboxRetriesJob`, `OutboxCleanupJob` |
| `com.forvmom.data.dao` / `entities` | Durable outbox, inbox, reservation, and HTTP request state | Existing DAO/entity types remain in established data-module packages |

Tests mirror the focused core packages. Persistence types were not moved because
the flat data-module layout is established across the repository and a package
migration would add JPA/component-scanning risk without changing responsibilities.
Quartz job classes also remain in `scheduler` because JDBC job stores can retain
job-class metadata.

## Responsibility boundaries

HTTP request idempotency answers whether a client retry represents the same
booking creation command. It is scoped by operation, authenticated caller, and a
digest of `Idempotency-Key`; it replays the original HTTP result.

The outgoing booking outbox owns one stable event intent for an accepted booking.
Its retry loop may enrich and publish the same durable intent again, but it never
creates another reservation.

The incoming consumer inbox identifies Kafka deliveries by producer and event ID.
It is separate from the `BookingReservation RESERVED -> RELEASED` business guard,
which ensures capacity is released once even across distinct events.

## Naming changes

| Previous internal name | Current internal name | Rationale |
| --- | --- | --- |
| `BookingCreationTxWorker` | `BookingCreationTransactionService` | Identifies the actual atomic transaction boundary |
| `BookingCreationTxService` | `BookingCreationRetryService` | Identifies bounded retry around fresh transactions |
| `BookingFailureEventService` | `BookingFailureEventProcessor` | Describes claim plus business processing |
| `BookingIdempotencyClaim` | `BookingRequestIdempotencyClaim` | Distinguishes HTTP request identity from Kafka event identity |
| `fingerprint` | `createFingerprint` | Uses an action name for computation |
| `start` / `StartResult` | `claimOrReplay` / `ClaimResult` | Exposes the two possible idempotency outcomes |
| `retriesForStuckEnrichments` | `recoverUnresolvedOutboxRecords` | Covers stuck reset, retry, and compensation |
| `OutboxCleanupService` | `BookingReliabilityMaintenanceService` | Reflects recovery, compensation, outbox cleanup, and request-record retention |

## Compatibility

No external contract changed during this refactor. The following deliberately keep
their existing names and values:

- `POST /admin/bookings`, `Idempotency-Key`, `Idempotency-Replayed`,
  `X-User-Id`, and response JSON fields including `bookingId`.
- PostgreSQL tables, columns, constraints, and status strings.
- Kafka topics, payload JSON, producer/event identity, consumer group, and
  acknowledgement behavior.
- Quartz job and trigger identities, scheduler intervals, retention windows, and
  job class packages.
- Booking reference and event ID generation semantics.

Splitting outbox retention and HTTP idempotency retention into separate Quartz
transactions is intentionally deferred: it would change partial-failure behavior.
Moving persistence classes or Quartz jobs is also deferred because framework and
stored-metadata compatibility require explicit rollout planning.
