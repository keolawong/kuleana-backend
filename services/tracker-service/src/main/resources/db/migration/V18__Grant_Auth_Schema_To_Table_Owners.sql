-- V18: Grant USAGE on auth schema to table owners so foreign key triggers can execute
GRANT USAGE, CREATE ON SCHEMA auth TO kahua_migration_test;
GRANT USAGE ON SCHEMA auth TO kahua_tracker_rwt;
GRANT USAGE ON SCHEMA auth TO PUBLIC;

-- Ensure all current and future tables in auth schema are accessible to runtime and migration users
GRANT ALL ON ALL TABLES IN SCHEMA auth TO kahua_migration_test;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA auth TO ${db_app_user};
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA auth TO kahua_migration_test;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA auth TO ${db_app_user};
