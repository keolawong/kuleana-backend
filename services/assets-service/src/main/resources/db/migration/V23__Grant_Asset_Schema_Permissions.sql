-- V23: Ensure runtime application user has USAGE and table permissions on asset schema
GRANT USAGE ON SCHEMA asset TO ${db_app_user};

GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA asset TO ${db_app_user};
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA asset TO ${db_app_user};

ALTER DEFAULT PRIVILEGES IN SCHEMA asset GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO ${db_app_user};
