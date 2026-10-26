CREATE INDEX IF NOT EXISTS idx_ticket_book_history_shift_gate ON asset.ticket_book_history (shift_id, gate_id);
CREATE INDEX IF NOT EXISTS idx_cash_bank_history_shift_gate ON asset.cash_bank_history (shift_id, gate_id);
CREATE INDEX IF NOT EXISTS idx_credit_report_history_shift_gate ON asset.credit_report_history (shift_id, gate_id);
