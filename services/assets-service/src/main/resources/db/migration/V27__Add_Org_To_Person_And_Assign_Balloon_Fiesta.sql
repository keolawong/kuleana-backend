-- Migration: V27__Add_Org_To_Person_And_Assign_Balloon_Fiesta.sql
-- 1. Reassign all existing devices to Albuquerque International Balloon Fiesta
UPDATE asset.device
SET org_id = 'b0000000-0000-0000-0000-000000000002'
WHERE org_id = 'a0000000-0000-0000-0000-000000000001';

-- 2. Add org_id column to asset.person
ALTER TABLE asset.person ADD COLUMN IF NOT EXISTS org_id UUID;

-- 3. Backfill person org_id from linked shifts & operations
UPDATE asset.person p
SET org_id = o.org_id
FROM asset.shift s
JOIN asset.operation o ON s.operation_id = o.operation_id
WHERE p.shift_id = s.shift_id
  AND o.org_id IS NOT NULL;

-- 4. For any unassigned or remaining personnel, set to Albuquerque International Balloon Fiesta
UPDATE asset.person
SET org_id = 'b0000000-0000-0000-0000-000000000002'
WHERE org_id IS NULL;

-- 5. Enforce NOT NULL and add index on org_id
ALTER TABLE asset.person ALTER COLUMN org_id SET NOT NULL;
CREATE INDEX IF NOT EXISTS idx_person_org_id ON asset.person(org_id);

-- 6. Grant permissions to db_app_user
GRANT SELECT, INSERT, UPDATE, DELETE ON asset.person TO ${db_app_user};
