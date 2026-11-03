-- V24: Add Org Id to Operation and default to seeded default org
ALTER TABLE asset.operation
    ADD COLUMN org_id UUID;

UPDATE asset.operation
SET org_id = 'a0000000-0000-0000-0000-000000000001'
WHERE org_id IS NULL;

ALTER TABLE asset.operation
    ALTER COLUMN org_id SET NOT NULL;

CREATE INDEX idx_operation_org_id ON asset.operation(org_id);

GRANT SELECT, INSERT, UPDATE, DELETE ON asset.operation TO ${db_app_user};
