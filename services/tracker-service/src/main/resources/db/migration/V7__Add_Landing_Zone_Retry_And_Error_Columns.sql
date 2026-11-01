ALTER TABLE tracker.tracking_landing_zone ADD COLUMN retry_count INT NOT NULL DEFAULT 0;
ALTER TABLE tracker.tracking_landing_zone ADD COLUMN error_message TEXT;
