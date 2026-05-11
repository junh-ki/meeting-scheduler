ALTER TABLE timeslot
    ADD CONSTRAINT uq_timeslot_organizer_time UNIQUE (app_user_id, start_time, end_time);