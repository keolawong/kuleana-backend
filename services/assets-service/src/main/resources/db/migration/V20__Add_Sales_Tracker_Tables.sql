-- V20__Add_Sales_Tracker_Tables.sql
-- Adds persistent sales tracking and worker headcount tables for cross-device synchronization

-- 1. Timestamped shift sales checkpoints
CREATE TABLE IF NOT EXISTS asset.shift_sales (
    shift_sales_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    shift_id UUID NOT NULL REFERENCES asset.shift(shift_id) ON DELETE CASCADE,
    recorded_at TIMESTAMP WITH TIME ZONE NOT NULL,
    total_sales NUMERIC(12, 2) NOT NULL,
    labor_cost NUMERIC(12, 2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_shift_sales_shift_time ON asset.shift_sales(shift_id, recorded_at ASC);

-- 2. Granular window sales recorded in each checkpoint
CREATE TABLE IF NOT EXISTS asset.window_sales (
    window_sales_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    shift_sales_id UUID NOT NULL REFERENCES asset.shift_sales(shift_sales_id) ON DELETE CASCADE,
    window_name VARCHAR(60) NOT NULL,
    sales_amount NUMERIC(12, 2) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_window_sales_parent ON asset.window_sales(shift_sales_id);

-- 3. Clocked-in worker types and pay rates for the shift
CREATE TABLE IF NOT EXISTS asset.shift_worker (
    shift_worker_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    shift_id UUID NOT NULL REFERENCES asset.shift(shift_id) ON DELETE CASCADE,
    role_name VARCHAR(50) NOT NULL,
    hourly_rate NUMERIC(8, 2) NOT NULL,
    count INT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_shift_worker_shift ON asset.shift_worker(shift_id);
