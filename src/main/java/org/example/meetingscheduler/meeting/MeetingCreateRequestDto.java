package org.example.meetingscheduler.meeting;

import java.util.List;

public record MeetingCreateRequestDto(
    Long timeslotId,
    String title,
    String description,
    List<Long> participantUserIds
) {}
