-- V19: Add app_name to roles and introduce SUPERADMIN role

-- 1. Add app_name column to auth.roles
ALTER TABLE auth.roles ADD COLUMN IF NOT EXISTS app_name VARCHAR(50);

-- 2. Insert SUPERADMIN role
INSERT INTO auth.roles (id, name, description, is_active, app_name)
VALUES (gen_random_uuid(), 'SUPERADMIN', 'Super Administrator with unrestricted full-suite authority', true, 'admin')
ON CONFLICT (name) DO NOTHING;

-- 3. Backfill app_name on existing roles
UPDATE auth.roles SET app_name = 'assets' WHERE lower(name) LIKE 'asset%' AND (app_name IS NULL OR app_name = '');
UPDATE auth.roles SET app_name = 'tracker' WHERE lower(name) IN ('viewer', 'editor', 'owner') AND (app_name IS NULL OR app_name = '');
UPDATE auth.roles SET app_name = 'admin' WHERE lower(name) IN ('admin', 'superadmin') AND (app_name IS NULL OR app_name = '');

-- 4. Grant SUPERADMIN role to keolawong
INSERT INTO auth.user_roles (user_id, role_id, status)
SELECT u.id, r.id, 'active'
FROM auth.users u
CROSS JOIN auth.roles r
WHERE u.username = 'keolawong' AND r.name = 'SUPERADMIN'
ON CONFLICT (user_id, role_id) DO NOTHING;

-- 5. Give SUPERADMIN all role_pages
INSERT INTO auth.role_pages (role_id, page_id, is_active, created_by, updated_by)
SELECT r.id, p.id, true, 'system', 'system'
FROM auth.roles r
CROSS JOIN auth.pages p
WHERE r.name = 'SUPERADMIN'
ON CONFLICT (role_id, page_id) DO NOTHING;

-- 6. Refresh backwards-compatible view in tracker schema
CREATE OR REPLACE VIEW tracker.roles AS SELECT * FROM auth.roles;
GRANT SELECT, INSERT, UPDATE, DELETE ON tracker.roles TO ${db_app_user};
