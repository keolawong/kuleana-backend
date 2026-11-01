-- Insert seller tracking page route if it doesn't already exist
INSERT INTO tracker.pages (path, name, description, created_by, updated_by) VALUES 
    ('/seller-tracking', 'Seller Tracking', 'Track seller accuracy across sales sessions, monitor over/under variances, and view session stubs.', 'system', 'system')
ON CONFLICT (path) DO NOTHING;
