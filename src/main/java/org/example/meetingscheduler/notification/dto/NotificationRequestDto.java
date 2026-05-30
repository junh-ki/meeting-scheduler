package org.example.meetingscheduler.notification.dto;

import java.time.LocalDateTime;

public record NotificationRequestDto(
    Long meetingId,
    String meetingTitle,
    LocalDateTime meetingStartTime,
    LocalDateTime meetingEndTime,
    NotificationEventType eventType,
    Long participantId,
    String participantName,
    String participantEmail
) {}
