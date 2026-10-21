-- PostgreSQL migration: run once before deploying the multi-hospital application.
-- Existing records are assigned to the initial hospital, preserving the current tenant.
BEGIN;

CREATE TABLE IF NOT EXISTS hospitals (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(160) NOT NULL,
    code VARCHAR(80) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_hospitals_code UNIQUE (code)
);

INSERT INTO hospitals (name, code, enabled)
VALUES ('Existing Hospital', 'DEFAULT', TRUE)
ON CONFLICT (code) DO NOTHING;

CREATE TABLE IF NOT EXISTS hospital_memberships (
    id BIGSERIAL PRIMARY KEY,
    hospital_id BIGINT NOT NULL REFERENCES hospitals(id),
    user_id BIGINT NOT NULL REFERENCES users(id),
    role VARCHAR(32) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_hospital_membership UNIQUE (hospital_id, user_id)
);

INSERT INTO hospital_memberships (hospital_id, user_id, role, enabled)
SELECT h.id, u.id,
       CASE WHEN u.role = 'ADMIN' AND u.id = (SELECT MIN(id) FROM users WHERE role = 'ADMIN')
            THEN 'PLATFORM_ADMIN' ELSE u.role END,
       u.enabled
FROM users u
CROSS JOIN hospitals h
WHERE h.code = 'DEFAULT'
ON CONFLICT (hospital_id, user_id) DO NOTHING;

ALTER TABLE appointments ADD COLUMN IF NOT EXISTS hospital_id BIGINT;
UPDATE appointments SET hospital_id = (SELECT id FROM hospitals WHERE code = 'DEFAULT')
WHERE hospital_id IS NULL;
ALTER TABLE appointments ALTER COLUMN hospital_id SET NOT NULL;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_appointments_hospital') THEN
        ALTER TABLE appointments ADD CONSTRAINT fk_appointments_hospital
            FOREIGN KEY (hospital_id) REFERENCES hospitals(id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_appointments_hospital ON appointments(hospital_id);
CREATE INDEX IF NOT EXISTS idx_hospital_membership_user ON hospital_memberships(user_id);

COMMIT;
