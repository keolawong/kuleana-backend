ALTER TABLE tracker.users 
ADD COLUMN first_name VARCHAR(100) NULL,
ADD COLUMN last_name VARCHAR(100) NULL,
ADD COLUMN force_password_change BOOLEAN NOT NULL DEFAULT false,
ADD COLUMN password_reset_expires_at TIMESTAMP WITH TIME ZONE NULL,
ADD COLUMN failed_login_attempts INT NOT NULL DEFAULT 0,
ADD COLUMN lockout_round INT NOT NULL DEFAULT 0,
ADD COLUMN account_locked_until TIMESTAMP WITH TIME ZONE NULL;

CREATE TABLE tracker.user_security_questions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES tracker.users(id) ON DELETE CASCADE,
    question VARCHAR(255) NOT NULL,
    answer_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('UTC', now())
);

CREATE INDEX idx_user_sec_questions_user_id ON tracker.user_security_questions(user_id);

GRANT SELECT, INSERT, UPDATE, DELETE ON tracker.user_security_questions TO ${db_app_user};
