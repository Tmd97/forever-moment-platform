-- Support/contact feature: simple one-shot support query form.
-- Guests submit name/email/phone directly; logged-in users are auto-filled
-- from their application_users profile and linked via application_user_id.
-- Lifecycle is OPEN -> RESOLVED only (no reply-thread/ticketing workflow).

CREATE TABLE IF NOT EXISTS support_queries (
    id BIGSERIAL PRIMARY KEY,
    reference_id VARCHAR(40) UNIQUE,
    application_user_id BIGINT,
    name VARCHAR(150) NOT NULL,
    email VARCHAR(150) NOT NULL,
    phone VARCHAR(20),
    subject VARCHAR(200),
    message TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    resolved_on TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_on TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_on TIMESTAMP,
    CONSTRAINT fk_support_queries_application_user
        FOREIGN KEY (application_user_id) REFERENCES application_users (id),
    CONSTRAINT ck_support_queries_status CHECK (status IN ('OPEN', 'RESOLVED'))
);

CREATE INDEX IF NOT EXISTS idx_support_queries_application_user
    ON support_queries (application_user_id)
    WHERE deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_support_queries_status
    ON support_queries (status)
    WHERE deleted = FALSE;
