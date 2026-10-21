-- Run once when upgrading an existing database to appointment approval workflow.
ALTER TABLE appointments
    ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'PENDING';
