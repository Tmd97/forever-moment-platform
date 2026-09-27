# Consumer event idempotency

## Previous behavior

`BOOKING_FAILED` redelivery relied only on `booking_reservation.status`. The guarded
`RESERVED -> RELEASED` update correctly prevented capacity from being restored twice,
but Platform did not remember which event delivery had already completed.

That business guard cannot distinguish an exact Kafka redelivery from a later,
legitimate event for the same booking. It also cannot provide producer-scoped event
identity, processing audit data, or a reusable deduplication boundary for other
Booking-origin consumers.

## New behavior

The consumer and processor live in `com.forvmom.core.idempotency.event`; durable
entities and DAOs remain in the data module's established packages.

Identity-bearing events are claimed in `consumer_event_inbox` using the unique key
`(producer, event_id)`. The inbox claim, reservation transition, inventory update, and
inbox `PROCESSED` transition share one database transaction.

- The same producer and event ID is an exact duplicate and its business handler is
  skipped.
- The same event ID from different producers is distinct.
- Different event IDs from one producer are distinct even when booking ID and event
  type are the same.
- Reservation state remains the separate business-effect guard, so any number of
  legitimate failure events can be recorded while capacity is released once.
- If processing fails, the transaction rolls back the inbox claim and business effect;
  Kafka is not acknowledged and redelivery can try again.
- Concurrent duplicates are serialized by PostgreSQL's unique index. `ON CONFLICT DO
  NOTHING` converts the loser into a normal duplicate result rather than leaking a
  unique-constraint error.

Examples:

| Producer | Event ID | Booking | Result |
| --- | --- | --- | --- |
| `booking-service` | `evt-10` | `MFB-1` | processed and capacity released |
| `booking-service` | `evt-10` | `MFB-1` | exact duplicate, skipped |
| `recovery-service` | `evt-10` | `MFB-1` | distinct event, recorded; release is a business no-op |
| `booking-service` | `evt-11` | `MFB-1` | distinct event, recorded; release is a business no-op |

## Legacy compatibility and malformed identity

Events with both `producer` and `eventId` absent follow the existing reservation guard
without an inbox row. Platform never invents a producer or event ID for these legacy
messages.

An event with only one identity field is malformed and is rejected. The listener does
not acknowledge it, so the configured Kafka retry/DLT policy remains responsible for
delivery failure handling.

## Migration and rollout

Apply `moment_forever_data/src/main/resources/sql/add_consumer_event_inbox.sql` before
deploying the Platform application version that writes inbox rows. The migration is
additive and requires no backfill because historical deliveries did not have durable
consumer identity.

Recommended rollout order:

1. Deploy Booking producers that populate both `producer` and `eventId`.
2. Apply the Platform inbox migration.
3. Deploy Platform consumer code.
4. Monitor malformed partial-identity failures and Kafka DLT traffic.

Older Booking producers remain supported through the fully legacy path during the
transition.
