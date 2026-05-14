package org.example.meetingscheduler.exception;

public class UnprocessableEntityException extends RuntimeException {

    public UnprocessableEntityException(final String message) {
        super(message);
    }
}
