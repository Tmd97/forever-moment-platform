ALTER TABLE booking_outbox
ALTER COLUMN status TYPE VARCHAR(50);

ALTER TABLE booking_outbox
ALTER COLUMN failure_reason TYPE VARCHAR(1000);

ALTER TABLE booking_outbox
ADD COLUMN IF NOT EXISTS processing_started_at TIMESTAMP;

UPDATE booking_outbox
SET processing_started_at = created_at
WHERE status = 'PROCESSING'
  AND processing_started_at IS NULL;