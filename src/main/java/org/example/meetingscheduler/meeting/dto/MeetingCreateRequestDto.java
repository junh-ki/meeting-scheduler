package org.example.meetingscheduler.meeting.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

public record MeetingCreateRequestDto(
    @NotBlank String title,
    String description,
    @NotNull LocalDateTime startTime,
    @NotNull LocalDateTime endTime,
    @NotEmpty List<@NotNull Long> participantUserIds
) {}
