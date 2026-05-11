DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uq_meeting_organizer_time'
    ) THEN
        ALTER TABLE meeting
            ADD CONSTRAINT uq_meeting_organizer_time UNIQUE (app_user_id, start_time, end_time);
    END IF;
END $$;
