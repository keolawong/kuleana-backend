-- 1. Add self-referencing parent_id column to flow_tracking
ALTER TABLE tracker.flow_tracking 
ADD COLUMN parent_id UUID REFERENCES tracker.flow_tracking(id) ON DELETE CASCADE;

CREATE INDEX idx_flow_tracking_parent_id ON tracker.flow_tracking(parent_id);

-- 2. Ensure standard application 'Kahua Asset' exists
INSERT INTO tracker.application (name, created_by, updated_by)
SELECT 'Kahua Asset', 'system', 'system'
WHERE NOT EXISTS (
    SELECT 1 FROM tracker.application WHERE name = 'Kahua Asset'
);

-- 3. Ensure Parent Lifecycle Flow 'Asset-Lifecycle' exists
INSERT INTO tracker.flow (flow_name, application_id, description, is_active, created_by, updated_by)
SELECT 'Asset-Lifecycle', a.id, 'Asset Parent Lifecycle Process Flow', true, 'system', 'system'
FROM tracker.application a
WHERE a.name = 'Kahua Asset'
  AND NOT EXISTS (
      SELECT 1 FROM tracker.flow f WHERE f.flow_name = 'Asset-Lifecycle' AND f.application_id = a.id
  );

-- Flow steps for 'Asset-Lifecycle': ACTIVE, RECONCILED, INACTIVE
INSERT INTO tracker.flow_step (flow_id, step_name, description, step_seq_num, created_by, updated_by)
SELECT f.id, 'ACTIVE', 'Asset is active in circulation', 1, 'system', 'system'
FROM tracker.flow f
JOIN tracker.application a ON f.application_id = a.id
WHERE a.name = 'Kahua Asset' AND f.flow_name = 'Asset-Lifecycle'
  AND NOT EXISTS (
      SELECT 1 FROM tracker.flow_step fs WHERE fs.flow_id = f.id AND fs.step_name = 'ACTIVE'
  );

INSERT INTO tracker.flow_step (flow_id, step_name, description, step_seq_num, created_by, updated_by)
SELECT f.id, 'RECONCILED', 'Asset is completely used, returned, and reconciled', 2, 'system', 'system'
FROM tracker.flow f
JOIN tracker.application a ON f.application_id = a.id
WHERE a.name = 'Kahua Asset' AND f.flow_name = 'Asset-Lifecycle'
  AND NOT EXISTS (
      SELECT 1 FROM tracker.flow_step fs WHERE fs.flow_id = f.id AND fs.step_name = 'RECONCILED'
  );

INSERT INTO tracker.flow_step (flow_id, step_name, description, step_seq_num, created_by, updated_by)
SELECT f.id, 'INACTIVE', 'Asset is deactivated or soft-deleted', 3, 'system', 'system'
FROM tracker.flow f
JOIN tracker.application a ON f.application_id = a.id
WHERE a.name = 'Kahua Asset' AND f.flow_name = 'Asset-Lifecycle'
  AND NOT EXISTS (
      SELECT 1 FROM tracker.flow_step fs WHERE fs.flow_id = f.id AND fs.step_name = 'INACTIVE'
  );

-- 4. Ensure Child Action Flow 'Asset-Action' exists
INSERT INTO tracker.flow (flow_name, application_id, description, is_active, created_by, updated_by)
SELECT 'Asset-Action', a.id, 'Asset Child Action Process Flow', true, 'system', 'system'
FROM tracker.application a
WHERE a.name = 'Kahua Asset'
  AND NOT EXISTS (
      SELECT 1 FROM tracker.flow f WHERE f.flow_name = 'Asset-Action' AND f.application_id = a.id
  );

-- Flow steps for 'Asset-Action': CREATION, ASSIGNMENT, RETURN, DAMAGED
INSERT INTO tracker.flow_step (flow_id, step_name, description, step_seq_num, created_by, updated_by)
SELECT f.id, 'CREATION', 'Asset creation or bulk split generation', 1, 'system', 'system'
FROM tracker.flow f
JOIN tracker.application a ON f.application_id = a.id
WHERE a.name = 'Kahua Asset' AND f.flow_name = 'Asset-Action'
  AND NOT EXISTS (
      SELECT 1 FROM tracker.flow_step fs WHERE fs.flow_id = f.id AND fs.step_name = 'CREATION'
  );

INSERT INTO tracker.flow_step (flow_id, step_name, description, step_seq_num, created_by, updated_by)
SELECT f.id, 'ASSIGNMENT', 'Deployment to gates, seller assignment, or transfer', 2, 'system', 'system'
FROM tracker.flow f
JOIN tracker.application a ON f.application_id = a.id
WHERE a.name = 'Kahua Asset' AND f.flow_name = 'Asset-Action'
  AND NOT EXISTS (
      SELECT 1 FROM tracker.flow_step fs WHERE fs.flow_id = f.id AND fs.step_name = 'ASSIGNMENT'
  );

INSERT INTO tracker.flow_step (flow_id, step_name, description, step_seq_num, created_by, updated_by)
SELECT f.id, 'RETURN', 'Asset return, unused return, or reconciliation check-in', 3, 'system', 'system'
FROM tracker.flow f
JOIN tracker.application a ON f.application_id = a.id
WHERE a.name = 'Kahua Asset' AND f.flow_name = 'Asset-Action'
  AND NOT EXISTS (
      SELECT 1 FROM tracker.flow_step fs WHERE fs.flow_id = f.id AND fs.step_name = 'RETURN'
  );

INSERT INTO tracker.flow_step (flow_id, step_name, description, step_seq_num, created_by, updated_by)
SELECT f.id, 'DAMAGED', 'Asset damaged or decommissioned during operation', 4, 'system', 'system'
FROM tracker.flow f
JOIN tracker.application a ON f.application_id = a.id
WHERE a.name = 'Kahua Asset' AND f.flow_name = 'Asset-Action'
  AND NOT EXISTS (
      SELECT 1 FROM tracker.flow_step fs WHERE fs.flow_id = f.id AND fs.step_name = 'DAMAGED'
  );
