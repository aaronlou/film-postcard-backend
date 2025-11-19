-- Add missing columns to photos table if they don't exist
-- This script is idempotent (safe to run multiple times)

DO $$ 
BEGIN
    -- Add title column if it doesn't exist
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name='photos' AND column_name='title') THEN
        ALTER TABLE photos ADD COLUMN title VARCHAR(255);
    END IF;

    -- Add description column if it doesn't exist
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name='photos' AND column_name='description') THEN
        ALTER TABLE photos ADD COLUMN description TEXT;
    END IF;

    -- Add location column if it doesn't exist
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name='photos' AND column_name='location') THEN
        ALTER TABLE photos ADD COLUMN location VARCHAR(255);
    END IF;

    -- Add camera column if it doesn't exist
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name='photos' AND column_name='camera') THEN
        ALTER TABLE photos ADD COLUMN camera VARCHAR(255);
    END IF;

    -- Add lens column if it doesn't exist
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name='photos' AND column_name='lens') THEN
        ALTER TABLE photos ADD COLUMN lens VARCHAR(255);
    END IF;

    -- Add settings column if it doesn't exist
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name='photos' AND column_name='settings') THEN
        ALTER TABLE photos ADD COLUMN settings VARCHAR(255);
    END IF;

    -- Add taken_at column if it doesn't exist
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name='photos' AND column_name='taken_at') THEN
        ALTER TABLE photos ADD COLUMN taken_at TIMESTAMP;
    END IF;
END $$;

-- Verify the columns were added
SELECT column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_name = 'photos'
ORDER BY ordinal_position;
