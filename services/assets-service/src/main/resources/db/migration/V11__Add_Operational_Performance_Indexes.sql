-- Operational performance indexes for assets tables
CREATE INDEX IF NOT EXISTS idx_cash_bank_op_shift_gate_status ON asset.cash_bank (operation_id, shift_id, gate_id, status);
CREATE INDEX IF NOT EXISTS idx_cash_bank_parent_id ON asset.cash_bank (parent_cash_bank_id);
CREATE INDEX IF NOT EXISTS idx_ticket_book_op_shift_gate_status ON asset.ticket_book (operation_id, shift_id, gate_id, status);
CREATE INDEX IF NOT EXISTS idx_ticket_book_parent_id ON asset.ticket_book (parent_ticket_book_id);
CREATE INDEX IF NOT EXISTS idx_credit_report_shift_gate ON asset.credit_report (shift_id, gate_id);
CREATE INDEX IF NOT EXISTS idx_person_sticker_shift_gate ON asset.person_sticker (shift_id, gate_id);
