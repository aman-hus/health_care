-- Store Cloudinary references on each user; image bytes remain in Cloudinary.
ALTER TABLE users ADD COLUMN IF NOT EXISTS profile_image_url VARCHAR(1000);
ALTER TABLE users ADD COLUMN IF NOT EXISTS profile_image_public_id VARCHAR(255);
