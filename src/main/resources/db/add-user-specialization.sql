-- Store an optional specialization directly on each user; no lookup table is needed.
ALTER TABLE users ADD COLUMN IF NOT EXISTS specialization VARCHAR(100);
