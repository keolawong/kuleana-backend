CREATE TABLE asset.device (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
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
GRANT SELECT, INSERT, UPDATE, DELETE ON asset.device TO ${db_app_user};

INSERT INTO asset.device (device_id, device_number, is_active, created_by)
SELECT DISTINCT
    TRIM(device_id) AS device_id,
    TRIM(device_id) AS device_number,
    TRUE,
    'Migration'
FROM (
    SELECT device_id FROM asset.cash_bank WHERE device_id IS NOT NULL AND TRIM(device_id) <> ''
    UNION
    SELECT device_id FROM asset.ticket_book WHERE device_id IS NOT NULL AND TRIM(device_id) <> ''
    UNION
    SELECT device_id FROM asset.credit_report WHERE device_id IS NOT NULL AND TRIM(device_id) <> ''
) existing_devices
ON CONFLICT DO NOTHING;
