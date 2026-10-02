-- FAQ feature: flat, globally-ordered list shown on the public Help/FAQ page.
-- Not grouped into categories and not attached to individual experiences.

CREATE TABLE IF NOT EXISTS faqs (
    id BIGSERIAL PRIMARY KEY,
    question TEXT NOT NULL,
    answer TEXT NOT NULL,
    display_order INTEGER NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_on TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_on TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_faqs_display_order
    ON faqs (display_order)
    WHERE deleted = FALSE;
