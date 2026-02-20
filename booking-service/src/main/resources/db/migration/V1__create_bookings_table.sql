CREATE TABLE bookings (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     VARCHAR(64)  NOT NULL,
    chat_id     BIGINT       NOT NULL,
    service     VARCHAR(128) NOT NULL,
    date        DATE         NOT NULL,
    time        TIME         NOT NULL,
    barber      VARCHAR(128) NOT NULL,
    status      VARCHAR(32)  NOT NULL DEFAULT 'CONFIRMED',
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_bookings_user_id_status ON bookings (user_id, status);
CREATE INDEX idx_bookings_date ON bookings (date);
