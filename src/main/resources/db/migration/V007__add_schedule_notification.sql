CREATE TABLE IF NOT EXISTS schedule_notification (
    id               BIGSERIAL    PRIMARY KEY,
    meeting_id       BIGINT       NOT NULL REFERENCES meeting (id),
    participant_id   BIGINT       NOT NULL REFERENCES app_user (id),
    status           VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    error_message    TEXT,
    last_attempt_at  TIMESTAMP WITH TIME ZONE,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_schedule_notification_pending
    ON schedule_notification (created_at)
    WHERE status = 'PENDING';
