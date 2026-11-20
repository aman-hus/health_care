-- Update an existing appointments status check to allow consultation completion.
-- Run once against the same PostgreSQL database used by the application.
BEGIN;

ALTER TABLE appointments
    DROP CONSTRAINT IF EXISTS appointments_status_check;

ALTER TABLE appointments
    ADD CONSTRAINT appointments_status_check
    CHECK (status IN ('PENDING', 'SCHEDULED', 'REJECTED', 'COMPLETED'));

COMMIT;
