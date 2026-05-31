CREATE TABLE IF NOT EXISTS delete_notification (
    id                 BIGSERIAL    PRIMARY KEY,
    meeting_id         BIGINT       NOT NULL,
    meeting_title      VARCHAR(255) NOT NULL,
    meeting_start_time TIMESTAMP    NOT NULL,
    meeting_end_time   TIMESTAMP    NOT NULL,
    participant_id     BIGINT       NOT NULL REFERENCES app_user (id),
    status             VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    error_message      TEXT,
    last_attempt_at    TIMESTAMP WITH TIME ZONE,
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_delete_notification_pending
    ON delete_notification (created_at)
    WHERE status = 'PENDING';
