package org.example.meetingscheduler.timeslot.dto;

import java.time.LocalDateTime;
import org.example.meetingscheduler.timeslot.SlotBookingStatus;

public record TimeslotUpdateRequestDto(
    LocalDateTime startTime,
    LocalDateTime endTime,
    SlotBookingStatus status
) {}
