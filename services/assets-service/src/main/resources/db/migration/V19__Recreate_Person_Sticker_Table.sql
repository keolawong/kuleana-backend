-- Recreate asset.person_sticker table required for seller stickers and QR codes
CREATE TABLE IF NOT EXISTS asset.person_sticker (
    person_sticker_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    person_id UUID REFERENCES asset.person(person_id) ON DELETE CASCADE,
    shift_id UUID REFERENCES asset.shift(shift_id) ON DELETE CASCADE,
    gate_id UUID REFERENCES asset.gate(gate_id) ON DELETE SET NULL,
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT timezone('UTC', now()),
    updated_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT timezone('UTC', now()),
    CONSTRAINT uq_person_sticker_person_shift UNIQUE (person_id, shift_id)
);

CREATE INDEX IF NOT EXISTS idx_person_sticker_shift ON asset.person_sticker(shift_id);
CREATE INDEX IF NOT EXISTS idx_person_sticker_person ON asset.person_sticker(person_id);

GRANT SELECT, INSERT, UPDATE, DELETE ON asset.person_sticker TO ${db_app_user};

-- Backfill seller stickers for active persons assigned to shifts
INSERT INTO asset.person_sticker (person_id, shift_id, gate_id, created_by)
SELECT person_id, shift_id, gate_id, 'Migration_V19'
FROM asset.person
WHERE shift_id IS NOT NULL
ON CONFLICT (person_id, shift_id) DO NOTHING;
