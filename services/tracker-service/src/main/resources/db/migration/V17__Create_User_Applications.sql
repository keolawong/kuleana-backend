-- V17: Create User Applications in auth schema
CREATE TABLE IF NOT EXISTS auth.user_applications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    app_name VARCHAR(50) NOT NULL, -- 'assets', 'tracker', 'admin', 'tournaments'
    status VARCHAR(20) NOT NULL DEFAULT 'active', -- 'active', 'inactive'
    primary_role VARCHAR(50), -- e.g. 'OPERATOR', 'LEAD', 'IT', 'ADMIN', 'VIEWER', 'EDITOR', 'OWNER'
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('UTC', now()),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('UTC', now()),
    CONSTRAINT uq_user_app UNIQUE (user_id, app_name)
);

GRANT SELECT, INSERT, UPDATE, DELETE ON auth.user_applications TO ${db_app_user};

-- Backwards-compatible view in tracker schema
CREATE OR REPLACE VIEW tracker.user_applications AS SELECT * FROM auth.user_applications;
GRANT SELECT, INSERT, UPDATE, DELETE ON tracker.user_applications TO ${db_app_user};
