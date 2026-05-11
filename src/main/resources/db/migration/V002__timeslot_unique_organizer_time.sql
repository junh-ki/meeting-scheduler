DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uq_timeslot_organizer_time'
    ) THEN
        ALTER TABLE timeslot
            ADD CONSTRAINT uq_timeslot_organizer_time UNIQUE (app_user_id, start_time, end_time);
    END IF;
END $$;
