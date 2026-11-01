CREATE TABLE tracker.tracking_landing_zone (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payload JSONB NOT NULL,
    original_user VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    claimed_by VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT timezone('UTC', now()),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT timezone('UTC', now())
);

CREATE INDEX idx_tracking_landing_zone_status ON tracker.tracking_landing_zone(status);

GRANT SELECT, INSERT, UPDATE, DELETE ON tracker.tracking_landing_zone TO ${db_app_user};
