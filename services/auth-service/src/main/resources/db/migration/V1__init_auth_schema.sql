-- Initialize auth schema
CREATE SCHEMA IF NOT EXISTS auth;
GRANT USAGE ON SCHEMA auth TO ${db_app_user};

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- 1. Users Table
CREATE TABLE auth.users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(100) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    is_active BOOLEAN NOT NULL DEFAULT true,
    is_locked BOOLEAN NOT NULL DEFAULT false,
    failed_login_attempts INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT timezone('UTC', now()),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT timezone('UTC', now())
);

GRANT SELECT, INSERT, UPDATE, DELETE ON auth.users TO ${db_app_user};

-- 2. Roles Table
CREATE TABLE auth.roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(50) UNIQUE NOT NULL,
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT timezone('UTC', now())
);

GRANT SELECT, INSERT, UPDATE, DELETE ON auth.roles TO ${db_app_user};

-- 3. User Roles Join Table
CREATE TABLE auth.user_roles (
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE,
    role_id UUID REFERENCES auth.roles(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL DEFAULT 'active',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT timezone('UTC', now()),
    PRIMARY KEY (user_id, role_id)
);

GRANT SELECT, INSERT, UPDATE, DELETE ON auth.user_roles TO ${db_app_user};

-- 4. User Pages Table (Route-level permissions)
CREATE TABLE auth.user_pages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE,
    page_path VARCHAR(255) NOT NULL,
    can_read BOOLEAN NOT NULL DEFAULT true,
    can_write BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT timezone('UTC', now()),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT timezone('UTC', now()),
    CONSTRAINT uq_user_page UNIQUE (user_id, page_path)
);

GRANT SELECT, INSERT, UPDATE, DELETE ON auth.user_pages TO ${db_app_user};

-- 5. Tokens Table (Revocation / Refresh / Audit)
CREATE TABLE auth.tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    token VARCHAR(512) UNIQUE NOT NULL,
    token_type VARCHAR(50) NOT NULL DEFAULT 'BEARER',
    username VARCHAR(100) NOT NULL,
    is_revoked BOOLEAN NOT NULL DEFAULT false,
    is_expired BOOLEAN NOT NULL DEFAULT false,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT timezone('UTC', now())
);

GRANT SELECT, INSERT, UPDATE, DELETE ON auth.tokens TO ${db_app_user};

-- Indexes for high performance
CREATE INDEX idx_users_username ON auth.users (username);
CREATE INDEX idx_users_email ON auth.users (email);
CREATE INDEX idx_tokens_token ON auth.tokens (token);
CREATE INDEX idx_tokens_username ON auth.tokens (username);
CREATE INDEX idx_user_pages_user_id ON auth.user_pages (user_id);

-- Pre-populate default core roles
INSERT INTO auth.roles (name, description) VALUES
    ('ROLE_ADMIN', 'Administrator with full system privileges'),
    ('ROLE_USER', 'Standard authenticated user'),
    ('ROLE_AUDITOR', 'Read-only access for compliance audit')
ON CONFLICT (name) DO NOTHING;
