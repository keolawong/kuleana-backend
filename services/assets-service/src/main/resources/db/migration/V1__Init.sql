CREATE SCHEMA IF NOT EXISTS asset;
GRANT USAGE ON SCHEMA asset TO ${db_app_user};

CREATE TABLE asset.operation (
    operation_id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    start_date DATE,
    end_date DATE,
    location_name VARCHAR(255),
    is_active BOOLEAN DEFAULT TRUE,
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT timezone('UTC', now()),
    updated_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT timezone('UTC', now())
);
GRANT SELECT, INSERT, UPDATE, DELETE ON asset.operation TO ${db_app_user};

CREATE TABLE asset.shift (
    shift_id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    operation_id UUID NOT NULL REFERENCES operation(operation_id),
    name VARCHAR(255) NOT NULL,
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    traffic VARCHAR(20),
    status VARCHAR(20),
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT timezone('UTC', now()),
    updated_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT timezone('UTC', now())
);
GRANT SELECT, INSERT, UPDATE, DELETE ON asset.shift TO ${db_app_user};

CREATE TABLE asset.gate (
    gate_id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    cash_banks_required_for_busy INTEGER,
    cash_banks_required_for_moderate INTEGER,
    cash_banks_required_for_lite INTEGER,
    display_order INTEGER,
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT timezone('UTC', now()),
    updated_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT timezone('UTC', now()),
    CONSTRAINT uq_gate_name UNIQUE (name)
);
GRANT SELECT, INSERT, UPDATE, DELETE ON asset.gate TO ${db_app_user};

CREATE TABLE asset.gate_captain (
    gate_captain_id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    captain_name VARCHAR(255) NOT NULL,
    gate_id UUID NOT NULL REFERENCES gate(gate_id),
    operation_id UUID NOT NULL REFERENCES operation(operation_id),
    shift_id UUID NOT NULL REFERENCES shift(shift_id),
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT timezone('UTC', now()),
    updated_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT timezone('UTC', now())
);
GRANT SELECT, INSERT, UPDATE, DELETE ON asset.gate_captain TO ${db_app_user};

CREATE TABLE asset.ticket_book (
    ticket_book_id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    scan_code VARCHAR(50) UNIQUE,
    start_num INT NOT NULL,
    end_num INT NOT NULL,
    next_ticket INT,
    is_reusable BOOLEAN DEFAULT TRUE,
    status VARCHAR(20),
    operation_id UUID REFERENCES operation(operation_id),
    shift_id UUID REFERENCES shift(shift_id),
    gate_id UUID REFERENCES gate(gate_id),
    device_id VARCHAR(50),
    parent_ticket_book_id UUID,
    process_id UUID,
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT timezone('UTC', now()),
    updated_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT timezone('UTC', now())
);
GRANT SELECT, INSERT, UPDATE, DELETE ON asset.ticket_book TO ${db_app_user};

CREATE TABLE asset.cash_bank (
    cash_bank_id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    bank_name VARCHAR(100),
    starting_amount DECIMAL(12, 2),
    official_count DECIMAL(12, 2),
    count_100s INT DEFAULT 0,
    count_50s INT DEFAULT 0,
    count_20s INT DEFAULT 0,
    count_10s INT DEFAULT 0,
    count_5s INT DEFAULT 0,
    count_1s INT DEFAULT 0,
    status VARCHAR(20),
    operation_id UUID REFERENCES operation(operation_id),
    shift_id UUID REFERENCES shift(shift_id),
    gate_id UUID REFERENCES gate(gate_id),
    device_id VARCHAR(50),
    parent_cash_bank_id UUID,
    process_id UUID,
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT timezone('UTC', now()),
    updated_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT timezone('UTC', now())
);
GRANT SELECT, INSERT, UPDATE, DELETE ON asset.cash_bank TO ${db_app_user};

CREATE TABLE asset.credit_report (
    credit_report_id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    total_amount DECIMAL(12, 2),
    operation_id UUID REFERENCES operation(operation_id),
    shift_id UUID REFERENCES shift(shift_id),
    gate_id UUID REFERENCES gate(gate_id),
    device_id VARCHAR(50),
    process_id UUID,
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT timezone('UTC', now()),
    updated_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT timezone('UTC', now())
);
GRANT SELECT, INSERT, UPDATE, DELETE ON asset.credit_report TO ${db_app_user};