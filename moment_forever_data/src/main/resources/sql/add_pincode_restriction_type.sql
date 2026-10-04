-- Experience-Location Pincode Restriction Type:
-- Supports WHITELIST (Allow) mode and BLACKLIST (Deny / Reverse) mode.
-- In BLACKLIST mode, selected pincodes are disabled / unserviceable, and all other pincodes in the location are serviceable.

ALTER TABLE experience_location_mappers
ADD COLUMN IF NOT EXISTS pincode_restriction_type VARCHAR(20) DEFAULT 'WHITELIST';