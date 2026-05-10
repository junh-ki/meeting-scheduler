package org.example.meetingscheduler.timeslot;

import java.time.LocalDateTime;
import org.example.meetingscheduler.user.UserResponseDto;

public record TimeslotResponseDto(
    Long id,
    UserResponseDto organizer,
    LocalDateTime startTime,
    LocalDateTime endTime,
    SlotBookingStatus status
) {}
