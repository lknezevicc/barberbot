ALTER TABLE app_users ADD COLUMN email VARCHAR(255) UNIQUE;
CREATE INDEX idx_app_users_email ON app_users(email);
