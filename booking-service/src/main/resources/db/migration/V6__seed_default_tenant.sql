-- Default demo tenant
INSERT INTO tenants (id, name, bot_token, bot_username, secret_token, contact_email, address, active, created_at, updated_at)
VALUES (
    'a0000000-0000-0000-0000-000000000001',
    'BarberBot Demo',
    'DEMO_BOT_TOKEN_CHANGE_ME',
    'BarberBotDemo',
    'demo-secret-token',
    'admin@barberbot.hr',
    'Ilica 1, 10000 Zagreb',
    true, now(), now()
) ON CONFLICT (name) DO NOTHING;

-- Default admin user (password: admin123)
-- BCrypt hash generated with cost factor 10
INSERT INTO app_users (id, tenant_id, telegram_id, chat_id, first_name, last_name, email, password_hash, role, created_at, updated_at)
VALUES (
    'b0000000-0000-0000-0000-000000000001',
    'a0000000-0000-0000-0000-000000000001',
    'admin',
    0,
    'Admin',
    'BarberBot',
    'admin@barberbot.hr',
    '$2a$10$dXJ3SW6G7P50lGmMQJrOuOZbPDCnCYgYHZ9FGOEHvJDq.qT3axemy',
    'ADMIN',
    now(), now()
) ON CONFLICT (email) DO NOTHING;
