package org.example.meetingscheduler.user;

public record UserResponseDto(
    Long id,
    String name,
    String email
) {}
