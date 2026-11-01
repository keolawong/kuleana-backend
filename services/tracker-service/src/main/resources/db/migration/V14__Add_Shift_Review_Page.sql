-- Insert /shift-review route into tracker.pages for dynamic role-based access control
INSERT INTO tracker.pages (path, name, description, created_by, updated_by) VALUES 
    ('/shift-review', 'Shift Review Panel', 'Read-only audit panel for reviewing check-in and check-out status of assets in a shift.', 'system', 'system')
ON CONFLICT (path) DO NOTHING;
