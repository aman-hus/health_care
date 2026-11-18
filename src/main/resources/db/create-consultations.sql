-- Consultation workflow foundation. Run once after the appointments, users,
-- and hospitals tables exist.
BEGIN;

CREATE TABLE IF NOT EXISTS consultations (
    id BIGSERIAL PRIMARY KEY,
    hospital_id BIGINT NOT NULL REFERENCES hospitals(id),
    appointment_id BIGINT NOT NULL REFERENCES appointments(id),
    patient_id BIGINT NOT NULL REFERENCES users(id),
    doctor_id BIGINT NOT NULL REFERENCES users(id),
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    next_section VARCHAR(20) NOT NULL DEFAULT 'DIAGNOSIS',
    diagnosis_data JSONB,
    evaluation_data JSONB,
    care_plan_data JSONB,
    fees_data JSONB,
    prescription_data JSONB,
    started_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_consultations_appointment UNIQUE (appointment_id),
    CONSTRAINT ck_consultations_status
        CHECK (status IN ('IN_PROGRESS', 'COMPLETED')),
    CONSTRAINT ck_consultations_next_section
        CHECK (next_section IN ('DIAGNOSIS', 'EVALUATION', 'CARE_PLAN', 'FEES', 'PRESCRIPTION')),
    CONSTRAINT ck_consultations_completion_timestamp
        CHECK ((status = 'COMPLETED' AND completed_at IS NOT NULL)
            OR (status = 'IN_PROGRESS' AND completed_at IS NULL))
);

CREATE INDEX IF NOT EXISTS idx_consultations_hospital_doctor_started
    ON consultations (hospital_id, doctor_id, started_at DESC);

CREATE INDEX IF NOT EXISTS idx_consultations_hospital_patient_started
    ON consultations (hospital_id, patient_id, started_at DESC);

COMMIT;
