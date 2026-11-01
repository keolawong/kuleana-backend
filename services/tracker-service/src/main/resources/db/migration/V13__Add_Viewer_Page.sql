-- Insert /viewer route into tracker.pages for dynamic role-based access control
INSERT INTO tracker.pages (path, name, description, created_by, updated_by) VALUES 
    ('/viewer', 'Gate Viewer Panel', 'Real-time read-only monitoring dashboard of check-in and check-out status for Gate Captains.', 'system', 'system')
ON CONFLICT (path) DO NOTHING;
