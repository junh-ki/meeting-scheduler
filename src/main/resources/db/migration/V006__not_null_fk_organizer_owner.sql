DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'meeting' AND column_name = 'app_user_id' AND is_nullable = 'YES'
    ) THEN
        ALTER TABLE meeting ALTER COLUMN app_user_id SET NOT NULL;
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'timeslot' AND column_name = 'app_user_id' AND is_nullable = 'YES'
    ) THEN
        ALTER TABLE timeslot ALTER COLUMN app_user_id SET NOT NULL;
    END IF;
END $$;
