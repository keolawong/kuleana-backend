-- 1. The Pages Table (Defines frontend UI routes)
CREATE TABLE tracker.pages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    path VARCHAR(255) UNIQUE NOT NULL,      -- e.g., '/inventory', '/deploy'
    name VARCHAR(100) NOT NULL,             -- e.g., 'Inventory', 'Deploy Gate'
    description TEXT,
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT timezone('UTC', now()),
    updated_by VARCHAR(100) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT timezone('UTC', now())
);

GRANT SELECT, INSERT, UPDATE, DELETE ON tracker.pages TO ${db_app_user};

-- 2. Links Pages to Roles (Defines what pages a role can access)
CREATE TABLE tracker.role_pages (
    role_id UUID REFERENCES tracker.roles(id) ON DELETE CASCADE,
    page_id UUID REFERENCES tracker.pages(id) ON DELETE CASCADE,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT timezone('UTC', now()),
    updated_by VARCHAR(100) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT timezone('UTC', now()),
    PRIMARY KEY (role_id, page_id)
);

GRANT SELECT, INSERT, UPDATE, DELETE ON tracker.role_pages TO ${db_app_user};

-- 3. Pre-populate the standard application routes
INSERT INTO tracker.pages (path, name, description, created_by, updated_by) VALUES 
    ('/inventory', 'Inventory', NULL, 'system', 'system'),
    ('/deploy', 'Deploy Asset', NULL, 'system', 'system'),
    ('/reconcile', 'Reconcile', NULL, 'system', 'system'),
    ('/person-stickers', 'Personnel Management', NULL, 'system', 'system'),
    ('/reports', 'Reports', NULL, 'system', 'system'),
    ('/admin', 'Admin', '', 'system', 'system'),
    ('/history', 'History', 'Asset History', 'system', 'system');
