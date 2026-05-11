ALTER TABLE app_user
    ADD CONSTRAINT uq_user_email UNIQUE (email);
