# Booking HTTP request idempotency

## Contract

`POST /admin/bookings` requires the standard `Idempotency-Key` header. Keys must
contain a non-whitespace value of at most 200 characters. The key is scoped by
`CREATE_BOOKING` and the trusted gateway `X-User-Id`, so two authenticated callers
may use the same key independently.
Only a SHA-256 digest of the key is persisted; clients must not put credentials or
other secrets in idempotency keys.

The header is required rather than transitional-optional because an optional path
would continue creating duplicate reservations for retried requests. Clients can
send the header before the Platform rollout because older deployments ignore it.

```http
POST /admin/bookings
X-User-Id: 42
X-User-Roles: USER
Idempotency-Key: 8c02c91e-2ed9-4cb3-8d3f-7882c21a5668
Content-Type: application/json

{
  "timeSlotMapperId": 100,
  "bookingDate": "2026-09-01",
  "guestCount": 2,
  "pincode": "560001",
  "addonMapperIds": [3, 9]
}
```

The first accepted request returns `202 Accepted`. An exact replay returns the
stored HTTP status and JSON body and adds `Idempotency-Replayed: true`. A key reused
by the same caller for a different normalized request returns `409 Conflict` with
no booking side effects. Add-on order and null-versus-empty add-ons are normalized;
other request values remain significant.

## Concurrency and failure behavior

Core request-idempotency code lives in
`com.forvmom.core.idempotency.request`; booking transaction and retry orchestration
remain in `com.forvmom.core.services`.

`booking_request_idempotency` owns HTTP retry identity. PostgreSQL atomically elects
one owner with `INSERT ... ON CONFLICT DO NOTHING`. The owner token is checked under
a row lock in the same transaction that reserves inventory and creates
`BookingReservation` and `BookingOutbox`; that transaction also stores the stable
booking reference, HTTP status, and serialized response and changes the request to
`COMPLETED`.

While an owner is `IN_PROGRESS`, another exact request returns `409 Conflict` with
`Retry-After: 1`; it never enters booking business logic. A domain or persistence
failure rolls back reservation, inventory, outbox, and completion together, then
marks the request `FAILED`. The same fingerprint can reclaim `FAILED` immediately.
An abandoned `IN_PROGRESS` claim is reclaimable after its five-minute lease; the
row lock prevents takeover from racing a still-running owner. A different
fingerprint always conflicts, including against failed or in-progress records.

## Separate identities

| Concern | Durable record | Identity and effect |
| --- | --- | --- |
| HTTP client retry | `booking_request_idempotency` | operation + caller + key; replays the original HTTP result |
| Outgoing Kafka work | `booking_outbox` | one stable event ID for the one accepted booking |
| Incoming Kafka redelivery | `consumer_event_inbox` | producer + event ID; deduplicates consumed events |

An HTTP replay creates no reservation, booking reference, outbox row, or event ID.
Kafka delivery remains at-least-once and is handled independently.

## Migration, rollout, and retention

1. Update booking clients and the gateway to forward `Idempotency-Key`; verify the
   gateway still overwrites untrusted `X-User-Id`.
2. Apply
   `moment_forever_data/src/main/resources/sql/add_booking_request_idempotency.sql`.
   It is additive and requires no backfill.
3. Deploy Platform and monitor `409` responses, failed claims, and table growth.

`BookingReliabilityMaintenanceService`, invoked by the existing daily Quartz
cleanup job, deletes only `COMPLETED` idempotency rows older
than 30 days. `FAILED` and `IN_PROGRESS` rows are retained for recovery and
operational investigation. Reusing a key after its completed row expires is treated
as a new request, so clients must not retry longer than the documented 30-day
window.
