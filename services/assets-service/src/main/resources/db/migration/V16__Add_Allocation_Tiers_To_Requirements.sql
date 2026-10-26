-- Add Multi-Tier Allocation and Priority Columns to shift_gate_requirement
ALTER TABLE asset.shift_gate_requirement
    ADD COLUMN IF NOT EXISTS min_workers INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS happy_workers INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS priority INT NOT NULL DEFAULT 1;

-- Backfill existing requirements intelligently based on existing workers_needed
UPDATE asset.shift_gate_requirement
SET min_workers = CASE
        WHEN workers_needed >= 2 THEN 2
        WHEN workers_needed = 1 THEN 1
        ELSE 0
    END,
    happy_workers = CASE
        WHEN workers_needed >= 4 THEN CEIL(workers_needed * 0.7)::INT
        ELSE workers_needed
    END,
    priority = CASE
        -- Give primary ingress gates priority 1, others priority 2 by default
        WHEN gate_id IN (SELECT gate_id FROM asset.gate WHERE name IN ('32', '20', '12')) THEN 1
        ELSE 2
    END
WHERE min_workers = 0 AND happy_workers = 0;
