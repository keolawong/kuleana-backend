ALTER TABLE asset.operation
ADD COLUMN ticket_price NUMERIC;

ALTER TABLE asset.operation
ADD COLUMN cc_fee NUMERIC;

ALTER TABLE asset.operation
ADD COLUMN cc_fee_type VARCHAR;