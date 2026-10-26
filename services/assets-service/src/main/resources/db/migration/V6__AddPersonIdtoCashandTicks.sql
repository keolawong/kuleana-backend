ALTER TABLE asset.cash_bank 
ADD COLUMN person_sticker_id UUID;

ALTER TABLE asset.ticket_book 
ADD COLUMN person_sticker_id UUID;

ALTER TABLE asset.credit_report 
ADD COLUMN person_sticker_id UUID;