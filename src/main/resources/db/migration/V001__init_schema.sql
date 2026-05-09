CREATE TABLE app_user (
    id    BIGSERIAL PRIMARY KEY,
    name  VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL
);

CREATE TABLE meeting (
    id            BIGSERIAL PRIMARY KEY,
    title         VARCHAR(255) NOT NULL,
    description   TEXT,
    start_time    TIMESTAMP    NOT NULL,
    end_time      TIMESTAMP    NOT NULL,
    app_user_id   BIGINT REFERENCES app_user (id)
);

CREATE TABLE participant (
    id            BIGSERIAL PRIMARY KEY,
    meeting_id    BIGINT NOT NULL REFERENCES meeting (id),
    app_user_id   BIGINT NOT NULL REFERENCES app_user (id),
    UNIQUE (meeting_id, app_user_id)
);

CREATE TABLE timeslot (
    id            BIGSERIAL   PRIMARY KEY,
    app_user_id   BIGINT REFERENCES app_user (id),
    start_time    TIMESTAMP   NOT NULL,
    end_time      TIMESTAMP   NOT NULL,
    status        VARCHAR(10) NOT NULL
);
