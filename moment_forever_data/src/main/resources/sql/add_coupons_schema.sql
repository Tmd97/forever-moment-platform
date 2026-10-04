-- Coupons Feature: Promotional discount codes and Experience-Coupon attachments.
-- Supports PERCENTAGE and FIXED_AMOUNT discounts with optional min booking amount & max discount caps.

CREATE TABLE IF NOT EXISTS coupons (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255),
    code VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(500),
    discount_type VARCHAR(20) NOT NULL DEFAULT 'PERCENTAGE',
    discount_value NUMERIC(10, 2) NOT NULL,
    max_discount_amount NUMERIC(10, 2),
    min_booking_amount NUMERIC(10, 2),
    valid_from DATE,
    valid_to DATE,
    usage_limit INT,
    usage_count INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_on TIMESTAMP,
    CONSTRAINT ck_coupons_discount_type CHECK (discount_type IN ('PERCENTAGE', 'FIXED_AMOUNT'))
);

CREATE TABLE IF NOT EXISTS experience_coupon_mappers (
    id BIGSERIAL PRIMARY KEY,
    experience_id BIGINT NOT NULL,
    coupon_id BIGINT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_on TIMESTAMP,
    CONSTRAINT fk_exp_coupon_experience FOREIGN KEY (experience_id) REFERENCES experience (id) ON DELETE CASCADE,
    CONSTRAINT fk_exp_coupon_coupon FOREIGN KEY (coupon_id) REFERENCES coupons (id) ON DELETE CASCADE,
    CONSTRAINT uk_experience_coupon UNIQUE (experience_id, coupon_id)
);

CREATE INDEX IF NOT EXISTS idx_coupons_code ON coupons (code) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_coupons_is_active ON coupons (is_active) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_exp_coupon_experience_id ON experience_coupon_mappers (experience_id) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_exp_coupon_coupon_id ON experience_coupon_mappers (coupon_id) WHERE deleted = FALSE;