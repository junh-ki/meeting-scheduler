DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uq_user_email'
    ) THEN
        ALTER TABLE app_user
            ADD CONSTRAINT uq_user_email UNIQUE (email);
    END IF;
END $$;
