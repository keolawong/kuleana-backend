-- Insert standard application routes if they don't already exist
INSERT INTO tracker.pages (path, name, description, created_by, updated_by) VALUES 
    ('/gate-assignments', 'Gate Assignments', 'Manage session worker rosters, quotas, and smart gate assignments.', 'system', 'system'),
    ('/sales-tracker', 'Sales Tracker', 'Track real-time sales pacing and labor costs.', 'system', 'system'),
    ('/landing-zone', 'System Activity', 'Monitor background activity and retry failed actions.', 'system', 'system')
ON CONFLICT (path) DO NOTHING;
