-- V21__Grant_Sales_Tracker_Permissions.sql
-- Explicitly grant SELECT, INSERT, UPDATE, DELETE permissions on sales tracker tables to runtime database user

GRANT SELECT, INSERT, UPDATE, DELETE ON asset.shift_sales TO ${db_app_user};
GRANT SELECT, INSERT, UPDATE, DELETE ON asset.window_sales TO ${db_app_user};
GRANT SELECT, INSERT, UPDATE, DELETE ON asset.shift_worker TO ${db_app_user};
