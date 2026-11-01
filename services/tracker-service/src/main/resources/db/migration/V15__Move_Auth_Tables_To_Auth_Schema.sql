-- V15: Decouple Authentication & IAM from Tracker schema into dedicated Auth schema
-- This migration preserves all existing users, credentials, and roles with zero data loss.

-- 1. Create auth schema and grant access
CREATE SCHEMA IF NOT EXISTS auth;
GRANT USAGE ON SCHEMA auth TO ${db_app_user};

-- 2. Move user & IAM tables from tracker schema to auth schema
ALTER TABLE IF EXISTS tracker.users SET SCHEMA auth;
ALTER TABLE IF EXISTS tracker.roles SET SCHEMA auth;
ALTER TABLE IF EXISTS tracker.user_roles SET SCHEMA auth;
ALTER TABLE IF EXISTS tracker.pages SET SCHEMA auth;
ALTER TABLE IF EXISTS tracker.permissions SET SCHEMA auth;
ALTER TABLE IF EXISTS tracker.role_pages SET SCHEMA auth;
ALTER TABLE IF EXISTS tracker.role_permissions SET SCHEMA auth;
ALTER TABLE IF EXISTS tracker.user_security_questions SET SCHEMA auth;

-- 3. Grant full DML permissions on newly relocated auth tables to application user
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA auth TO ${db_app_user};
ALTER DEFAULT PRIVILEGES IN SCHEMA auth GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO ${db_app_user};

-- 4. Create transparent, auto-updatable views in tracker schema
-- Ensures 100% backwards compatibility for existing tracker services and queries
CREATE OR REPLACE VIEW tracker.users AS SELECT * FROM auth.users;
CREATE OR REPLACE VIEW tracker.roles AS SELECT * FROM auth.roles;
CREATE OR REPLACE VIEW tracker.user_roles AS SELECT * FROM auth.user_roles;
CREATE OR REPLACE VIEW tracker.pages AS SELECT * FROM auth.pages;
CREATE OR REPLACE VIEW tracker.permissions AS SELECT * FROM auth.permissions;
CREATE OR REPLACE VIEW tracker.role_pages AS SELECT * FROM auth.role_pages;
CREATE OR REPLACE VIEW tracker.role_permissions AS SELECT * FROM auth.role_permissions;
CREATE OR REPLACE VIEW tracker.user_security_questions AS SELECT * FROM auth.user_security_questions;

-- 5. Grant permissions on backwards-compatible views
GRANT SELECT, INSERT, UPDATE, DELETE ON tracker.users TO ${db_app_user};
GRANT SELECT, INSERT, UPDATE, DELETE ON tracker.roles TO ${db_app_user};
GRANT SELECT, INSERT, UPDATE, DELETE ON tracker.user_roles TO ${db_app_user};
GRANT SELECT, INSERT, UPDATE, DELETE ON tracker.pages TO ${db_app_user};
GRANT SELECT, INSERT, UPDATE, DELETE ON tracker.permissions TO ${db_app_user};
GRANT SELECT, INSERT, UPDATE, DELETE ON tracker.role_pages TO ${db_app_user};
GRANT SELECT, INSERT, UPDATE, DELETE ON tracker.role_permissions TO ${db_app_user};
GRANT SELECT, INSERT, UPDATE, DELETE ON tracker.user_security_questions TO ${db_app_user};
