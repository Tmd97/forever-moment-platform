ALTER TABLE booking_outbox
    ADD COLUMN IF NOT EXISTS event_id VARCHAR(60),
    ADD COLUMN IF NOT EXISTS event_producer VARCHAR(60),
    ADD COLUMN IF NOT EXISTS schema_version INTEGER,
    ADD COLUMN IF NOT EXISTS occurred_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS correlation_id VARCHAR(100),
    ADD COLUMN IF NOT EXISTS causation_id VARCHAR(100);

UPDATE booking_outbox
SET event_id = 'legacy-booking-outbox-' || id
WHERE event_id IS NULL;

UPDATE booking_outbox
SET event_producer = 'moment-forever-core',
    schema_version = 1,
    occurred_at = created_at AT TIME ZONE 'UTC',
    correlation_id = booking_reference_id
WHERE event_producer IS NULL
   OR schema_version IS NULL
   OR occurred_at IS NULL
   OR correlation_id IS NULL;

ALTER TABLE booking_outbox
    ALTER COLUMN event_id SET NOT NULL,
    ALTER COLUMN event_producer SET NOT NULL,
    ALTER COLUMN schema_version SET NOT NULL,
    ALTER COLUMN occurred_at SET NOT NULL,
    ALTER COLUMN correlation_id SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_booking_outbox_event_id
    ON booking_outbox (event_id);
