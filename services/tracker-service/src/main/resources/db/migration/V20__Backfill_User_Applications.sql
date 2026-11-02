-- V20: Backfill user_applications from existing user_roles

-- 1. Backfill Assets application for users who have ASSET-* roles
INSERT INTO auth.user_applications (id, user_id, app_name, status, primary_role)
SELECT DISTINCT gen_random_uuid(), ur.user_id, 'assets', 'active',
  CASE 
    WHEN lower(r.name) LIKE '%lead%' THEN 'LEAD'
    WHEN lower(r.name) LIKE '%it%' THEN 'IT'
    ELSE 'OPERATOR'
  END
FROM auth.user_roles ur
JOIN auth.roles r ON r.id = ur.role_id
WHERE lower(r.name) LIKE 'asset%'
ON CONFLICT (user_id, app_name) DO NOTHING;

-- 2. Backfill Tracker application for users who have tracker roles
INSERT INTO auth.user_applications (id, user_id, app_name, status, primary_role)
SELECT DISTINCT gen_random_uuid(), ur.user_id, 'tracker', 'active',
  CASE 
    WHEN lower(r.name) = 'owner' THEN 'OWNER'
    WHEN lower(r.name) = 'editor' THEN 'EDITOR'
    ELSE 'VIEWER'
  END
FROM auth.user_roles ur
JOIN auth.roles r ON r.id = ur.role_id
WHERE lower(r.name) IN ('viewer', 'editor', 'owner')
ON CONFLICT (user_id, app_name) DO NOTHING;

-- 3. Backfill Admin application for users who have admin or superadmin roles
INSERT INTO auth.user_applications (id, user_id, app_name, status, primary_role)
SELECT DISTINCT gen_random_uuid(), ur.user_id, 'admin', 'active', 'ADMIN'
FROM auth.user_roles ur
JOIN auth.roles r ON r.id = ur.role_id
WHERE lower(r.name) IN ('admin', 'superadmin')
ON CONFLICT (user_id, app_name) DO NOTHING;

-- 4. Refresh backwards-compatible view in tracker schema
CREATE OR REPLACE VIEW tracker.user_applications AS SELECT * FROM auth.user_applications;
GRANT SELECT, INSERT, UPDATE, DELETE ON tracker.user_applications TO ${db_app_user};
