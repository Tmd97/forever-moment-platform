-- Vendors Feature: Vendor business profiles linked to auth_users
-- Vendors can self-register or be created by SuperAdmin/Admin.
-- Vendor self-service allows vendors to manage their own business profile and toggle status.

CREATE TABLE IF NOT EXISTS vendors (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255),
    vendor_code VARCHAR(50) UNIQUE,
    business_name VARCHAR(150) NOT NULL,
    category VARCHAR(100) NOT NULL,
    contact_name VARCHAR(100) NOT NULL,
    contact_email VARCHAR(150) NOT NULL UNIQUE,
    contact_phone VARCHAR(20),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    auth_user_id BIGINT UNIQUE NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_on TIMESTAMP,
    CONSTRAINT fk_vendors_auth_user FOREIGN KEY (auth_user_id) REFERENCES auth_users (id) ON DELETE CASCADE,
    CONSTRAINT ck_vendors_status CHECK (status IN ('ACTIVE', 'PENDING', 'INACTIVE'))
);

CREATE INDEX IF NOT EXISTS idx_vendors_auth_user_id ON vendors (auth_user_id) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_vendors_status ON vendors (status) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_vendors_category ON vendors (category) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_vendors_vendor_code ON vendors (vendor_code) WHERE deleted = FALSE;