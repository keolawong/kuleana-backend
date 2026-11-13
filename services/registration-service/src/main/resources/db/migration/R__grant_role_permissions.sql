-- Repeatable migration to ensure runtime application user has schema and table permissions
GRANT USAGE ON SCHEMA registration TO ${db_app_user};

GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA registration TO ${db_app_user};
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA registration TO ${db_app_user};

ALTER DEFAULT PRIVILEGES IN SCHEMA registration GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO ${db_app_user};
ALTER DEFAULT PRIVILEGES IN SCHEMA registration GRANT USAGE, SELECT ON SEQUENCES TO ${db_app_user};
