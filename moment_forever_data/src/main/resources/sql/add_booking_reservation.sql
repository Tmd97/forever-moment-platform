CREATE TABLE IF NOT EXISTS booking_reservation (
    booking_reference_id VARCHAR(60) PRIMARY KEY,
    slot_mapper_id BIGINT NOT NULL,
    booking_date DATE NOT NULL,
    guest_count INTEGER NOT NULL CHECK (guest_count > 0),
    status VARCHAR(20) NOT NULL CHECK (status IN ('RESERVED', 'RELEASED')),
    reserved_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    released_at TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

INSERT INTO booking_reservation (
    booking_reference_id,
    slot_mapper_id,
    booking_date,
    guest_count,
    status,
    reserved_at,
    released_at,
    version
)
SELECT
    booking_reference_id,
    (payload::jsonb ->> 'slotMapperId')::BIGINT,
    (payload::jsonb ->> 'bookingDate')::DATE,
    (payload::jsonb ->> 'guestCount')::INTEGER,
    CASE WHEN status = 'COMPENSATED' THEN 'RELEASED' ELSE 'RESERVED' END,
    created_at,
    compensated_at,
    0
FROM booking_outbox
WHERE payload::jsonb ?& ARRAY['slotMapperId', 'bookingDate', 'guestCount']
ON CONFLICT (booking_reference_id) DO NOTHING;
