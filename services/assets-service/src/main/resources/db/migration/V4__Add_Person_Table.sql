CREATE TABLE asset.person (
    person_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    notes VARCHAR(255),
    external_id VARCHAR(100) NULL,
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT timezone('UTC', now()),
    updated_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT timezone('UTC', now())
);
GRANT SELECT, INSERT, UPDATE, DELETE ON asset.person TO ${db_app_user};

CREATE TABLE asset.person_sticker (
    person_sticker_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    person_id UUID,
    shift_id UUID,
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT timezone('UTC', now()),
    updated_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT timezone('UTC', now())
);
GRANT SELECT, INSERT, UPDATE, DELETE ON asset.person_sticker TO ${db_app_user};