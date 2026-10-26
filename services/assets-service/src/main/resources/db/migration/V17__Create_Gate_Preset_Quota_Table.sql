-- Create Gate Preset Quota Table
CREATE TABLE asset.gate_preset_quota (
    preset_quota_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    preset_type VARCHAR(20) NOT NULL, -- 'BUSY', 'MODERATE', 'LITE'
    gate_id UUID NOT NULL REFERENCES asset.gate(gate_id) ON DELETE CASCADE,
    job_role VARCHAR(50) NOT NULL, -- 'Ticket Seller', 'Ticket Scanner'
    planned_workers INT NOT NULL DEFAULT 0,
    min_workers INT NOT NULL DEFAULT 0,
    happy_workers INT NOT NULL DEFAULT 0,
    priority INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP DEFAULT timezone('UTC', now()),
    updated_at TIMESTAMP DEFAULT timezone('UTC', now()),
    CONSTRAINT uq_preset_gate_role UNIQUE (preset_type, gate_id, job_role)
);

CREATE INDEX idx_preset_quota ON asset.gate_preset_quota(preset_type, gate_id);
GRANT SELECT, INSERT, UPDATE, DELETE ON asset.gate_preset_quota TO ${db_app_user};

-- Seed baseline templates for active gates
-- Busy Template:
-- Gate 32: planned 10, min 2, happy 6, priority 1
-- Gate 20: planned 10, min 2, happy 6, priority 1
-- Gate 12: planned 6, min 2, happy 4, priority 2
-- Gate 9:  planned 5, min 2, happy 3, priority 2
-- Gate 21: planned 5, min 2, happy 3, priority 2
-- Gate 30: planned 2, min 1, happy 2, priority 3
INSERT INTO asset.gate_preset_quota (preset_type, gate_id, job_role, planned_workers, min_workers, happy_workers, priority)
SELECT 'BUSY', g.gate_id, r.role, p.planned, p.min_w, p.happy_w, p.prio
FROM (
    VALUES 
        ('32', 10, 2, 6, 1),
        ('20', 10, 2, 6, 1),
        ('12', 6, 2, 4, 2),
        ('9',  5, 2, 3, 2),
        ('21', 5, 2, 3, 2),
        ('30', 2, 1, 2, 3)
) AS p(gate_name, planned, min_w, happy_w, prio)
JOIN asset.gate g ON g.name = p.gate_name
CROSS JOIN (VALUES ('Ticket Seller'), ('Ticket Scanner')) AS r(role)
ON CONFLICT (preset_type, gate_id, job_role) DO NOTHING;

-- Moderate Template:
-- Gate 32: planned 6, min 2, happy 4, priority 1
-- Gate 20: planned 6, min 2, happy 4, priority 1
-- Gate 12: planned 4, min 2, happy 3, priority 2
-- Gate 9:  planned 3, min 2, happy 2, priority 2
-- Gate 21: planned 3, min 2, happy 2, priority 2
-- Gate 30: planned 2, min 1, happy 2, priority 3
INSERT INTO asset.gate_preset_quota (preset_type, gate_id, job_role, planned_workers, min_workers, happy_workers, priority)
SELECT 'MODERATE', g.gate_id, r.role, p.planned, p.min_w, p.happy_w, p.prio
FROM (
    VALUES 
        ('32', 6, 2, 4, 1),
        ('20', 6, 2, 4, 1),
        ('12', 4, 2, 3, 2),
        ('9',  3, 2, 2, 2),
        ('21', 3, 2, 2, 2),
        ('30', 2, 1, 2, 3)
) AS p(gate_name, planned, min_w, happy_w, prio)
JOIN asset.gate g ON g.name = p.gate_name
CROSS JOIN (VALUES ('Ticket Seller'), ('Ticket Scanner')) AS r(role)
ON CONFLICT (preset_type, gate_id, job_role) DO NOTHING;

-- Lite Template:
-- Gate 32: planned 4, min 2, happy 3, priority 1
-- Gate 20: planned 4, min 2, happy 3, priority 1
-- Gate 12: planned 2, min 1, happy 2, priority 2
-- Gate 9:  planned 2, min 1, happy 2, priority 2
-- Gate 21: planned 2, min 1, happy 2, priority 2
-- Gate 30: planned 1, min 1, happy 1, priority 3
INSERT INTO asset.gate_preset_quota (preset_type, gate_id, job_role, planned_workers, min_workers, happy_workers, priority)
SELECT 'LITE', g.gate_id, r.role, p.planned, p.min_w, p.happy_w, p.prio
FROM (
    VALUES 
        ('32', 4, 2, 3, 1),
        ('20', 4, 2, 3, 1),
        ('12', 2, 1, 2, 2),
        ('9',  2, 1, 2, 2),
        ('21', 2, 1, 2, 2),
        ('30', 1, 1, 1, 3)
) AS p(gate_name, planned, min_w, happy_w, prio)
JOIN asset.gate g ON g.name = p.gate_name
CROSS JOIN (VALUES ('Ticket Seller'), ('Ticket Scanner')) AS r(role)
ON CONFLICT (preset_type, gate_id, job_role) DO NOTHING;
