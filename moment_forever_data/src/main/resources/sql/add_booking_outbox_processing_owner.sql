-- Add one ownership token per outbox processing attempt.
ALTER TABLE booking_outbox
    ADD COLUMN IF NOT EXISTS processing_owner_token VARCHAR(36);

-- Old workers have no token, so safely return their rows to the retry flow.
UPDATE booking_outbox
SET status = 'FAILED',
    retry_count = retry_count + 1,
    processing_started_at = NULL,
    processing_owner_token = NULL
WHERE status = 'PROCESSING';

-- Helps the Quartz job find workers whose processing lease timed out.
CREATE INDEX IF NOT EXISTS idx_booking_outbox_processing_lease
    ON booking_outbox (processing_started_at)
    WHERE status = 'PROCESSING';

-- PROCESSING must have an owner and all other states must not have one.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'ck_booking_outbox_processing_owner'
          AND conrelid = 'booking_outbox'::regclass
    ) THEN
        ALTER TABLE booking_outbox
            ADD CONSTRAINT ck_booking_outbox_processing_owner
            CHECK (
                (
                    status = 'PROCESSING'
                    AND processing_owner_token IS NOT NULL
                    AND processing_started_at IS NOT NULL
                )
                OR (
                    status <> 'PROCESSING'
                    AND processing_owner_token IS NULL
                )
            );
    END IF;
END
$$;
