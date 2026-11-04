-- Add operation_id column to asset.gate and drop global name unique constraint before duplication
ALTER TABLE asset.gate ADD COLUMN IF NOT EXISTS operation_id UUID REFERENCES asset.operation(operation_id) ON DELETE CASCADE;
ALTER TABLE asset.gate DROP CONSTRAINT IF EXISTS uq_gate_name;

DO $$
DECLARE
    v_primary_op_id UUID;
    v_op RECORD;
BEGIN
    -- 1. Identify primary 2026 operation
    SELECT operation_id INTO v_primary_op_id
    FROM asset.operation
    WHERE name = '2026 Albuquerque International Balloon Fiesta'
       OR name = '2026'
       OR (name ILIKE '%2026%' AND is_active = true)
       OR name ILIKE '%2026%'
    ORDER BY 
       CASE 
         WHEN name = '2026' THEN 1
         WHEN name = '2026 Albuquerque International Balloon Fiesta' THEN 2
         WHEN is_active = true THEN 3
         ELSE 4
       END,
       created_at DESC
    LIMIT 1;

    IF v_primary_op_id IS NULL THEN
        SELECT operation_id INTO v_primary_op_id
        FROM asset.operation
        ORDER BY is_active DESC, created_at DESC
        LIMIT 1;
    END IF;

    -- 2. Assign all existing gates to the primary 2026 operation
    UPDATE asset.gate
    SET operation_id = v_primary_op_id
    WHERE operation_id IS NULL;

    -- 3. Duplicate full gate set for every other operation
    FOR v_op IN SELECT operation_id FROM asset.operation WHERE operation_id != v_primary_op_id LOOP
        INSERT INTO asset.gate (
            gate_id,
            operation_id,
            name,
            is_active,
            cash_banks_required_for_busy,
            cash_banks_required_for_moderate,
            cash_banks_required_for_lite,
            display_order,
            created_by
        )
        SELECT
            gen_random_uuid(),
            v_op.operation_id,
            g.name,
            g.is_active,
            g.cash_banks_required_for_busy,
            g.cash_banks_required_for_moderate,
            g.cash_banks_required_for_lite,
            g.display_order,
            'System'
        FROM asset.gate g
        WHERE g.operation_id = v_primary_op_id;
    END LOOP;

    -- 4. Re-bind asset assignments for non-2026 operations to their operation's new gates

    -- cash_bank (has direct operation_id)
    UPDATE asset.cash_bank cb
    SET gate_id = g_new.gate_id
    FROM asset.gate g_old,
         asset.gate g_new
    WHERE cb.gate_id = g_old.gate_id
      AND g_new.name = g_old.name
      AND g_old.operation_id = v_primary_op_id
      AND g_new.operation_id = cb.operation_id
      AND cb.operation_id != v_primary_op_id;

    -- ticket_book (has direct operation_id)
    UPDATE asset.ticket_book tb
    SET gate_id = g_new.gate_id
    FROM asset.gate g_old,
         asset.gate g_new
    WHERE tb.gate_id = g_old.gate_id
      AND g_new.name = g_old.name
      AND g_old.operation_id = v_primary_op_id
      AND g_new.operation_id = tb.operation_id
      AND tb.operation_id != v_primary_op_id;

    -- gate_captain (has direct operation_id)
    UPDATE asset.gate_captain gc
    SET gate_id = g_new.gate_id
    FROM asset.gate g_old,
         asset.gate g_new
    WHERE gc.gate_id = g_old.gate_id
      AND g_new.name = g_old.name
      AND g_old.operation_id = v_primary_op_id
      AND g_new.operation_id = gc.operation_id
      AND gc.operation_id != v_primary_op_id;

    -- shift_gate_requirement (via shift.operation_id)
    UPDATE asset.shift_gate_requirement sgr
    SET gate_id = g_new.gate_id
    FROM asset.shift s,
         asset.gate g_old,
         asset.gate g_new
    WHERE sgr.shift_id = s.shift_id
      AND sgr.gate_id = g_old.gate_id
      AND g_new.name = g_old.name
      AND g_new.operation_id = s.operation_id
      AND s.operation_id != v_primary_op_id
      AND g_old.operation_id = v_primary_op_id;

    -- person (via shift.operation_id)
    UPDATE asset.person p
    SET gate_id = g_new.gate_id
    FROM asset.shift s,
         asset.gate g_old,
         asset.gate g_new
    WHERE p.shift_id = s.shift_id
      AND p.gate_id = g_old.gate_id
      AND g_new.name = g_old.name
      AND g_new.operation_id = s.operation_id
      AND s.operation_id != v_primary_op_id
      AND g_old.operation_id = v_primary_op_id;

    -- org_gate_preference (via shift.operation_id)
    UPDATE asset.org_gate_preference ogp
    SET gate_id = g_new.gate_id
    FROM asset.shift s,
         asset.gate g_old,
         asset.gate g_new
    WHERE ogp.shift_id = s.shift_id
      AND ogp.gate_id = g_old.gate_id
      AND g_new.name = g_old.name
      AND g_new.operation_id = s.operation_id
      AND s.operation_id != v_primary_op_id
      AND g_old.operation_id = v_primary_op_id;

    -- credit_report (via shift.operation_id)
    UPDATE asset.credit_report cr
    SET gate_id = g_new.gate_id
    FROM asset.shift s,
         asset.gate g_old,
         asset.gate g_new
    WHERE cr.shift_id = s.shift_id
      AND cr.gate_id = g_old.gate_id
      AND g_new.name = g_old.name
      AND g_new.operation_id = s.operation_id
      AND s.operation_id != v_primary_op_id
      AND g_old.operation_id = v_primary_op_id;

    -- person_sticker (via shift.operation_id)
    UPDATE asset.person_sticker ps
    SET gate_id = g_new.gate_id
    FROM asset.shift s,
         asset.gate g_old,
         asset.gate g_new
    WHERE ps.shift_id = s.shift_id
      AND ps.gate_id = g_old.gate_id
      AND g_new.name = g_old.name
      AND g_new.operation_id = s.operation_id
      AND s.operation_id != v_primary_op_id
      AND g_old.operation_id = v_primary_op_id;

    -- active_checkin_session (via shift.operation_id)
    UPDATE asset.active_checkin_session acs
    SET gate_id = g_new.gate_id
    FROM asset.shift s,
         asset.gate g_old,
         asset.gate g_new
    WHERE acs.shift_id = s.shift_id
      AND acs.gate_id = g_old.gate_id
      AND g_new.name = g_old.name
      AND g_new.operation_id = s.operation_id
      AND s.operation_id != v_primary_op_id
      AND g_old.operation_id = v_primary_op_id;

    -- history tables (via shift.operation_id)
    UPDATE asset.cash_bank_history cbh
    SET gate_id = g_new.gate_id
    FROM asset.shift s,
         asset.gate g_old,
         asset.gate g_new
    WHERE cbh.shift_id = s.shift_id
      AND cbh.gate_id = g_old.gate_id
      AND g_new.name = g_old.name
      AND g_new.operation_id = s.operation_id
      AND s.operation_id != v_primary_op_id
      AND g_old.operation_id = v_primary_op_id;

    UPDATE asset.ticket_book_history tbh
    SET gate_id = g_new.gate_id
    FROM asset.shift s,
         asset.gate g_old,
         asset.gate g_new
    WHERE tbh.shift_id = s.shift_id
      AND tbh.gate_id = g_old.gate_id
      AND g_new.name = g_old.name
      AND g_new.operation_id = s.operation_id
      AND s.operation_id != v_primary_op_id
      AND g_old.operation_id = v_primary_op_id;

    UPDATE asset.credit_report_history crh
    SET gate_id = g_new.gate_id
    FROM asset.shift s,
         asset.gate g_old,
         asset.gate g_new
    WHERE crh.shift_id = s.shift_id
      AND crh.gate_id = g_old.gate_id
      AND g_new.name = g_old.name
      AND g_new.operation_id = s.operation_id
      AND s.operation_id != v_primary_op_id
      AND g_old.operation_id = v_primary_op_id;

    -- 5. Duplicate gate preset quotas for all new gates
    INSERT INTO asset.gate_preset_quota (preset_type, gate_id, job_role, planned_workers, min_workers, happy_workers, priority)
    SELECT 
        gpq.preset_type,
        g_new.gate_id,
        gpq.job_role,
        gpq.planned_workers,
        gpq.min_workers,
        gpq.happy_workers,
        gpq.priority
    FROM asset.gate_preset_quota gpq
    JOIN asset.gate g_old ON gpq.gate_id = g_old.gate_id
    JOIN asset.gate g_new ON g_new.name = g_old.name
    WHERE g_old.operation_id = v_primary_op_id
      AND g_new.operation_id != v_primary_op_id
    ON CONFLICT DO NOTHING;

END $$;

-- 6. Enforce NOT NULL and scoped unique constraint
ALTER TABLE asset.gate ALTER COLUMN operation_id SET NOT NULL;
ALTER TABLE asset.gate DROP CONSTRAINT IF EXISTS uq_gate_name;
ALTER TABLE asset.gate ADD CONSTRAINT uq_gate_operation_name UNIQUE (operation_id, name);

CREATE INDEX IF NOT EXISTS idx_gate_operation_id ON asset.gate(operation_id);

GRANT SELECT, INSERT, UPDATE, DELETE ON asset.gate TO ${db_app_user};
