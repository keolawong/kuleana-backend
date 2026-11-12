-- V3: Universal Registration Defaults and App Configurations

-- 1. App Registration Configurations
CREATE TABLE IF NOT EXISTS auth.app_registration_configs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    app_name VARCHAR(50) NOT NULL UNIQUE,
    display_title VARCHAR(100) NOT NULL,
    default_primary_role VARCHAR(50) NOT NULL DEFAULT 'VIEWER',
    default_roles TEXT[] NOT NULL DEFAULT ARRAY['viewer'],
    require_captcha BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('UTC', now()),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('UTC', now())
);

-- Seed default registration configs for known apps
INSERT INTO auth.app_registration_configs (app_name, display_title, default_primary_role, default_roles, require_captcha)
VALUES 
    ('assets', 'Kuleana Assets', 'OPERATOR', ARRAY['ASSET-READ', 'ASSET-WRITE'], true),
    ('tracker', 'Kuleana Tracker', 'VIEWER', ARRAY['viewer'], true),
    ('admin', 'Kuleana Admin', 'VIEWER', ARRAY['viewer'], true)
ON CONFLICT (app_name) DO NOTHING;

-- 2. Organizations Onboarding Defaults
ALTER TABLE auth.organizations
    ADD COLUMN IF NOT EXISTS default_application_roles JSONB DEFAULT '{"assets": ["ASSET-READ"], "tracker": ["viewer"]}'::jsonb,
    ADD COLUMN IF NOT EXISTS allow_public_registration BOOLEAN DEFAULT true,
    ADD COLUMN IF NOT EXISTS default_active_status VARCHAR(32) DEFAULT 'active';

-- Ensure unique slug index on lower(slug)
CREATE UNIQUE INDEX IF NOT EXISTS idx_organizations_slug_lower ON auth.organizations (LOWER(slug));

-- 3. Grants
GRANT SELECT, INSERT, UPDATE, DELETE ON auth.app_registration_configs TO ${db_app_user};
