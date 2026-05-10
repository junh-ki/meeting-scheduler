package org.example.meetingscheduler.meeting;

import java.time.LocalDateTime;
import java.util.List;
import org.example.meetingscheduler.participant.ParticipantResponseDto;
import org.example.meetingscheduler.user.dto.UserResponseDto;

public record MeetingResponseDto(
    Long id,
    String title,
    String description,
    LocalDateTime startTime,
    LocalDateTime endTime,
    UserResponseDto organizer,
    List<ParticipantResponseDto> participants
) {}
