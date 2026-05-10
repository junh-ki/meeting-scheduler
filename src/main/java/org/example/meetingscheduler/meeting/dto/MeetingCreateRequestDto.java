package org.example.meetingscheduler.meeting.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record MeetingCreateRequestDto(
    @NotNull Long timeslotId,
    @NotBlank String title,
    String description,
    @NotNull List<Long> participantUserIds
) {}
