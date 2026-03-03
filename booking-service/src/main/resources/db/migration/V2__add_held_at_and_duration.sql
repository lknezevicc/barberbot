ALTER TABLE bookings ADD COLUMN held_at TIMESTAMPTZ;
ALTER TABLE bookings ADD COLUMN duration_minutes INT NOT NULL DEFAULT 30;

CREATE INDEX idx_bookings_barber_date_status ON bookings (barber, date, status);
