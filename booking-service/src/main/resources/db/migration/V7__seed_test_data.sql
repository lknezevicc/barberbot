-- Fix column type mismatch: Hibernate maps int -> INTEGER, DDL created SMALLINT
ALTER TABLE working_hours ALTER COLUMN day_of_week TYPE INTEGER;

-- Fix admin password hash for 'admin123'
UPDATE app_users SET password_hash = '$2b$10$A36cTN4k7.shx.FPQw/nbuhcTtDmH3eM6pLEsPHMqyzVAK/8uldFe'
WHERE email = 'admin@barberbot.hr';

-- Demo employees
INSERT INTO employees (id, tenant_id, name, specialization, contact, active, created_at)
VALUES
    ('e0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001', 'Marko Horvat', 'Musko sisanje, fade', '091-111-2222', true, now()),
    ('e0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000001', 'Ivan Kovacevic', 'Brijanje, brada', '091-333-4444', true, now()),
    ('e0000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000001', 'Luka Babic', 'Djecje sisanje, styling', '091-555-6666', true, now())
ON CONFLICT DO NOTHING;

-- Demo services
INSERT INTO services (id, tenant_id, name, duration_minutes, price, description, active, created_at)
VALUES
    ('50000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001', 'Sisanje', 30, 15.00, 'Klasicno musko sisanje', true, now()),
    ('50000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000001', 'Brijanje', 20, 10.00, 'Brijanje britve i oblikovanje brade', true, now()),
    ('50000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000001', 'Sisanje i brijanje', 45, 22.00, 'Kompletna usluga sisanja i brijanja', true, now()),
    ('50000000-0000-0000-0000-000000000004', 'a0000000-0000-0000-0000-000000000001', 'Djecje sisanje', 20, 10.00, 'Sisanje za djecu do 12 godina', true, now()),
    ('50000000-0000-0000-0000-000000000005', 'a0000000-0000-0000-0000-000000000001', 'Styling', 15, 8.00, 'Oblikovanje frizure', true, now())
ON CONFLICT DO NOTHING;

-- Working hours: Mon-Fri 09:00-17:00, Sat 09:00-13:00 for Marko
INSERT INTO working_hours (id, tenant_id, employee_id, day_of_week, start_time, end_time)
VALUES
    (gen_random_uuid(), 'a0000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000001', 1, '09:00', '17:00'),
    (gen_random_uuid(), 'a0000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000001', 2, '09:00', '17:00'),
    (gen_random_uuid(), 'a0000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000001', 3, '09:00', '17:00'),
    (gen_random_uuid(), 'a0000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000001', 4, '09:00', '17:00'),
    (gen_random_uuid(), 'a0000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000001', 5, '09:00', '17:00'),
    (gen_random_uuid(), 'a0000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000001', 6, '09:00', '13:00')
ON CONFLICT DO NOTHING;

-- Working hours: Mon-Fri 10:00-18:00 for Ivan
INSERT INTO working_hours (id, tenant_id, employee_id, day_of_week, start_time, end_time)
VALUES
    (gen_random_uuid(), 'a0000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000002', 1, '10:00', '18:00'),
    (gen_random_uuid(), 'a0000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000002', 2, '10:00', '18:00'),
    (gen_random_uuid(), 'a0000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000002', 3, '10:00', '18:00'),
    (gen_random_uuid(), 'a0000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000002', 4, '10:00', '18:00'),
    (gen_random_uuid(), 'a0000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000002', 5, '10:00', '18:00')
ON CONFLICT DO NOTHING;

-- Working hours: Mon-Fri 09:00-16:00, Sat 09:00-14:00 for Luka
INSERT INTO working_hours (id, tenant_id, employee_id, day_of_week, start_time, end_time)
VALUES
    (gen_random_uuid(), 'a0000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000003', 1, '09:00', '16:00'),
    (gen_random_uuid(), 'a0000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000003', 2, '09:00', '16:00'),
    (gen_random_uuid(), 'a0000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000003', 3, '09:00', '16:00'),
    (gen_random_uuid(), 'a0000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000003', 4, '09:00', '16:00'),
    (gen_random_uuid(), 'a0000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000003', 5, '09:00', '16:00'),
    (gen_random_uuid(), 'a0000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000003', 6, '09:00', '14:00')
ON CONFLICT DO NOTHING;

-- Demo bookings (next week, spread across employees)
INSERT INTO bookings (id, tenant_id, employee_id, service_id, user_id, chat_id, service, date, time, barber, status, duration_minutes, created_at, updated_at)
VALUES
    (gen_random_uuid(), 'a0000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000001', '50000000-0000-0000-0000-000000000001',
     'user_1001', 1001, 'Sisanje', CURRENT_DATE + INTERVAL '1 day', '09:00', 'Marko Horvat', 'CONFIRMED', 30, now(), now()),

    (gen_random_uuid(), 'a0000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000001', '50000000-0000-0000-0000-000000000003',
     'user_1002', 1002, 'Sisanje i brijanje', CURRENT_DATE + INTERVAL '1 day', '10:00', 'Marko Horvat', 'CONFIRMED', 45, now(), now()),

    (gen_random_uuid(), 'a0000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000001', '50000000-0000-0000-0000-000000000001',
     'user_1003', 1003, 'Sisanje', CURRENT_DATE + INTERVAL '1 day', '14:00', 'Marko Horvat', 'HELD', 30, now(), now()),

    (gen_random_uuid(), 'a0000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000002', '50000000-0000-0000-0000-000000000002',
     'user_1004', 1004, 'Brijanje', CURRENT_DATE + INTERVAL '1 day', '10:30', 'Ivan Kovacevic', 'CONFIRMED', 20, now(), now()),

    (gen_random_uuid(), 'a0000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000002', '50000000-0000-0000-0000-000000000003',
     'user_1005', 1005, 'Sisanje i brijanje', CURRENT_DATE + INTERVAL '2 days', '11:00', 'Ivan Kovacevic', 'CONFIRMED', 45, now(), now()),

    (gen_random_uuid(), 'a0000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000003', '50000000-0000-0000-0000-000000000004',
     'user_1006', 1006, 'Djecje sisanje', CURRENT_DATE + INTERVAL '2 days', '09:30', 'Luka Babic', 'CONFIRMED', 20, now(), now()),

    (gen_random_uuid(), 'a0000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000001', '50000000-0000-0000-0000-000000000001',
     'user_1007', 1007, 'Sisanje', CURRENT_DATE + INTERVAL '3 days', '09:00', 'Marko Horvat', 'CONFIRMED', 30, now(), now()),

    (gen_random_uuid(), 'a0000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000002', '50000000-0000-0000-0000-000000000002',
     'user_1001', 1001, 'Brijanje', CURRENT_DATE + INTERVAL '3 days', '14:00', 'Ivan Kovacevic', 'CANCELLED', 20, now(), now()),

    (gen_random_uuid(), 'a0000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000003', '50000000-0000-0000-0000-000000000005',
     'user_1008', 1008, 'Styling', CURRENT_DATE, '11:00', 'Luka Babic', 'CONFIRMED', 15, now(), now()),

    (gen_random_uuid(), 'a0000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000001', '50000000-0000-0000-0000-000000000003',
     'user_1009', 1009, 'Sisanje i brijanje', CURRENT_DATE, '15:00', 'Marko Horvat', 'CONFIRMED', 45, now(), now())
ON CONFLICT DO NOTHING;

-- Demo blocked slot (Marko is off this Saturday)
INSERT INTO blocked_slots (id, tenant_id, employee_id, date, all_day, reason)
VALUES
    (gen_random_uuid(), 'a0000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000001',
     CURRENT_DATE + INTERVAL '6 days', true, 'Slobodan dan')
ON CONFLICT DO NOTHING;

-- Demo client users
INSERT INTO app_users (id, tenant_id, telegram_id, chat_id, first_name, last_name, role, created_at, updated_at)
VALUES
    (gen_random_uuid(), 'a0000000-0000-0000-0000-000000000001', 'user_1001', 1001, 'Ante', 'Matic', 'CLIENT', now(), now()),
    (gen_random_uuid(), 'a0000000-0000-0000-0000-000000000001', 'user_1002', 1002, 'Josip', 'Novak', 'CLIENT', now(), now()),
    (gen_random_uuid(), 'a0000000-0000-0000-0000-000000000001', 'user_1003', 1003, 'Petra', 'Juric', 'CLIENT', now(), now()),
    (gen_random_uuid(), 'a0000000-0000-0000-0000-000000000001', 'user_1004', 1004, 'Ana', 'Simic', 'CLIENT', now(), now()),
    (gen_random_uuid(), 'a0000000-0000-0000-0000-000000000001', 'user_1005', 1005, 'Tomislav', 'Radic', 'CLIENT', now(), now())
ON CONFLICT DO NOTHING;
