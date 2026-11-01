-- Performance indexes for tracker tables
CREATE INDEX IF NOT EXISTS idx_flow_tracking_external_id ON tracker.flow_tracking (external_id);
CREATE INDEX IF NOT EXISTS idx_flow_tracking_parent_id ON tracker.flow_tracking (parent_id);
CREATE INDEX IF NOT EXISTS idx_flow_tracking_session_gate ON tracker.flow_tracking (session_id, gate_id);
CREATE INDEX IF NOT EXISTS idx_flow_tracking_flow_status ON tracker.flow_tracking (flow_id, status);
CREATE INDEX IF NOT EXISTS idx_flow_step_tracking_flow_tracking_id ON tracker.flow_step_tracking (flow_tracking_id);
CREATE INDEX IF NOT EXISTS idx_tracking_landing_zone_status_created ON tracker.tracking_landing_zone (status, created_at);
