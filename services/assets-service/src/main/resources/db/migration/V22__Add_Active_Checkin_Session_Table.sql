-- V22__Add_Active_Checkin_Session_Table.sql
-- Table and indexes for tracking active check-in sessions across gates and shifts

CREATE TABLE IF NOT EXISTS asset.active_checkin_session (
    session_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    gate_id UUID NOT NULL REFERENCES asset.gate(gate_id),
    shift_id UUID NOT NULL REFERENCES asset.shift(shift_id),
    username VARCHAR(100) NOT NULL,
    user_display_name VARCHAR(150),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    last_heartbeat_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_active_checkin_session_status_heartbeat
    ON asset.active_checkin_session(status, last_heartbeat_at);

CREATE INDEX IF NOT EXISTS idx_active_checkin_session_username
    ON asset.active_checkin_session(username);

CREATE INDEX IF NOT EXISTS idx_active_checkin_session_gate_shift
    ON asset.active_checkin_session(gate_id, shift_id);

GRANT SELECT, INSERT, UPDATE, DELETE ON asset.active_checkin_session TO ${db_app_user};
