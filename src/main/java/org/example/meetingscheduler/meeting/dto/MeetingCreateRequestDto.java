package org.example.meetingscheduler.meeting.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

public record MeetingCreateRequestDto(
    @NotNull Long organizerId,
    @NotBlank String title,
    String description,
    @NotNull LocalDateTime startTime,
    @NotNull LocalDateTime endTime,
    @NotNull List<Long> participantUserIds
) {}
