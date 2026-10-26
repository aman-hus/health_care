-- Add globally unique MRNs. Existing users receive a number based on their
-- first hospital membership (lowest hospital ID); platform-only users remain null.
ALTER TABLE users ADD COLUMN IF NOT EXISTS mrn_number VARCHAR(100);

WITH first_membership AS (
    SELECT DISTINCT ON (hm.user_id)
           hm.user_id,
           h.name AS hospital_name
    FROM hospital_memberships hm
    JOIN hospitals h ON h.id = hm.hospital_id
    ORDER BY hm.user_id, hm.hospital_id
)
UPDATE users u
SET mrn_number = UPPER(SUBSTRING(REGEXP_REPLACE(fm.hospital_name, '[^A-Za-z]', '', 'g') FROM 1 FOR 3))
                 || '-' || u.id::text
FROM first_membership fm
WHERE fm.user_id = u.id
  AND u.mrn_number IS NULL;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_users_mrn_number') THEN
        ALTER TABLE users ADD CONSTRAINT uk_users_mrn_number UNIQUE (mrn_number);
    END IF;
END $$;
