-- V1: Create registration schema and core tables
CREATE SCHEMA IF NOT EXISTS registration;

CREATE TABLE IF NOT EXISTS registration.registration_forms (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id UUID NOT NULL,
    slug VARCHAR(150) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    form_mode VARCHAR(40) NOT NULL CHECK (form_mode IN ('GENERAL_EVENT', 'SHIFTS_AND_ROLES', 'COMPETITION_CATEGORIES', 'HYBRID')),
    external_event_id VARCHAR(255),
    event_timezone VARCHAR(100) NOT NULL DEFAULT 'America/Denver',
    go_live_at_utc TIMESTAMPTZ,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at_utc TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at_utc TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_registration_forms_org_slug UNIQUE (org_id, slug)
);

CREATE INDEX IF NOT EXISTS idx_registration_forms_org_active
    ON registration.registration_forms (org_id, is_active);

CREATE TABLE IF NOT EXISTS registration.registration_form_versions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    form_id UUID NOT NULL REFERENCES registration.registration_forms(id) ON DELETE CASCADE,
    version INTEGER NOT NULL,
    schema_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    logic_rules_json JSONB NOT NULL DEFAULT '[]'::jsonb,
    config_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    cutoff_windows_json JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at_utc TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_registration_form_versions_form_version UNIQUE (form_id, version)
);

CREATE INDEX IF NOT EXISTS idx_registration_form_versions_form_id
    ON registration.registration_form_versions (form_id);

CREATE TABLE IF NOT EXISTS registration.registration_options (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    form_version_id UUID NOT NULL REFERENCES registration.registration_form_versions(id) ON DELETE CASCADE,
    external_ref_id VARCHAR(255),
    option_type VARCHAR(64) NOT NULL,
    label VARCHAR(255) NOT NULL,
    metadata_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    max_capacity INTEGER,
    reserved_count INTEGER NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_registration_options_reserved_nonneg CHECK (reserved_count >= 0),
    CONSTRAINT chk_registration_options_capacity CHECK (max_capacity IS NULL OR max_capacity >= 0)
);

CREATE INDEX IF NOT EXISTS idx_registration_options_form_version
    ON registration.registration_options (form_version_id);

CREATE TABLE IF NOT EXISTS registration.form_account_quotas (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    form_id UUID NOT NULL REFERENCES registration.registration_forms(id) ON DELETE CASCADE,
    account_user_id VARCHAR(255) NOT NULL,
    override_max_submissions INTEGER,
    CONSTRAINT uq_form_account_quotas_form_user UNIQUE (form_id, account_user_id)
);

CREATE TABLE IF NOT EXISTS registration.registration_submissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id UUID NOT NULL,
    form_id UUID NOT NULL REFERENCES registration.registration_forms(id),
    form_version_id UUID NOT NULL REFERENCES registration.registration_form_versions(id),
    account_user_id VARCHAR(255) NOT NULL,
    participant_key VARCHAR(255) NOT NULL,
    participant_profile_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    answers_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    admin_fields_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    applied_window_id VARCHAR(128),
    fee_snapshot_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    payment_status VARCHAR(32) NOT NULL DEFAULT 'NOT_REQUIRED' CHECK (payment_status IN ('NOT_REQUIRED', 'PENDING', 'PAID', 'WAIVED')),
    sync_status VARCHAR(32) NOT NULL DEFAULT 'PENDING' CHECK (sync_status IN ('PENDING', 'SYNCED', 'FAILED')),
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'SUBMITTED', 'WAITLISTED', 'CANCELLED')),
    confirmation_code VARCHAR(32),
    created_at_utc TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at_utc TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    submitted_at_utc TIMESTAMPTZ,
    cancelled_at_utc TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_registration_submissions_org_form
    ON registration.registration_submissions (org_id, form_id, status);
CREATE INDEX IF NOT EXISTS idx_registration_submissions_account
    ON registration.registration_submissions (account_user_id, form_id, status);
CREATE INDEX IF NOT EXISTS idx_registration_submissions_participant
    ON registration.registration_submissions (form_id, participant_key, status);

CREATE TABLE IF NOT EXISTS registration.registration_submission_selections (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    submission_id UUID NOT NULL REFERENCES registration.registration_submissions(id) ON DELETE CASCADE,
    option_id UUID NOT NULL REFERENCES registration.registration_options(id),
    status VARCHAR(32) NOT NULL CHECK (status IN ('ACTIVE', 'WAITLISTED', 'CANCELLED'))
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_submission_option_active
    ON registration.registration_submission_selections (submission_id, option_id)
    WHERE status = 'ACTIVE';

CREATE INDEX IF NOT EXISTS idx_submission_selections_option_status
    ON registration.registration_submission_selections (option_id, status);

CREATE TABLE IF NOT EXISTS registration.agreement_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id UUID NOT NULL,
    template_key VARCHAR(128) NOT NULL,
    version INTEGER NOT NULL,
    type VARCHAR(64) NOT NULL,
    age_rule VARCHAR(32) NOT NULL DEFAULT 'ALL_AGES',
    requirement_mode VARCHAR(32) NOT NULL DEFAULT 'REQUIRED',
    title VARCHAR(255) NOT NULL,
    summary_bullets_json JSONB NOT NULL DEFAULT '[]'::jsonb,
    body_html TEXT NOT NULL,
    content_hash VARCHAR(64) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_agreement_templates_org_key_ver UNIQUE (org_id, template_key, version)
);

CREATE INDEX IF NOT EXISTS idx_agreement_templates_org_active
    ON registration.agreement_templates (org_id, is_active);

CREATE TABLE IF NOT EXISTS registration.signed_agreements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    submission_id UUID NOT NULL REFERENCES registration.registration_submissions(id) ON DELETE RESTRICT,
    participant_id VARCHAR(255) NOT NULL,
    participant_name VARCHAR(255) NOT NULL,
    is_minor BOOLEAN NOT NULL DEFAULT FALSE,
    esign_consent_accepted BOOLEAN NOT NULL,
    minor_assent_name VARCHAR(255),
    minor_assent_at_utc TIMESTAMPTZ,
    signer_name VARCHAR(255) NOT NULL,
    signer_relationship VARCHAR(100),
    signature_data TEXT NOT NULL,
    template_version_id UUID NOT NULL REFERENCES registration.agreement_templates(id),
    snapshot_hash VARCHAR(64) NOT NULL,
    ip_address VARCHAR(64),
    user_agent TEXT,
    signed_at_utc TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_signed_agreements_submission
    ON registration.signed_agreements (submission_id);

CREATE TABLE IF NOT EXISTS registration.registration_audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id UUID NOT NULL,
    submission_id UUID REFERENCES registration.registration_submissions(id) ON DELETE SET NULL,
    actor_user_id VARCHAR(255) NOT NULL,
    action VARCHAR(100) NOT NULL,
    before_json JSONB,
    after_json JSONB,
    created_at_utc TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_registration_audit_log_submission
    ON registration.registration_audit_log (submission_id, created_at_utc DESC);

CREATE TABLE IF NOT EXISTS registration.registration_outbox (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id UUID NOT NULL,
    submission_id UUID NOT NULL REFERENCES registration.registration_submissions(id) ON DELETE RESTRICT,
    event_type VARCHAR(100) NOT NULL,
    payload_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'PROCESSING', 'SYNCED', 'FAILED')),
    retry_count INTEGER NOT NULL DEFAULT 0,
    next_attempt_at_utc TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at_utc TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    processed_at_utc TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_registration_outbox_status_next
    ON registration.registration_outbox (status, next_attempt_at_utc);

-- Explicit runtime role grants
GRANT USAGE ON SCHEMA registration TO ${db_app_user};
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA registration TO ${db_app_user};
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA registration TO ${db_app_user};
