package org.example.meetingscheduler.timeslot;

import java.time.LocalDateTime;

public record TimeslotUpdateRequestDto(
    LocalDateTime startTime,
    LocalDateTime endTime,
    SlotBookingStatus status
) {}
