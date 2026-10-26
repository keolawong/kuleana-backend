-- V1__init_assets_schema.sql
-- Kuleana Systems Assets Service Schema Definition

CREATE SCHEMA IF NOT EXISTS asset;

CREATE TABLE IF NOT EXISTS asset.operation (
    operation_id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    start_date DATE,
    end_date DATE,
    location_name VARCHAR(255),
    is_active BOOLEAN DEFAULT TRUE,
    ticket_price DECIMAL(12, 2),
    cc_fee DECIMAL(12, 2),
    cc_fee_type VARCHAR(50),
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT timezone('UTC', now()),
    updated_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT timezone('UTC', now())
);

CREATE TABLE IF NOT EXISTS asset.gate (
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

CREATE TABLE IF NOT EXISTS asset.shift (
    shift_id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    operation_id UUID NOT NULL REFERENCES asset.operation(operation_id),
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

CREATE TABLE IF NOT EXISTS asset.person (
    person_id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    shift_id UUID REFERENCES asset.shift(shift_id),
    gate_id UUID REFERENCES asset.gate(gate_id),
    organization VARCHAR(150) DEFAULT 'Individual / None',
    job_role VARCHAR(50) DEFAULT 'Ticket Seller',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    notes VARCHAR(255),
    external_id VARCHAR(100),
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT timezone('UTC', now()),
    updated_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT timezone('UTC', now())
);

CREATE TABLE IF NOT EXISTS asset.device (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    device_id VARCHAR(100) NOT NULL,
    device_number VARCHAR(100) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT timezone('UTC', now()),
    updated_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT timezone('UTC', now()),
    CONSTRAINT uq_device_id UNIQUE (device_id),
    CONSTRAINT uq_device_number UNIQUE (device_number)
);

CREATE TABLE IF NOT EXISTS asset.cash_bank (
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
    count_1cs INT DEFAULT 0,
    status VARCHAR(20),
    operation_id UUID REFERENCES asset.operation(operation_id),
    shift_id UUID REFERENCES asset.shift(shift_id),
    gate_id UUID REFERENCES asset.gate(gate_id),
    device_id VARCHAR(50),
    parent_cash_bank_id UUID,
    process_id UUID,
    person_id UUID REFERENCES asset.person(person_id),
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT timezone('UTC', now()),
    updated_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT timezone('UTC', now())
);

CREATE TABLE IF NOT EXISTS asset.ticket_book (
    ticket_book_id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    scan_code VARCHAR(50) UNIQUE,
    start_num INT NOT NULL,
    end_num INT NOT NULL,
    next_ticket INT,
    is_reusable BOOLEAN DEFAULT TRUE,
    status VARCHAR(20),
    operation_id UUID REFERENCES asset.operation(operation_id),
    shift_id UUID REFERENCES asset.shift(shift_id),
    gate_id UUID REFERENCES asset.gate(gate_id),
    device_id VARCHAR(50),
    parent_ticket_book_id UUID,
    process_id UUID,
    person_id UUID REFERENCES asset.person(person_id),
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT timezone('UTC', now()),
    updated_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT timezone('UTC', now())
);

CREATE TABLE IF NOT EXISTS asset.credit_report (
    credit_report_id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    total_amount DECIMAL(12, 2),
    operation_id UUID REFERENCES asset.operation(operation_id),
    shift_id UUID REFERENCES asset.shift(shift_id),
    gate_id UUID REFERENCES asset.gate(gate_id),
    device_id VARCHAR(50),
    process_id UUID,
    person_id UUID REFERENCES asset.person(person_id),
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT timezone('UTC', now()),
    updated_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT timezone('UTC', now())
);
