package org.example.meetingscheduler.participant;

import org.example.meetingscheduler.user.UserResponseDto;

public record ParticipantResponseDto(
    Long id,
    UserResponseDto user
) {}
