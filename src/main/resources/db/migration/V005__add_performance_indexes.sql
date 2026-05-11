CREATE INDEX IF NOT EXISTS idx_timeslot_owner_status_time
    ON timeslot (app_user_id, status, start_time, end_time);

CREATE INDEX IF NOT EXISTS idx_meeting_start_end_time
    ON meeting (start_time, end_time);
