CREATE TABLE IF NOT EXISTS booking_request_idempotency (
    id BIGSERIAL PRIMARY KEY,
    operation VARCHAR(60) NOT NULL,
    caller_id VARCHAR(100) NOT NULL,
    idempotency_key_hash VARCHAR(64) NOT NULL,
    request_fingerprint VARCHAR(64) NOT NULL,
    status VARCHAR(20) NOT NULL,
    owner_token VARCHAR(36) NOT NULL,
    lease_expires_at TIMESTAMP NOT NULL,
    booking_reference_id VARCHAR(60),
    http_status INTEGER,
    response_body TEXT,
    failure_reason VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    CONSTRAINT uk_booking_request_idempotency_scope
        UNIQUE (operation, caller_id, idempotency_key_hash),
    CONSTRAINT ck_booking_request_idempotency_status
        CHECK (status IN ('IN_PROGRESS', 'COMPLETED', 'FAILED')),
    CONSTRAINT ck_booking_request_idempotency_completed
        CHECK (
            status <> 'COMPLETED'
            OR (
                booking_reference_id IS NOT NULL
                AND http_status IS NOT NULL
                AND response_body IS NOT NULL
                AND completed_at IS NOT NULL
            )
        )
);

CREATE INDEX IF NOT EXISTS idx_booking_request_idempotency_retention
    ON booking_request_idempotency (completed_at)
    WHERE status = 'COMPLETED';
