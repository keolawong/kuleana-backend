-- 1. Create Staffing Requirements Table
CREATE TABLE asset.shift_gate_requirement (
    requirement_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    shift_id UUID NOT NULL REFERENCES asset.shift(shift_id) ON DELETE CASCADE,
    gate_id UUID NOT NULL REFERENCES asset.gate(gate_id) ON DELETE CASCADE,
    job_role VARCHAR(50) NOT NULL,
    workers_needed INT NOT NULL DEFAULT 0,
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT timezone('UTC', now()),
    updated_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT timezone('UTC', now()),
    CONSTRAINT uq_shift_gate_role UNIQUE (shift_id, gate_id, job_role)
);
CREATE INDEX idx_req_shift_gate ON asset.shift_gate_requirement(shift_id, gate_id);
GRANT SELECT, INSERT, UPDATE, DELETE ON asset.shift_gate_requirement TO ${db_app_user};

-- 2. Consolidate asset.person Table (Merge person_sticker into person)
ALTER TABLE asset.person
    ADD COLUMN IF NOT EXISTS shift_id UUID REFERENCES asset.shift(shift_id) ON DELETE CASCADE,
    ADD COLUMN IF NOT EXISTS gate_id UUID REFERENCES asset.gate(gate_id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS organization VARCHAR(150) NOT NULL DEFAULT 'Individual / None',
    ADD COLUMN IF NOT EXISTS job_role VARCHAR(50) NOT NULL DEFAULT 'Ticket Seller',
    ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT true;

-- Backfill shift_id and gate_id from existing person_sticker records if person_sticker table exists
DO $$
BEGIN
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_schema = 'asset' AND table_name = 'person_sticker') THEN
        UPDATE asset.person p
        SET shift_id = ps.shift_id,
            gate_id = ps.gate_id
        FROM asset.person_sticker ps
        WHERE p.person_id = ps.person_id;

        -- Re-point foreign keys in cash_bank, ticket_book, credit_report to person.person_id
        UPDATE asset.cash_bank cb
        SET person_sticker_id = ps.person_id
        FROM asset.person_sticker ps
        WHERE cb.person_sticker_id = ps.person_sticker_id;

        UPDATE asset.ticket_book tb
        SET person_sticker_id = ps.person_id
        FROM asset.person_sticker ps
        WHERE tb.person_sticker_id = ps.person_sticker_id;

        UPDATE asset.credit_report cr
        SET person_sticker_id = ps.person_id
        FROM asset.person_sticker ps
        WHERE cr.person_sticker_id = ps.person_sticker_id;

        -- Safely drop legacy person_sticker table
        DROP TABLE IF EXISTS asset.person_sticker CASCADE;
    END IF;
END $$;

-- Rename foreign key columns from person_sticker_id to person_id in asset tables if needed
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_schema = 'asset' AND table_name = 'cash_bank' AND column_name = 'person_sticker_id'
    ) THEN
        ALTER TABLE asset.cash_bank RENAME COLUMN person_sticker_id TO person_id;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_schema = 'asset' AND table_name = 'ticket_book' AND column_name = 'person_sticker_id'
    ) THEN
        ALTER TABLE asset.ticket_book RENAME COLUMN person_sticker_id TO person_id;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_schema = 'asset' AND table_name = 'credit_report' AND column_name = 'person_sticker_id'
    ) THEN
        ALTER TABLE asset.credit_report RENAME COLUMN person_sticker_id TO person_id;
    END IF;
END $$;

-- Add constraints on cash_bank, ticket_book, credit_report referencing asset.person
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_cash_bank_person') THEN
        ALTER TABLE asset.cash_bank
            ADD CONSTRAINT fk_cash_bank_person FOREIGN KEY (person_id) REFERENCES asset.person(person_id) ON DELETE SET NULL;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_ticket_book_person') THEN
        ALTER TABLE asset.ticket_book
            ADD CONSTRAINT fk_ticket_book_person FOREIGN KEY (person_id) REFERENCES asset.person(person_id) ON DELETE SET NULL;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_credit_report_person') THEN
        ALTER TABLE asset.credit_report
            ADD CONSTRAINT fk_credit_report_person FOREIGN KEY (person_id) REFERENCES asset.person(person_id) ON DELETE SET NULL;
    END IF;
END $$;

-- Performance Indexes on consolidated person table
CREATE INDEX IF NOT EXISTS idx_person_shift_gate ON asset.person(shift_id, gate_id);
CREATE INDEX IF NOT EXISTS idx_person_shift_role ON asset.person(shift_id, job_role);
CREATE INDEX IF NOT EXISTS idx_person_shift_org ON asset.person(shift_id, organization);
CREATE INDEX IF NOT EXISTS idx_person_org_name ON asset.person(organization, last_name);
GRANT SELECT, INSERT, UPDATE, DELETE ON asset.person TO ${db_app_user};

-- 3. Create Shift-Scoped Organization Gate Preferences Table
CREATE TABLE asset.org_gate_preference (
    preference_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    shift_id UUID NOT NULL REFERENCES asset.shift(shift_id) ON DELETE CASCADE,
    gate_id UUID NOT NULL REFERENCES asset.gate(gate_id) ON DELETE CASCADE,
    organization VARCHAR(150) NOT NULL,
    priority INT NOT NULL DEFAULT 1, -- 1 = High, 2 = Medium, 3 = Low
    notes VARCHAR(255),
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT timezone('UTC', now()),
    updated_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT timezone('UTC', now()),
    CONSTRAINT uq_shift_org_gate_pref UNIQUE (shift_id, organization, gate_id)
);
CREATE INDEX idx_org_pref_shift_org ON asset.org_gate_preference(shift_id, organization);
CREATE INDEX idx_org_pref_shift_gate ON asset.org_gate_preference(shift_id, gate_id);
GRANT SELECT, INSERT, UPDATE, DELETE ON asset.org_gate_preference TO ${db_app_user};
