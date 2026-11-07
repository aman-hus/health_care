-- Doctors declare availability separately for each hospital and calendar date.
CREATE TABLE IF NOT EXISTS doctor_availability (
    id BIGSERIAL PRIMARY KEY,
    hospital_id BIGINT NOT NULL REFERENCES hospitals(id),
    doctor_id BIGINT NOT NULL REFERENCES users(id),
    availability_date DATE NOT NULL,
    available BOOLEAN NOT NULL,
    CONSTRAINT uk_doctor_availability_day UNIQUE (hospital_id, doctor_id, availability_date)
);

ALTER TABLE doctor_availability ADD COLUMN IF NOT EXISTS working_start_time TIME;
ALTER TABLE doctor_availability ADD COLUMN IF NOT EXISTS working_end_time TIME;
