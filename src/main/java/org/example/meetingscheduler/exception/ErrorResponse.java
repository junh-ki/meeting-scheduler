package org.example.meetingscheduler.exception;

public record ErrorResponse(int status,
                            String message) {}
