INSERT INTO asset.operation (name, is_active, start_date, end_date, created_by) VALUES
    ('2026 Albuquerque International Balloon Fiesta', TRUE, '2026-10-03', '2026-10-11', 'System'),
    ('2026 Albuquerque International Balloon Fiesta New Mexico Day', TRUE, '2026-10-05', '2026-10-05', 'System');

update asset.operation set is_active = false, updated_at = timezone('UTC', now()), updated_by = 'System' where start_date between to_date('2025-01-01', 'yyyy-mm-dd') and to_date('2025-12-31', 'yyyy-mm-dd');

INSERT INTO asset.shift (operation_id, name, start_time, end_time, traffic, created_by) VALUES
    ((SELECT operation_id FROM asset.operation WHERE name = '2026 Albuquerque International Balloon Fiesta'), '1', '2026-10-03 02:00:00', '2026-10-03 10:00:00', 'busy', 'System'),
    ((SELECT operation_id FROM asset.operation WHERE name = '2026 Albuquerque International Balloon Fiesta'), '2', '2026-10-03 14:00:00', '2026-10-03 23:00:00', 'busy', 'System'),
    ((SELECT operation_id FROM asset.operation WHERE name = '2026 Albuquerque International Balloon Fiesta'), '3', '2026-10-04 02:00:00', '2026-10-04 10:00:00', 'busy', 'System'),
    ((SELECT operation_id FROM asset.operation WHERE name = '2026 Albuquerque International Balloon Fiesta'), '4', '2026-10-04 14:00:00', '2026-10-04 23:00:00', 'moderate', 'System'),
    ((SELECT operation_id FROM asset.operation WHERE name = '2026 Albuquerque International Balloon Fiesta'), '5', '2026-10-05 02:00:00', '2026-10-05 10:00:00', 'lite', 'System'),
    ((SELECT operation_id FROM asset.operation WHERE name = '2026 Albuquerque International Balloon Fiesta'), '6', '2026-10-06 02:00:00', '2026-10-06 10:00:00', 'lite', 'System'),
    ((SELECT operation_id FROM asset.operation WHERE name = '2026 Albuquerque International Balloon Fiesta'), '7', '2026-10-07 02:00:00', '2026-10-07 10:00:00', 'lite', 'System'),
    ((SELECT operation_id FROM asset.operation WHERE name = '2026 Albuquerque International Balloon Fiesta'), '8', '2026-10-08 02:00:00', '2026-10-08 10:00:00', 'moderate', 'System'),
    ((SELECT operation_id FROM asset.operation WHERE name = '2026 Albuquerque International Balloon Fiesta'), '9', '2026-10-08 14:00:00', '2026-10-08 23:00:00', 'busy', 'System'),
    ((SELECT operation_id FROM asset.operation WHERE name = '2026 Albuquerque International Balloon Fiesta'), '10', '2026-10-09 02:00:00', '2026-10-09 10:00:00', 'moderate', 'System'),
    ((SELECT operation_id FROM asset.operation WHERE name = '2026 Albuquerque International Balloon Fiesta'), '11', '2026-10-09 14:00:00', '2026-10-09 23:00:00', 'busy', 'System'),
    ((SELECT operation_id FROM asset.operation WHERE name = '2026 Albuquerque International Balloon Fiesta'), '12', '2026-10-10 02:00:00', '2026-10-10 10:00:00', 'busy', 'System'),
    ((SELECT operation_id FROM asset.operation WHERE name = '2026 Albuquerque International Balloon Fiesta'), '13', '2026-10-10 14:00:00', '2026-10-10 23:00:00', 'busy', 'System'),
    ((SELECT operation_id FROM asset.operation WHERE name = '2026 Albuquerque International Balloon Fiesta'), '14', '2026-10-11 02:00:00', '2026-10-11 10:00:00', 'moderate', 'System'),
    ((SELECT operation_id FROM asset.operation WHERE name = '2026 Albuquerque International Balloon Fiesta New Mexico Day'), 'NM', '2026-10-05 02:00:00', '2026-10-05 10:00:00', 'moderate', 'System');