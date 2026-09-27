CREATE TABLE IF NOT EXISTS consumer_event_inbox (
    id BIGSERIAL PRIMARY KEY,
    producer VARCHAR(100) NOT NULL,
    event_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(60),
    correlation_id VARCHAR(100),
    status VARCHAR(20) NOT NULL,
    received_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP,
    CONSTRAINT uk_consumer_event_inbox_identity UNIQUE (producer, event_id),
    CONSTRAINT ck_consumer_event_inbox_status CHECK (status IN ('RECEIVED', 'PROCESSED'))
);

CREATE INDEX IF NOT EXISTS idx_consumer_event_inbox_aggregate
    ON consumer_event_inbox (aggregate_id, event_type);
