-- ============================================================
-- ImiTrade — add guest user support
-- ============================================================

-- Add guest flag and token to users
ALTER TABLE users ADD COLUMN is_guest BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE users ADD COLUMN guest_token UUID UNIQUE;

-- Make auth fields nullable so guests don't need fake emails / passwords
ALTER TABLE users ALTER COLUMN email DROP NOT NULL;
ALTER TABLE users ALTER COLUMN username DROP NOT NULL;
ALTER TABLE users ALTER COLUMN password_hash DROP NOT NULL;
