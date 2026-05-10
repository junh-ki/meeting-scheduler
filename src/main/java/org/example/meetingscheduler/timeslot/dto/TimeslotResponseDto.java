package org.example.meetingscheduler.timeslot.dto;

import java.time.LocalDateTime;
import org.example.meetingscheduler.timeslot.SlotBookingStatus;
import org.example.meetingscheduler.user.dto.UserResponseDto;

public record TimeslotResponseDto(
    Long id,
    UserResponseDto organizer,
    LocalDateTime startTime,
    LocalDateTime endTime,
    SlotBookingStatus status
) {}
