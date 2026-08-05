ALTER TABLE guests ADD COLUMN access_token VARCHAR(128);
ALTER TABLE guests ADD COLUMN expires_at TIMESTAMP;

CREATE UNIQUE INDEX uq_guests_access_token ON guests (access_token);
CREATE INDEX idx_guests_expires_at ON guests (expires_at);
