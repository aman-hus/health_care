-- Add demographic and contact fields collected by admin user registration.
ALTER TABLE users ADD COLUMN IF NOT EXISTS contact_number VARCHAR(30);
ALTER TABLE users ADD COLUMN IF NOT EXISTS gender VARCHAR(10);
ALTER TABLE users ADD COLUMN IF NOT EXISTS date_of_birth DATE;
