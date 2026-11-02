CREATE TABLE IF NOT EXISTS auth.organizations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    slug VARCHAR(64) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    logo_url VARCHAR(512),
    status VARCHAR(32) NOT NULL DEFAULT 'active',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('UTC', now()),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('UTC', now())
);

CREATE TABLE IF NOT EXISTS auth.user_organizations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    org_id UUID NOT NULL REFERENCES auth.organizations(id) ON DELETE CASCADE,
    role VARCHAR(32) NOT NULL DEFAULT 'MEMBER',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('UTC', now()),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('UTC', now()),
    CONSTRAINT uq_user_org UNIQUE (user_id, org_id)
);

-- Seed initial default organization
INSERT INTO auth.organizations (id, slug, name, description, status)
VALUES ('a0000000-0000-0000-0000-000000000001', 'default-org', 'Default Organization', 'Default system organization', 'active')
ON CONFLICT (slug) DO NOTHING;

-- Link all existing users to default-org
INSERT INTO auth.user_organizations (user_id, org_id, role)
SELECT id, 'a0000000-0000-0000-0000-000000000001'::uuid,
       CASE WHEN LOWER(username) = 'keolawong' THEN 'ORG_ADMIN' ELSE 'MEMBER' END
FROM auth.users
ON CONFLICT (user_id, org_id) DO NOTHING;

-- Seed second organization for multi-org testing
INSERT INTO auth.organizations (id, slug, name, description, status)
VALUES ('b0000000-0000-0000-0000-000000000002', 'balloon-fiesta', 'Albuquerque International Balloon Fiesta', 'Albuquerque International Balloon Fiesta organization', 'active')
ON CONFLICT (slug) DO NOTHING;

-- Link keolawong and SBabicz to balloon-fiesta as ORG_ADMIN
INSERT INTO auth.user_organizations (user_id, org_id, role)
SELECT id, 'b0000000-0000-0000-0000-000000000002'::uuid, 'ORG_ADMIN'
FROM auth.users
WHERE LOWER(username) IN ('keolawong', 'sbabicz')
ON CONFLICT (user_id, org_id) DO NOTHING;

-- Grants
GRANT SELECT, INSERT, UPDATE, DELETE ON auth.organizations TO ${db_app_user};
GRANT SELECT, INSERT, UPDATE, DELETE ON auth.user_organizations TO ${db_app_user};
