ALTER TABLE guests DROP CONSTRAINT fk_guests_created_by;
ALTER TABLE guests ALTER COLUMN created_by DROP NOT NULL;
ALTER TABLE guests ADD CONSTRAINT fk_guests_created_by FOREIGN KEY (created_by) REFERENCES users (user_id) ON DELETE SET NULL;

ALTER TABLE events DROP CONSTRAINT fk_events_created_by;
ALTER TABLE events ALTER COLUMN created_by DROP NOT NULL;
ALTER TABLE events ADD CONSTRAINT fk_events_created_by FOREIGN KEY (created_by) REFERENCES users (user_id) ON DELETE SET NULL;

ALTER TABLE attendance DROP CONSTRAINT fk_attendance_marked_by;
ALTER TABLE attendance ALTER COLUMN marked_by DROP NOT NULL;
ALTER TABLE attendance ADD CONSTRAINT fk_attendance_marked_by FOREIGN KEY (marked_by) REFERENCES users (user_id) ON DELETE SET NULL;
