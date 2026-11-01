CREATE TABLE tracker.shedlock (
    name VARCHAR(64) NOT NULL,
    lock_until TIMESTAMP NOT NULL,
    locked_at TIMESTAMP NOT NULL,
    locked_by VARCHAR(255) NOT NULL,
    PRIMARY KEY (name)
);

GRANT SELECT, INSERT, UPDATE, DELETE ON tracker.shedlock TO ${db_app_user};
