-- Clean up legacy Flex / Lead rows from shift gate requirements and preset quotas
DELETE FROM asset.shift_gate_requirement 
WHERE job_role ILIKE '%flex%' OR job_role ILIKE '%lead%';

DELETE FROM asset.gate_preset_quota 
WHERE job_role ILIKE '%flex%' OR job_role ILIKE '%lead%';
