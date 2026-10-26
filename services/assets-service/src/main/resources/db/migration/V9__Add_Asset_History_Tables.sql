ALTER TABLE asset.ticket_book DROP CONSTRAINT IF EXISTS ticket_book_scan_code_key;

CREATE TABLE asset.ticket_book_history (
    ticket_book_history_id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    ticket_book_id UUID,
    scan_code VARCHAR(50),
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
    created_at TIMESTAMP DEFAULT timezone('UTC', now())
);
GRANT SELECT, INSERT, UPDATE, DELETE ON asset.ticket_book_history TO ${db_app_user};

CREATE INDEX idx_ticket_book_history_ticket_book_id ON asset.ticket_book_history (ticket_book_id);

CREATE TABLE asset.cash_bank_history (
    cash_bank_history_id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    cash_bank_id UUID,
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
    created_at TIMESTAMP DEFAULT timezone('UTC', now())
);
GRANT SELECT, INSERT, UPDATE, DELETE ON asset.cash_bank_history TO ${db_app_user};

CREATE INDEX idx_cash_bank_history_cash_bank_id ON asset.cash_bank_history (cash_bank_id);

CREATE TABLE asset.credit_report_history (
    credit_report_history_id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    credit_report_id UUID,
    total_amount DECIMAL(12, 2),
    operation_id UUID REFERENCES operation(operation_id),
    shift_id UUID REFERENCES shift(shift_id),
    gate_id UUID REFERENCES gate(gate_id),
    device_id VARCHAR(50),
    process_id UUID,
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT timezone('UTC', now())
);
GRANT SELECT, INSERT, UPDATE, DELETE ON asset.credit_report_history TO ${db_app_user};

CREATE INDEX idx_credit_report_history_credit_report_id ON asset.credit_report_history (credit_report_id);