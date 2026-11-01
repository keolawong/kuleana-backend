-- Flow step for 'Asset-Action': EDIT
INSERT INTO tracker.flow_step (flow_id, step_name, description, step_seq_num, created_by, updated_by)
SELECT f.id, 'EDIT', 'Asset details modified in inventory pool', 5, 'system', 'system'
FROM tracker.flow f
JOIN tracker.application a ON f.application_id = a.id
WHERE a.name = 'Kahua Asset' AND f.flow_name = 'Asset-Action'
  AND NOT EXISTS (
      SELECT 1 FROM tracker.flow_step fs WHERE fs.flow_id = f.id AND fs.step_name = 'EDIT'
  );
