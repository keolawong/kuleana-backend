CREATE SCHEMA IF NOT EXISTS tracker;
GRANT USAGE ON SCHEMA tracker TO ${db_app_user};

CREATE TABLE tracker.users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT  timezone('UTC', now()),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT  timezone('UTC', now())
);

CREATE EXTENSION IF NOT EXISTS pgcrypto;
INSERT INTO tracker.users (username, password_hash)
VALUES ('keolawong', crypt('password', gen_salt('bf', 4)));

GRANT SELECT, INSERT, UPDATE, DELETE ON tracker.users TO ${db_app_user};

-- 1. The Roles Table (Your AD Groups)
CREATE TABLE tracker.roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(50) UNIQUE NOT NULL,       -- e.g., 'admin', 'flow_manager', 'auditor'
    is_active BOOLEAN NOT NULL DEFAULT true,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('UTC', now())
);

INSERT INTO tracker.roles (name, description) VALUES
    ('admin', 'Administrator role'),
    ('owner', 'Application owner, can create applications'),
    ('editor', 'Can edit content'),
    ('viewer', 'Read-only access');

GRANT SELECT, INSERT, UPDATE, DELETE ON tracker.roles TO ${db_app_user};

-- 2. The Permissions Table (The fine-grained actions)
CREATE TABLE tracker.permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) UNIQUE NOT NULL,     -- e.g., 'flow:create', 'flow:update', 'app:view'
    description TEXT
);

GRANT SELECT, INSERT, UPDATE, DELETE ON tracker.permissions TO ${db_app_user};

-- 3. Links Permissions to Roles (Define what a role can do)
CREATE TABLE role_permissions (
    role_id UUID REFERENCES roles(id) ON DELETE CASCADE,
    permission_id UUID REFERENCES permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

GRANT SELECT, INSERT, UPDATE, DELETE ON tracker.role_permissions TO ${db_app_user};

-- 4. Links Users to Roles (Assigns one or more roles to a user)
CREATE TABLE tracker.user_roles (
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    role_id UUID REFERENCES roles(id) ON DELETE CASCADE,
    status VARCHAR(10) NOT NULL,
    PRIMARY KEY (user_id, role_id)
);

INSERT INTO tracker.user_roles (user_id, role_id, status)
SELECT u.id, r.id, 'active'
FROM tracker.users u, tracker.roles r
WHERE u.username = 'keolawong' AND r.name IN ('admin', 'owner', 'editor', 'viewer');

GRANT SELECT, INSERT, UPDATE, DELETE ON tracker.user_roles TO ${db_app_user};

CREATE TABLE tracker.application (
     id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
     name        VARCHAR(255) NOT NULL,
     write_role_id UUID REFERENCES tracker.roles(id),
     read_role_id UUID REFERENCES tracker.roles(id),
     is_active   BOOLEAN NOT NULL DEFAULT true,
     created_by  VARCHAR(100) NOT NULL,
     created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT timezone('UTC', now()),
     updated_by  VARCHAR(100) NOT NULL,
     updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT timezone('UTC', now())
);

-- Ensure your runtime API account can interact with the new table
GRANT SELECT, INSERT, UPDATE, DELETE ON tracker.application TO ${db_app_user};

CREATE TABLE tracker.flow (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    flow_name       VARCHAR(255) NOT NULL,
    application_id  UUID REFERENCES tracker.application(id),
    description     TEXT,
    is_active       BOOLEAN NOT NULL DEFAULT true,
    created_by      VARCHAR(100) NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT timezone('UTC', now()),
    updated_by      VARCHAR(100) NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT timezone('UTC', now()),

-- Foreign key relationship linking back to execution_flow
    CONSTRAINT fk_flow_application FOREIGN KEY (application_id)
        REFERENCES tracker.application (id) ON DELETE CASCADE
);

-- Grant permissions to your runtime API account
GRANT SELECT, INSERT, UPDATE, DELETE ON tracker.flow TO ${db_app_user};

CREATE TABLE tracker.flow_step (
   id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
   flow_id         UUID NOT NULL,
   step_name       VARCHAR(255) NOT NULL,
   description     TEXT,
   step_seq_num    INT NOT NULL,
   created_by      VARCHAR(100) NOT NULL,
   created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT timezone('UTC', now()),
   updated_by      VARCHAR(100) NOT NULL,
   updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT timezone('UTC', now()),

-- Foreign key relationship linking back to execution_flow
   CONSTRAINT fk_flow_step_flow FOREIGN KEY (flow_id)
       REFERENCES tracker.flow (id) ON DELETE CASCADE
);

-- Grant permissions to your runtime API account
GRANT SELECT, INSERT, UPDATE, DELETE ON tracker.flow_step TO ${db_app_user};

CREATE TABLE tracker.flow_tracking (
   id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
   flow_id         UUID NOT NULL,
   external_id     UUID,
   status          VARCHAR(50),
   type            VARCHAR(50),
   session_id      UUID,
   gate_id         UUID,
   created_by      VARCHAR(100) NOT NULL,
   created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT timezone('UTC', now()),
   updated_by      VARCHAR(100) NOT NULL,
   updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT timezone('UTC', now()),

-- Foreign key to link back to the parent execution_flow directly
   CONSTRAINT fk_flow_tracking_flow FOREIGN KEY (flow_id)
       REFERENCES tracker.flow (id) ON DELETE CASCADE
);

-- Grant permissions to your runtime API account
GRANT SELECT, INSERT, UPDATE, DELETE ON tracker.flow_tracking TO ${db_app_user};

CREATE TABLE tracker.flow_step_tracking (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    flow_tracking_id UUID NOT NULL,
    flow_step_id    UUID NOT NULL,
    status          VARCHAR(50),
    message         TEXT,
    amount          NUMERIC(20, 4),
    count           INT,
    created_by      VARCHAR(100) NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT timezone('UTC', now()),

-- Foreign key linking back to flow_tracking
    CONSTRAINT fk_flow_step_tracking_flow_tracking FOREIGN KEY (flow_tracking_id)
        REFERENCES tracker.flow_tracking (id) ON DELETE CASCADE,
    CONSTRAINT fk_flow_step_tracking_flow_step FOREIGN KEY (flow_step_id)
        REFERENCES tracker.flow_step (id) ON DELETE CASCADE
);

-- Grant permissions to your runtime API account
GRANT SELECT, INSERT, UPDATE, DELETE ON tracker.flow_step_tracking TO ${db_app_user};