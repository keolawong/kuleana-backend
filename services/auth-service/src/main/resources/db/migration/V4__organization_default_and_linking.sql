-- V4: Organization Default Designation and Account Linking Support

-- 1. Add is_default column to organizations
ALTER TABLE auth.organizations
    ADD COLUMN IF NOT EXISTS is_default BOOLEAN NOT NULL DEFAULT false;

-- 2. Ensure default-org is designated as the default organization
UPDATE auth.organizations
SET is_default = true
WHERE slug = 'default-org'
  AND NOT EXISTS (
      SELECT 1 FROM auth.organizations WHERE is_default = true
  );

-- 3. Unique partial index ensuring at most one organization is marked as default
CREATE UNIQUE INDEX IF NOT EXISTS idx_organizations_single_default
    ON auth.organizations (is_default)
    WHERE is_default = true;
