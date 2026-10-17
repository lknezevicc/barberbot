ALTER TABLE bookings ADD COLUMN tenant_id UUID REFERENCES tenants(id);
ALTER TABLE bookings ADD COLUMN employee_id UUID REFERENCES employees(id);
ALTER TABLE bookings ADD COLUMN service_id UUID REFERENCES services(id);

CREATE INDEX idx_bookings_tenant ON bookings(tenant_id, date, status);
CREATE INDEX idx_bookings_employee ON bookings(employee_id, date, status);
