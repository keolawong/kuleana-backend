-- Migration: V26__Add_Org_To_Device.sql
-- Add org_id to asset.device with default org backfill, index, and per-tenant unique constraints

-- 1. Add org_id column if not exists
ALTER TABLE asset.device ADD COLUMN IF NOT EXISTS org_id UUID;

-- 2. Backfill existing records with default organization
UPDATE asset.device
SET org_id = 'a0000000-0000-0000-0000-000000000001'
WHERE org_id IS NULL;

-- 3. Set NOT NULL
ALTER TABLE asset.device ALTER COLUMN org_id SET NOT NULL;

-- 4. Drop global uniqueness constraints and add per-organization constraints
ALTER TABLE asset.device DROP CONSTRAINT IF EXISTS uq_device_id;
ALTER TABLE asset.device DROP CONSTRAINT IF EXISTS uq_device_number;

ALTER TABLE asset.device ADD CONSTRAINT uq_device_id_org UNIQUE (device_id, org_id);
ALTER TABLE asset.device ADD CONSTRAINT uq_device_number_org UNIQUE (device_number, org_id);

-- 5. Add index on org_id
CREATE INDEX IF NOT EXISTS idx_device_org_id ON asset.device(org_id);

-- 6. Permissions
GRANT SELECT, INSERT, UPDATE, DELETE ON asset.device TO ${db_app_user};
